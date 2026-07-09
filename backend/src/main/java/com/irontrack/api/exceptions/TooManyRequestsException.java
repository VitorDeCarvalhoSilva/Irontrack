package com.irontrack.api.exceptions;

/**
 * Rate limit excedido (05_DEVOPS_E_SEGURANCA.md §E.2 — 429 Too Many
 * Requests). {@code errorCode} é opcional e reflete o catálogo de
 * 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md (ex: {@code TOO_MANY_LOGIN_ATTEMPTS}).
 */
public class TooManyRequestsException extends RuntimeException {

    private final String errorCode;

    public TooManyRequestsException(String message) {
        this(message, null);
    }

    public TooManyRequestsException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
