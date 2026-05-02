package com.ecommerce.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Şifre ve şifre onayı eşleşiyor mu diye kontrol eden class-level annotation.
 *
 * @Target(TYPE) : Bu annotation bir sınıfa uygulanır field'a değil,
 * çünkü iki farklı field'ı karşılaştırmamız gerekiyor.
 *
 * @Constraint(validatedBy = ...) : Bu annotation kullanıldığında hangi
 * Validator sınıfının çalıştırılacağını belirtir.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {

    String message() default "Şifreler eşleşmiyor";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
