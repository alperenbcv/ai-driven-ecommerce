package com.ecommerce.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Discovery Service güvenlik konfigürasyonu.
 *
 * Bu servis Eureka Server olarak çalıştığı için diğer mikroservisler buraya
 * kayıt olur ve birbirlerini service name üzerinden bulabilir.
 *
 * Bu konfigürasyonda:
 *
 * 1. @Configuration
 *    Bu sınıfın Spring tarafından konfigürasyon sınıfı olarak okunmasını sağlar.
 *
 * 2. @EnableWebSecurity
 *    Spring Security'yi bu servis için aktif hale getirir.
 *
 * 3. SecurityFilterChain
 *    HTTP güvenlik kurallarını tanımlar.
 *
 * 4. csrf.ignoringRequestMatchers("/eureka/**")
 *    Eureka client servisleri Eureka Server'a kayıt olurken POST/PUT gibi
 *    istekler gönderir. CSRF aktif kalırsa bu istekler engellenebilir.
 *    Bu yüzden sadece /eureka/** endpoint'leri için CSRF kontrolü devre dışı bırakılır.
 *
 * 5. /actuator/**
 *    Health check ve monitoring endpoint'leri için public bırakılır.
 *    Docker, gateway veya monitoring araçları servisin ayakta olup olmadığını
 *    buradan kontrol edebilir.
 *
 * 6. anyRequest().authenticated()
 *    Eureka dashboard ve diğer discovery endpoint'lerine erişim için
 *    authentication zorunlu tutulur.
 *
 * 7. httpBasic()
 *    Basit username/password doğrulaması kullanılır.
 *    Discovery service kullanıcıya açık bir servis olmadığı için JWT yerine
 *    basic authentication yeterli.
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});
        return http.build();
    }
}
