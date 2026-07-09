package com.irontrack.api.dto.response;

/**
 * Resposta genérica de mensagem — usada por
 * {@code POST /auth/forgot-password} (03_CONTRATOS_API.md §2.7), que sempre
 * responde `202 Accepted` com uma mensagem genérica, nunca revelando se o
 * e-mail existe na base.
 */
public record MessageResponse(String message) {
}
