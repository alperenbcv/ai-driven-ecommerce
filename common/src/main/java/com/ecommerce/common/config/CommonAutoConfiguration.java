package com.ecommerce.common.config;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;


/**
 * Common modülünün auto-configuration sınıfıdır.
 *
 * Bu proje microservice yapısında olduğu için ortak kullanılan bazı yapılar
 * ayrı bir common modülde tutuluyor. Örneğin:
 *
 * - ApiResponse
 * - PageResponse
 * - BusinessException
 * - NotFoundException
 * - GlobalExceptionHandler
 * - BaseEntity
 *
 * Her serviste GlobalExceptionHandler sınıfını tek tek component scan içine almak
 * yerine, common modül kendi auto-configuration sınıfı üzerinden bu bean'i
 * otomatik olarak uygulamaya dahil eder.
 *
 * @AutoConfiguration:
 * Bu sınıfın Spring Boot tarafından otomatik konfigürasyon sınıfı olarak
 * algılanmasını sağlar. Böylece common modülü dependency olarak ekleyen servisler,
 * gerekli ortak bean'leri otomatik alabilir.
 *
 * @ConditionalOnWebApplication(type = SERVLET):
 * Bu konfigürasyonun sadece servlet tabanlı web uygulamalarında çalışmasını sağlar.
 * Yani Spring MVC kullanan servislerde aktif olur.
 *
 * @Import(GlobalExceptionHandler.class):
 * GlobalExceptionHandler sınıfını Spring context'e manuel olarak dahil eder.
 * Böylece controller/service katmanlarında fırlatılan BusinessException,
 * NotFoundException veya validation hataları merkezi olarak yakalanır ve
 * standart hata yanıtı formatında dönülür.
 *
 * Neden böyle yaptım:
 * Her microservice içinde aynı exception handler kodunu tekrar yazmak yerine,
 * ortak hata yönetimini common modüle taşıdım. Bu sayede tüm servislerde
 * tutarlı response formatı, daha az tekrar ve merkezi bakım sağlanır.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(GlobalExceptionHandler.class)
public class CommonAutoConfiguration {
}
