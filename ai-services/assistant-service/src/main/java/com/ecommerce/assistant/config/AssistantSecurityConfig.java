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
 * Assistant Service güvenlik konfigürasyonu.
 *
 * Bu servis JWT token'ı doğrudan kendisi çözmez. Çünkü JWT doğrulama işlemi Gateway tarafında yapılır.
 *
 * Gateway başarılı doğrulama yaptıktan sonra isteğe bazı header bilgileri ekler:
 *
 *   X-User-Id   → Giriş yapan kullanıcının ID bilgisi
 *   X-User-Role → Kullanıcının rolü: USER, SELLER veya ADMIN
 *
 * Bu sınıfın amacı:
 * Gateway'den gelen bu header bilgilerini okuyup Spring SecurityContext içine koymaktır.
 *
 * Neden bunu yapıyoruz?
 * Çünkü bazı endpoint veya method'larda ileride şu tarz kontroller yazmak isteyebiliriz:
 *
 *   @PreAuthorize("hasRole('ADMIN')")
 *   @PreAuthorize("hasAnyRole('USER', 'SELLER')")
 *
 * Bu annotation'ların çalışabilmesi için Spring Security'nin mevcut kullanıcının
 * rolünü bilmesi gerekir. Biz de bu bilgiyi header'lardan alıp SecurityContext'e ekliyoruz.
 *
 * Akış:
 *
 * 1. İstek Assistant Service'e gelir.
 *
 * 2. gatewayRoleFilter her istekte bir kez çalışır.
 *    Bunun için OncePerRequestFilter kullanılır.
 *
 * 3. Filter, request header içinden X-User-Id ve X-User-Role değerlerini okur.
 *
 * 4. Eğer iki header da varsa:
 *    - Kullanıcı authenticated kabul edilir.
 *    - Role bilgisi "ROLE_" prefix'i ile authority'ye çevrilir.
 *      Örnek:
 *        X-User-Role: ADMIN
 *        Spring authority: ROLE_ADMIN
 *
 * 5. UsernamePasswordAuthenticationToken oluşturulur.
 *    Burada username olarak userId kullanılır.
 *    Password/credential kullanılmaz çünkü kullanıcı zaten gateway'de doğrulanmıştır.
 *
 * 6. Oluşturulan authentication nesnesi SecurityContextHolder'a eklenir.
 *    Böylece Spring Security, bu request boyunca kullanıcının kimliğini ve rolünü bilir.
 *
 * 7. Eğer header bilgileri yoksa SecurityContext temizlenir.
 *    Bu durumda istek anonim kabul edilir.
 *
 * 8. filterChain.doFilter(...) çağrılır ve istek normal akışına devam eder.
 *
 * SecurityFilterChain tarafında:
 *
 *   CSRF kapatılır.
 *   Çünkü bu servis stateless REST API olarak çalışır.
 *
 *   Session kullanılmaz.
 *   Her request bağımsızdır, kullanıcı bilgisi session'da tutulmaz.
 *
 *   Tüm HTTP isteklerine permitAll verilir.
 *   Çünkü temel JWT kontrolü gateway'de yapılır.
 *   Bu servisteki rol bazlı kontroller ise ihtiyaç olursa @PreAuthorize ile yapılır.
 *
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
