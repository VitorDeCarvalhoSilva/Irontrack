package com.irontrack.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.10 — {@code DELETE /api/v1/users/me}.
 */
public record DeleteAccountRequest(
        @NotBlank(message = "Senha é obrigatória.")
        String password
) {
}
