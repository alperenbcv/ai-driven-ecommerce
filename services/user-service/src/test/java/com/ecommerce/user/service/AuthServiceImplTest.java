package com.ecommerce.user.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.user.config.EmailBloomFilter;
import com.ecommerce.user.dto.request.LoginRequest;
import com.ecommerce.user.dto.request.RegisterRequest;
import com.ecommerce.user.dto.response.AuthResponse;
import com.ecommerce.user.dto.response.UserResponse;
import com.ecommerce.user.entity.RoleName;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.repository.EmailVerificationTokenRepository;
import com.ecommerce.user.repository.PasswordResetTokenRepository;
import com.ecommerce.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Testleri")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;
    @Mock private EmailBloomFilter emailBloomFilter;
    @Mock private EmailVerificationTokenRepository verificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RabbitTemplate userRabbitTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Ali");
        registerRequest.setLastName("Yılmaz");
        registerRequest.setEmail("ali@test.com");
        registerRequest.setPassword("Sifre123!");
        registerRequest.setConfirmPassword("Sifre123!");

        savedUser = User.builder()
                .firstName("Ali")
                .lastName("Yılmaz")
                .email("ali@test.com")
                .password("$2a$hashed_password")
                .role(RoleName.USER)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Başarılı kayıt → doğrulanana kadar token dönmez; kullanıcı bilgisi döner")
        void register_success() {
            given(emailBloomFilter.mightExist("ali@test.com")).willReturn(false);
            given(passwordEncoder.encode("Sifre123!")).willReturn("$2a$hashed_password");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toUserResponse(savedUser)).willReturn(UserResponse.builder()
                    .email("ali@test.com").firstName("Ali").lastName("Yılmaz").build());

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNull();
            assertThat(response.getRefreshToken()).isNull();
            assertThat(response.getUser().getEmail()).isEqualTo("ali@test.com");

            then(passwordEncoder).should().encode("Sifre123!");
            then(emailBloomFilter).should().add("ali@test.com");
            then(userRepository).should().save(any(User.class));
            then(jwtService).should(never()).generateAccessToken(any());
            then(jwtService).should(never()).generateRefreshToken(any());
        }

        @Test
        @DisplayName("Email zaten kayıtlı → Bloom Filter + DB doğrulama → BusinessException")
        void register_emailAlreadyExists_throwsBusinessException() {

            given(emailBloomFilter.mightExist("ali@test.com")).willReturn(true);
            given(userRepository.existsByEmail("ali@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("zaten kayıtlı");

            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("Bloom Filter false positive → DB kontrol eder ama email yok → kayıt başarılı")
        void register_bloomFilterFalsePositive_registersSuccessfully() {

            given(emailBloomFilter.mightExist("ali@test.com")).willReturn(true);
            given(userRepository.existsByEmail("ali@test.com")).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("$2a$hashed");
            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(userMapper.toUserResponse(any())).willReturn(UserResponse.builder().build());

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNull();
            assertThat(response.getRefreshToken()).isNull();
            then(userRepository).should().save(any(User.class));
            then(jwtService).should(never()).generateAccessToken(any());
            then(jwtService).should(never()).generateRefreshToken(any());
        }
    }

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("Başarılı giriş → token döner")
        void login_success() {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("ali@test.com");
            loginRequest.setPassword("Sifre123!");
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(null);
            given(userRepository.findByEmailAndActiveTrue("ali@test.com"))
                    .willReturn(Optional.of(savedUser));
            given(jwtService.generateAccessToken(savedUser)).willReturn("access-token");
            given(jwtService.generateRefreshToken(savedUser)).willReturn("refresh-token");
            given(userMapper.toUserResponse(savedUser)).willReturn(
                    UserResponse.builder().email("ali@test.com").build());

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("Yanlış şifre → AuthenticationManager exception fırlatır → BusinessException")
        void login_wrongPassword_throwsBusinessException() {

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("ali@test.com");
            loginRequest.setPassword("yanlisSifre");
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("hatalı");

            then(userRepository).should(never()).findByEmailAndActiveTrue(any());
        }

        @Test
        @DisplayName("Email bulunamadı veya hesap pasif → BusinessException")
        void login_userNotFound_throwsBusinessException() {
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail("yok@test.com");
            loginRequest.setPassword("Sifre123!");
            given(authenticationManager.authenticate(any())).willReturn(null);
            given(userRepository.findByEmailAndActiveTrue("yok@test.com"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Hesap bulunamadı");
        }
    }
}
