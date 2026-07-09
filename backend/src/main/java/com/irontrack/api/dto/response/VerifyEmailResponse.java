package com.irontrack.api.dto.response;

import java.time.Instant;

/**
 * 03_CONTRATOS_API.md §2.6 — resposta de {@code GET /auth/verify-email/{token}}.
 */
public record VerifyEmailResponse(String email, Instant verifiedAt) {
}
