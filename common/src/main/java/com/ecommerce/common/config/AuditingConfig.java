package com.ecommerce.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 *
 * Bu sınıf, entity'lerdeki audit alanlarının otomatik doldurulmasını sağlar.
 * Örneğin BaseEntity içinde aşağıdaki gibi alanlar var:
 *
 * - createdAt
 * - updatedAt
 * - createdBy
 * - updatedBy
 *
 * Spring Data JPA bu alanları entity save/update işlemlerinde otomatik set edebilir.
 *
 * @EnableJpaAuditing:
 * JPA auditing mekanizmasını aktif eder.
 *
 * auditorAwareRef = "auditorProvider":
 * createdBy / updatedBy gibi alanlara hangi kullanıcı adının yazılacağını belirleyen
 * AuditorAware bean'ini gösterir.
 *
 * AuditorAware<String>:
 * Spring Data JPA'ya "bu işlemi yapan kullanıcı kim?" sorusunun cevabını verir.
 * Burada String dönüyoruz çünkü createdBy / updatedBy alanları String olarak tutuluyor.
 *
 * SecurityContextHolder:
 * Spring Security'nin mevcut request için tuttuğu authentication bilgisidir.
 * Kullanıcı JWT ile giriş yaptıysa burada authenticated user bilgisi bulunur.
 *
 * Eğer kullanıcı yoksa:
 * - auth null ise
 * - kullanıcı authenticate olmamışsa
 * - principal anonymousUser ise
 *
 * "system" döndürülür.
 *
 * Neden "system" dönüyor:
 * Bazı kayıtlar kullanıcı isteğiyle değil, sistem tarafından oluşturulur.
 * Örneğin DataLoader, event listener, background işlem veya public endpoint çağrısı sırasında
 * aktif kullanıcı olmayabilir. Bu durumda audit alanlarının boş kalmaması için
 * işlemi yapan taraf "system" olarak işaretlenir.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
