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
 * Tüm springdoc kullanan mikroservislerde ortak OpenAPI başlığı ve JWT security scheme.
 * Classpath'te springdoc yoksa (ör. bildirim servisi) etkinleşmez.
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
