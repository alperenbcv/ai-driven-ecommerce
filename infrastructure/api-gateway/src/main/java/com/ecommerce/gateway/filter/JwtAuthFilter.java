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

                // Downstream: yalnızca doğrulanmış JWT claim'leri (önceden strip edilen spoof header'ların üzerine)
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
        // Gerektiğinde route-level konfigürasyon eklenebilir
    }
}
