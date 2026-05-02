package com.ecommerce.assistant.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Assistant servisi için güvenlik yapılandırması.
 * <p>
 * JWT doğrulaması API Gateway üzerindedir; bu serviste token parse edilmez.
 * Gateway istemciden gelen isteği doğruladıktan sonra kimlik bilgisini başlıklarla iletir;
 * buradaki {@link #gatewayRoleFilter()} bu başlıklardan ({@code X-User-Id}, {@code X-User-Role})
 * {@link SecurityContextHolder} içinde bir {@link org.springframework.security.core.Authentication}
 * oluşturur böylece denetleyici veya servisteki {@link org.springframework.security.access.prepost.PreAuthorize}
 * ifadeleri çalışır.
 * <p>
 * HTTP katmanında tüm istekler {@code anyRequest().permitAll()} ile serbest bırakılır; erişim kısıtları
 * gerekiyorsa method security ({@code @PreAuthorize} vb.) ile verilir. Oturum kullanılmaz
 * ({@link SessionCreationPolicy#STATELESS}), CSRF kapalıdır (durumsuz API).
 */
@Configuration
@EnableMethodSecurity
public class AssistantSecurityConfig {

    @Bean
    public OncePerRequestFilter gatewayRoleFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {

                String role = request.getHeader("X-User-Role");
                String userId = request.getHeader("X-User-Id");
                if (role != null && userId != null) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    SecurityContextHolder.clearContext();
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public SecurityFilterChain assistantSecurity(HttpSecurity http,
                                                 OncePerRequestFilter gatewayRoleFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(gatewayRoleFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
