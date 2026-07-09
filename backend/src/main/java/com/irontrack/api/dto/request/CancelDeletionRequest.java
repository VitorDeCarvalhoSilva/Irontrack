package com.irontrack.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.11 — {@code POST /api/v1/auth/cancel-deletion}.
 * Reverificação direta de {@code email}+{@code password}, sem token/link
 * por e-mail (simplificação deliberada — ver §2.11).
 */
public record CancelDeletionRequest(
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        String password
) {
}
