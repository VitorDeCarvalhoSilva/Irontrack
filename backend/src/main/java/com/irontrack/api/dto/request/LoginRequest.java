package com.irontrack.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.2 — {@code POST /api/v1/auth/login}.
 */
public record LoginRequest(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        String password
) {
}
