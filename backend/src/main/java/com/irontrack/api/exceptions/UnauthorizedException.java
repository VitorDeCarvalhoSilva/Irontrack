package com.irontrack.api.exceptions;

/**
 * Token ausente, inválido ou expirado (01_ARQUITETURA_E_PADROES.md §4.1 — 401 Unauthorized).
 * {@code errorCode} é opcional e reflete o catálogo de
 * 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md (ex: {@code INVALID_REFRESH_TOKEN}).
 */
public class UnauthorizedException extends RuntimeException {

    private final String errorCode;

    public UnauthorizedException(String message) {
        this(message, null);
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
