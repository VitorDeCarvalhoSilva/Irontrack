package com.irontrack.api.dto.request;

import jakarta.validation.constraints.Email;

/**
 * 03_CONTRATOS_API.md §2.8 — {@code PATCH /api/v1/users/me}. Todos os campos
 * são opcionais (atualização parcial) — sem {@code @NotBlank}.
 */
public record UpdateProfileRequest(
        String name,

        @Email(message = "E-mail inválido.")
        String email
) {
}
