package com.irontrack.api.dto.request;

import com.irontrack.api.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.9 — {@code POST /api/v1/users/me/change-password}.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Senha atual é obrigatória.")
        String currentPassword,

        @NotBlank(message = "Nova senha é obrigatória.")
        @StrongPassword
        String newPassword
) {
}
