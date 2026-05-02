package com.ecommerce.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 *
 * Bu controller, API Gateway üzerinden yönlendirilen bir microservice'e
 * erişilemediği durumlarda kullanıcıya daha anlamlı bir hata mesajı döndürmek
 * için kullanılır.
 *
 * Normalde gateway bir isteği user-service, product-service gibi ilgili servise
 * yönlendirir. Ancak hedef servis kapalıysa, timeout alıyorsa vs. istek bu controller'a düşer.
 *
 * userFallback():
 * Kullanıcı servisi erişilemez olduğunda çağrılır.
 * 503 SERVICE_UNAVAILABLE döner ve kullanıcıya user-service'in geçici olarak
 * kullanılamadığını söyler.
 *
 * productFallback():
 * Ürün servisi erişilemez olduğunda çağrılır.
 * 503 SERVICE_UNAVAILABLE döner ve ürün servisi için özel mesaj verir.
 *
 * genericFallback():
 * Tanımlı olmayan veya genel fallback durumlarında çalışır.
 * Böylece hangi servis olduğu net bilinmese bile kullanıcı boş/teknik bir hata
 * yerine anlaşılır bir mesaj alır.
 *
 * Neden kullandım?
 * Microservice mimarisinde her servis her zaman ayakta olmayabilir.
 * Gateway seviyesinde fallback kullanarak servis çökmesi veya geçici bağlantı
 * problemlerinde frontend'e kontrollü, okunabilir ve standart bir response
 * dönmüş oluyoruz.
 */

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user")
    public ResponseEntity<Map<String, String>> userFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Kullanıcı servisi şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."));
    }

    @RequestMapping("/product")
    public ResponseEntity<Map<String, String>> productFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Ürün servisi şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."));
    }

    @RequestMapping("/**")
    public ResponseEntity<Map<String, String>> genericFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Servis şu anda kullanılamıyor. Lütfen daha sonra tekrar deneyin."));
    }
}
