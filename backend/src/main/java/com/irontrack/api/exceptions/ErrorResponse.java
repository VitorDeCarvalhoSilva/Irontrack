package com.irontrack.api.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Payload de erro padronizado (03_CONTRATOS_API.md §1.4): timestamp, status,
 * error, message, path. {@code errorCode} é um campo adicional opcional
 * (14_CATALOGO_DE_ERROS_DE_NEGOCIO.md) — omitido do JSON quando nulo via
 * {@link JsonInclude}, preservando o payload exato de 5 campos para erros
 * genéricos enquanto já preparado para receber o código das exceções de
 * negócio das próximas sprints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String errorCode
) {

    public ErrorResponse(int status, String error, String message, String path, String errorCode) {
        this(Instant.now(), status, error, message, path, errorCode);
    }
}
