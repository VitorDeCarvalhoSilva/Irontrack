package com.irontrack.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.4 ({@code POST /auth/refresh}) e §2.5
 * ({@code POST /auth/logout}) — mesmo formato de request nos dois endpoints.
 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken é obrigatório.")
        String refreshToken
) {
}
