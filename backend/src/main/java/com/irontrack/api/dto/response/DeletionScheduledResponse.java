package com.irontrack.api.dto.response;

import java.time.Instant;

/**
 * 03_CONTRATOS_API.md §2.10 — resposta de {@code DELETE /api/v1/users/me}.
 */
public record DeletionScheduledResponse(Instant deletionScheduledFor) {
}
