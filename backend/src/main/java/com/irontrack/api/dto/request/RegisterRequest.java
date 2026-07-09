package com.irontrack.api.dto.request;

import com.irontrack.api.dto.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.1 — {@code POST /api/v1/auth/register}.
 */
public record RegisterRequest(
        @NotBlank(message = "Nome é obrigatório.")
        String name,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @StrongPassword
        String password
) {
}
