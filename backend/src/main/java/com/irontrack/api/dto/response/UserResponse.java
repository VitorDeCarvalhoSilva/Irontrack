package com.irontrack.api.dto.response;

import java.time.Instant;

/**
 * 03_CONTRATOS_API.md §2.1/§2.3 — resposta de usuário (register/me).
 */
public record UserResponse(
        String id,
        String name,
        String email,
        Instant emailVerifiedAt,
        Instant createdAt
) {
}
