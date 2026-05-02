package com.ecommerce.user.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP ile service katmanı method-level loglama.
 *
 * @Around → metot çağrısını sarar; öncesinde ve sonrasında kod çalıştırır.
 * pointcut → com.ecommerce.user.service paketi altındaki tüm public metodlar.
 *
 * Ne logluyor?
 * - Hangi metot çağrıldı
 * - Kaç ms sürdü
 * - Hata varsa exception mesajı
 *
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.ecommerce.user.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        log.debug("→ {} çağrıldı", methodName);
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("← {} tamamlandı ({}ms)", methodName, elapsed);
            return result;
        } catch (Exception ex) {
            log.error("✗ {} hata fırlattı: {}", methodName, ex.getMessage());
            throw ex;
        }
    }
}
