package com.ecommerce.user.validation;

import com.ecommerce.user.dto.request.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @PasswordMatch annotation'ının gerçek doğrulama mantığı.
 *
 * ConstraintValidator<AnnotationType, ValidatedObjectType>
 *   → RegisterRequest nesnesi üzerinde çalışır.
 *
 */
public class PasswordMatchValidator
        implements ConstraintValidator<PasswordMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request.getPassword() == null || request.getConfirmPassword() == null) {
            return true;
        }
        return request.getPassword().equals(request.getConfirmPassword());
    }
}
