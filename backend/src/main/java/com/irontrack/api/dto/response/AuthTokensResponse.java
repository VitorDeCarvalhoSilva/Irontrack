package com.irontrack.api.dto.response;

/**
 * 03_CONTRATOS_API.md §2.2/§2.4 — resposta de login/refresh.
 */
public record AuthTokensResponse(String accessToken, String refreshToken) {
}
