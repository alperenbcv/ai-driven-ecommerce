package com.ecommerce.user.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.user.config.EmailBloomFilter;
import com.ecommerce.user.config.UserRabbitMQConfig;
import com.ecommerce.user.dto.request.LoginRequest;
import com.ecommerce.user.dto.request.RegisterRequest;
import com.ecommerce.user.dto.response.AuthResponse;
import com.ecommerce.user.entity.EmailVerificationToken;
import com.ecommerce.user.entity.PasswordResetToken;
import com.ecommerce.user.entity.RoleName;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.EmailVerificationTokenRepository;
import com.ecommerce.user.repository.PasswordResetTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final EmailBloomFilter emailBloomFilter;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RabbitTemplate userRabbitTemplate;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Bloom Filter: "kesinlikle yok" diyorsa DB'ye gitme
        // "muhtemelen var" diyorsa VEYA filter henüz ısınmamışsa DB'ye sor
        boolean bloomSaysExists = emailBloomFilter.mightExist(request.getEmail());
        if (bloomSaysExists && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Bu e-posta adresi zaten kayıtlı", "EMAIL_ALREADY_EXISTS");
        }
        // Bloom filter false diyorsa da DB unique constraint bunu yakalar çift güvenlik

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleName.USER)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        emailBloomFilter.add(savedUser.getEmail());
        log.info("Yeni kullanıcı kaydedildi: {}", savedUser.getEmail());

        // E-posta doğrulama tokeni oluştur ve rabbitmq'ye event gönder
        String verificationToken = generateAndSaveVerificationToken(savedUser);
        publishVerificationEmail(savedUser, verificationToken);

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .user(userMapper.toUserResponse(savedUser))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException("E-posta veya şifre hatalı", "INVALID_CREDENTIALS");
        }

        User user = userRepository.findByEmailAndActiveTrue(request.getEmail())
                .orElseThrow(() -> new BusinessException("Hesap bulunamadı veya pasif", "USER_NOT_FOUND"));

        if (!user.isEmailVerified()) {
            throw new BusinessException(
                "E-posta adresiniz doğrulanmamış. Lütfen gelen kutunuzu kontrol edin.",
                "EMAIL_NOT_VERIFIED"
            );
        }

        log.info("Kullanıcı giriş yaptı: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı", "USER_NOT_FOUND"));

        if (user.isEmailVerified()) {
            throw new BusinessException("Bu hesap zaten doğrulanmış", "ALREADY_VERIFIED");
        }

        String token = generateAndSaveVerificationToken(user);
        publishVerificationEmail(user, token);
        log.info("Doğrulama maili yeniden gönderildi: {}", email);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Geçersiz doğrulama linki", "INVALID_TOKEN"));

        User user = verToken.getUser();

        if (verToken.isUsed()) {
            if (user.isEmailVerified()) {
                log.info("E-posta zaten doğrulanmış: {}", user.getEmail());
                return;
            }
            throw new BusinessException("Bu doğrulama linki daha önce kullanılmış", "TOKEN_ALREADY_USED");
        }
        if (verToken.isExpired()) {
            if (user.isEmailVerified()) {
                log.info("E-posta zaten doğrulanmış, süresi dolmuş token yoksayıldı: {}", user.getEmail());
                return;
            }
            throw new BusinessException("Doğrulama linkinin süresi dolmuş", "TOKEN_EXPIRED");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        verToken.setUsed(true);
        verificationTokenRepository.save(verToken);

        log.info("E-posta doğrulandı: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailAndActiveTrue(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());

            String resetToken = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(resetToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(token);

            publishPasswordResetEmail(user, resetToken);
            log.info("Şifre sıfırlama maili gönderildi: {}", email);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Geçersiz şifre sıfırlama linki", "INVALID_TOKEN"));

        if (resetToken.isUsed()) {
            throw new BusinessException("Bu link daha önce kullanılmış", "TOKEN_ALREADY_USED");
        }
        if (resetToken.isExpired()) {
            throw new BusinessException("Şifre sıfırlama linkinin süresi dolmuş (1 saat)", "TOKEN_EXPIRED");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Şifre sıfırlandı: {}", user.getEmail());
    }

    @Override
    public boolean checkEmailExists(String email) {
        // UI doğruluk için her zaman DB
        // Bloom filter register metodunda DB git gelini azaltmak için kullanılıyor
        return userRepository.existsByEmail(email);
    }


    private String generateAndSaveVerificationToken(User user) {
        verificationTokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        EmailVerificationToken verToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        verificationTokenRepository.save(verToken);
        return token;
    }

    private void publishVerificationEmail(User user, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", user.getEmail());
        payload.put("firstName", user.getFirstName());
        payload.put("verificationToken", token);
        payload.put("verificationLink", frontendUrl + "/verify-email?token=" + token);

        userRabbitTemplate.convertAndSend(
            UserRabbitMQConfig.USER_EXCHANGE,
            UserRabbitMQConfig.USER_REGISTERED,
            payload
        );
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenStr) {
        if (!jwtService.isTokenValid(refreshTokenStr) || !jwtService.isRefreshToken(refreshTokenStr)) {
            throw new BusinessException("Geçersiz veya süresi dolmuş refresh token", "INVALID_REFRESH_TOKEN");
        }

        Long userId = jwtService.extractUserId(refreshTokenStr);
        User user = userRepository.findById(userId)
                .filter(u -> u.isActive() && u.isEmailVerified())
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı", "USER_NOT_FOUND"));

        log.info("Access token yenilendi: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .user(userMapper.toUserResponse(user))
                .build();
    }

    private void publishPasswordResetEmail(User user, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", user.getEmail());
        payload.put("firstName", user.getFirstName());
        payload.put("resetToken", token);
        payload.put("resetLink", frontendUrl + "/reset-password?token=" + token);

        userRabbitTemplate.convertAndSend(
            UserRabbitMQConfig.USER_EXCHANGE,
            UserRabbitMQConfig.USER_PASSWORD_RESET,
            payload
        );
    }
}
