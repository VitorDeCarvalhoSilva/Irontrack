package com.irontrack.api.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Regra de força de senha compartilhada entre todo endpoint que define/troca
 * senha (03_CONTRATOS_API.md §2.7/§2.9: mínimo 8 caracteres, letras e
 * números) — um único lugar para a regra em vez de repetir o regex em cada
 * DTO (`RegisterRequest`, `ResetPasswordRequest`, `ChangePasswordRequest`).
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "A senha deve ter no mínimo 8 caracteres contendo letras e números.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
