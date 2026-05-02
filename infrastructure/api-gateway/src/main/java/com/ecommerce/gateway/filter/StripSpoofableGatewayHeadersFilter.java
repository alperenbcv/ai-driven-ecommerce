package com.ecommerce.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * Client tarafından gönderilebilecek sahte gateway user header'larını temizleyen filtre.
 *
 * Bu filter bütün Gateway isteklerinde çalışır ve request içindeki:
 * - X-User-Id
 * - X-User-Role
 * - X-User-Email header'larını siler.
 *
 * Neden gerekli :
 * İç servisler kullanıcı bilgisini çoğunlukla bu header'lardan okuyor.
 * Örneğin Product Service, Order Service veya Assistant Service:
 *
 *   X-User-Id   → işlemi yapan kullanıcı
 *   X-User-Role → USER / SELLER / ADMIN bilgisi gibi değerleri Gateway'den gelmiş kabul ediyor.
 *
 * Eğer client doğrudan bu header'ları kendisi gönderirse: X-User-Role: ADMIN
 *
 * gibi sahte bir değerle kendini admin gibi göstermeye çalışabilir.
 * Bu nedenle Gateway, dışarıdan gelen bu header'ları önce temizler.
 *
 * Akış:
 * 1. Request Gateway'e gelir.
 * 2. Bu GlobalFilter en yüksek öncelikle çalışır.
 * 3. Client'ın gönderdiği X-User-* header'ları silinir.
 * 4. Daha sonra JwtAuthFilter gibi authentication filter'ları çalışır.
 * 5. JWT doğrulandıktan sonra güvenilir X-User-* header'ları Gateway tarafından yeniden eklenir.
 * 6. İç servisler artık client'ın değil, Gateway'in eklediği güvenilir header'ları okur.
 *
 * GlobalFilter:
 * Bu filter route bazlı değil, Gateway'den geçen bütün request'lerde çalışır.
 *
 * Ordered.HIGHEST_PRECEDENCE:
 * Filter'ın mümkün olan en erken sırada çalışmasını sağlar.
 * Böylece JwtAuthFilter veya başka filter'lar user header eklemeden önce
 * dışarıdan gelen sahte değerler temizlenmiş olur.
 *
 */
@Component
public class StripSpoofableGatewayHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                    h.remove("X-User-Email");
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
