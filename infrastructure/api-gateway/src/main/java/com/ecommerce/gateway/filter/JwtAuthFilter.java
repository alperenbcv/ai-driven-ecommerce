package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 *
 * Bu filter, Spring Cloud Gateway üzerinden geçen protected endpoint isteklerinde
 * Authorization header içindeki Bearer JWT token'ı doğrular.
 *
 * Neden gateway'de kullandım:
 * Mikroservis mimarisinde her servisin JWT parse etmesi yerine merkezi bir noktada
 * token doğrulaması yapmak daha pratiktir. Gateway token'ı doğrular, kullanıcıya ait
 * temel bilgileri iç servislere header olarak aktarır.
 *
 * Temel akış:
 * 1. İstekten Authorization header okunur.
 * 2. Header yoksa veya "Bearer " ile başlamıyorsa 401 Unauthorized döndürülür.
 * 3. Bearer prefix'i çıkarılarak gerçek JWT token alınır.
 * 4. Token jwt.secret ile doğrulanır ve içindeki claim'ler parse edilir.
 * 5. Token içinden userId, role ve email bilgileri alınır.
 * 6. Bu bilgiler X-User-Id, X-User-Role ve X-User-Email header'ları olarak iç servislere eklenir.
 * 7. İstek, güncellenmiş header'larla ilgili mikroservise yönlendirilir.
 *
 * AbstractGatewayFilterFactory:
 * Spring Cloud Gateway'de custom filter yazmak için kullanılan temel sınıftır.
 * Bu sınıf sayesinde filter application.yml içinde route bazında kullanılabilir.
 *
 *
 * Config tarafını ekledim fakat şuan rol bazlı bir ayar vs. ekleyemedim bu nedenle şimdilik boş.
 *
 * parseClaims():
 * JWT token'ı jwt.secret ile doğrular. İmza geçersizse, token bozuksa veya
 * token süresi dolmuşsa exception fırlatır.
 *
 * unauthorized():
 * Token geçersiz olduğunda response status 401 yapılır ve request zinciri
 * devam ettirilmez. Böylece istek backend servislere ulaşmadan gateway'de kesilir.
 *
 * Header temizleme:
 * İç servislere X-User-Id, X-User-Role ve X-User-Email header'ları eklenmeden önce
 * aynı isimli eski header'lar silinir. Böylece client'ın sahte X-User-Role gibi
 * header göndererek kendini ADMIN göstermesi engellenir.
 */

@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Authorization header eksik veya geçersiz");
            }

            String token = authHeader.substring(7);
            try {
                Claims claims = parseClaims(token);
                String userId = claims.getSubject();
                if (userId == null || userId.isBlank()) {
                    return unauthorized(exchange, "Token içinde kullanıcı kimliği yok");
                }
                String role = claims.get("role", String.class);
                String email = claims.get("email", String.class);

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(req -> req.headers(headers -> {
                            headers.remove("X-User-Id");
                            headers.remove("X-User-Role");
                            headers.remove("X-User-Email");
                            headers.add("X-User-Id", userId);
                            headers.add("X-User-Role", role != null ? role : "USER");
                            headers.add("X-User-Email", email != null ? email : "");
                        }))
                        .build();

                return chain.filter(mutatedExchange);
            } catch (Exception e) {
                log.warn("JWT doğrulama hatası: {}", e.getMessage());
                return unauthorized(exchange, "Geçersiz veya süresi dolmuş token");
            }
        };
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.debug("Yetkisiz erişim: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
