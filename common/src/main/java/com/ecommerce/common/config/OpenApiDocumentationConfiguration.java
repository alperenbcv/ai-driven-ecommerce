package com.ecommerce.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
/**
 * Common modül üzerinden tüm servisler için ortak Swagger / OpenAPI ayarı sağlar.
 *
 * Bu proje microservice yapısında olduğu için her servisin kendi Swagger dokümantasyonu
 * vardır. Ancak hepsinde aynı güvenlik şeması, benzer başlık formatı ve ortak açıklama
 * kullanmak istediğimiz için OpenAPI konfigürasyonunu common modüle aldım.
 *
 * @AutoConfiguration:
 * Bu sınıfı Spring Boot auto-configuration mekanizmasına dahil eder.
 * Böylece common modülü dependency olarak kullanan servislerde bu OpenAPI ayarı
 * otomatik olarak aktif olabilir.
 *
 * @ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI"):
 * Bu konfigürasyon sadece Swagger/OpenAPI dependency'si classpath'te varsa çalışır.
 * Yani bir servis springdoc-openapi kullanmıyorsa bu bean oluşturulmaya çalışılmaz.
 * Bu da common modülün farklı servislerde esnek kullanılmasını sağlar.
 *
 * @Bean:
 * ecommerceOpenApi metodu bir OpenAPI bean'i üretir.
 * Springdoc bu bean'i okuyarak Swagger UI üzerinde servis başlığı, açıklaması,
 * server bilgisi ve security ayarlarını gösterir.
 *
 * @ConditionalOnMissingBean(OpenAPI.class):
 * Eğer ilgili servis kendi özel OpenAPI bean'ini tanımlamışsa, common modüldeki
 * varsayılan bean devreye girmez. Böylece servis bazlı override imkanı korunur.
 *
 * @Value("${spring.application.name:application}"):
 * Servisin application.yml içindeki adını okur.
 * Örneğin product-service için başlık otomatik olarak
 * "E-Commerce — Product service" gibi oluşturulur.
 * Eğer değer yoksa varsayılan olarak "application" kullanılır.
 *
 * @Value("${server.port:8080}"):
 * Servisin port bilgisini okur ve Swagger'daki server URL'ine ekler.
 * Bu sayede Swagger UI üzerinden doğrudan ilgili servisin lokal portuna istek atılabilir.
 *
 * OpenAPI içeriğinde:
 * - info(): API başlığı, açıklama, versiyon, iletişim ve lisans bilgisini tanımlar.
 * - servers(): Swagger UI'da kullanılacak servis adresini gösterir.
 * - addSecurityItem(): Endpoint'lerin Bearer JWT ile korunabileceğini belirtir.
 * - components().addSecuritySchemes(): Swagger UI'daki Authorize butonunda
 *   JWT token girilebilmesi için bearer authentication şemasını tanımlar.
 *
 * Neden kullandım?
 * Her serviste aynı Swagger ayarlarını tekrar tekrar yazmak yerine common modüle
 * aldım. Böylece bütün microservice'lerde tutarlı API dokümantasyonu, ortak JWT
 * güvenlik tanımı ve merkezi bakım sağlanmış oldu.
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
public class OpenApiDocumentationConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI ecommerceOpenApi(
            @Value("${spring.application.name:application}") String applicationName,
            @Value("${server.port:8080}") String serverPort) {
        String readable = applicationName.replace('-', ' ');
        String titleFirst = readable.substring(0, 1).toUpperCase() + readable.substring(1);
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce — " + titleFirst)
                        .description("""
                                Mikroservis REST API. Frontend ve gateway çağrılarında çoğu uç nokta \
                                **Bearer JWT** gerektirir; `Authorization: Bearer <access_token>` başlığını ekleyin. \
                                Swagger UI'da **Authorize** düğmesinden token girebilirsiniz.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("E-Commerce Bootcamp").email("dev@localhost"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Doğrudan servis portu (Swagger / Postman)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerJwt"))
                .components(new Components().addSecuritySchemes("bearerJwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("`POST /api/auth/login` yanıtındaki access token")));
    }
}
