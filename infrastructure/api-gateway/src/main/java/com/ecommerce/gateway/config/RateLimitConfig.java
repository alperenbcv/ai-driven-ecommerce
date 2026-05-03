package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 *
 *
 * Rate limiting mantığında her kullanıcı ve ip için ayrı bir sayaç tutulur.
 * KeyResolver ise bu sayacın anahtarını üretir.
 *
 * Örneğin:
 * - Giriş yapmamış kullanıcı için: ip:192.168.1.10
 * - Giriş yapmış kullanıcı için: token:hashedJwtValue
 *
 * ipKeyResolver():
 * Sadece client ip adresine göre rate limit anahtarı üretir.
 * Özellikle public endpointlerde ip bazlı sınırlama için kullanılabilir.
 *
 * principalOrIpKeyResolver():
 * Öncelikli kullanılan KeyResolver'dır.
 *
 * @Primary:
 * Birden fazla KeyResolver bean'i olduğu için Spring'in varsayılan olarak
 * hangisini kullanacağını belirtir. Burada ana resolver olarak
 * principalOrIpKeyResolver seçilir.
 *
 * Çalışma mantığı:
 * 1. Request içinde Authorization header var mı kontrol edilir.
 * 2. Header "Bearer ..." formatındaysa JWT token alınır.
 * 3. Token doğrudan Redis key olarak kullanılmaz SHA-256 ile hashlenir.
 * 4. Böylece rate limit anahtarı token bazlı olur.
 * 5. Token yoksa kullanıcı anonymous kabul edilir ve IP adresi kullanılır.
 *
 * Neden token'ı hashledim:
 * JWT access token hassas bir bilgidir. Redis key'lerinde veya loglarda
 * token'ın düz metin olarak görünmesini istemeyiz. Bu yüzden token SHA-256 ile
 * tek yönlü hashlenir ve yalnızca hash değeri key olarak kullanılır.
 *
 * clientIp():
 * Kullanıcının IP adresini bulur.
 *
 * Önce X-Forwarded-For header'ına bakılır. Çünkü gateway-load balancer-proxy vs.
 * arkasında gerçek client IP genellikle bu header içinde gelir.
 *
 * X-Forwarded-For birden fazla IP içerebilir:
 *   client, proxy1, proxy2
 *
 * Bu yüzden ilk IP alınır.
 *
 * Eğer X-Forwarded-For yoksa request'in remoteAddress bilgisi kullanılır.
 * O da yoksa "unknown" döndürülür.
 *
 * Base64 URL encoder kullanılır çünkü Redis key içinde güvenli şekilde
 * saklanabilecek, slash veya özel karakter problemi çıkarmayan bir format üretir.
 *
 * Eğer SHA-256 hesaplanırken beklenmeyen bir hata olursa fallback olarak
 * Java hashCode değeri kullanılır. 
 *
 * Neden kullandım :
 * Gateway seviyesinde rate limiting yaparak tek bir kullanıcının veya IP'nin
 * kısa sürede çok fazla istek atmasını engelliyoruz. Bu hem brute force,
 * spam ve kötüye kullanım riskini azaltır hem de backend servislerini gereksiz
 * trafik yükünden korur.
 */

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(clientIp(exchange));
    }
    @Bean
    @Primary
    public KeyResolver principalOrIpKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return Mono.just("token:" + sha256(authHeader.substring(7)));
            }

            return Mono.just("ip:" + clientIp(exchange));
        };
    }

    private static String clientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
