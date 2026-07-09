package com.irontrack.api.dto.request;

import com.irontrack.api.dto.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * 03_CONTRATOS_API.md §2.7 — {@code POST /api/v1/auth/reset-password}.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "token é obrigatório.")
        String token,

        @NotBlank(message = "Senha é obrigatória.")
        @StrongPassword
        String newPassword
) {
}
