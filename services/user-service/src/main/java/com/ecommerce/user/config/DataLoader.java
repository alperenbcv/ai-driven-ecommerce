package com.ecommerce.user.config;

import com.ecommerce.user.entity.RoleName;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "Sifre123!";

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@demo.com")) {
            log.info("Demo kullanıcılar zaten mevcut, atlanıyor.");
            return;
        }

        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        userRepository.save(User.builder()
                .firstName("Ali").lastName("Yılmaz")
                .email("ali@demo.com")
                .password(hash)
                .role(RoleName.USER)
                .active(true)
                .emailVerified(true)
                .build());

        userRepository.save(User.builder()
                .firstName("Satıcı Demo").lastName("Hesabı")
                .email("seller@demo.com")
                .password(hash)
                .role(RoleName.SELLER)
                .active(true)
                .emailVerified(true)
                .build());

        userRepository.save(User.builder()
                .firstName("Admin").lastName("Kullanıcı")
                .email("admin@demo.com")
                .password(hash)
                .role(RoleName.ADMIN)
                .active(true)
                .emailVerified(true)
                .build());

        log.info("Demo kullanıcılar oluşturuldu (şifre: {})", DEMO_PASSWORD);
    }
}
