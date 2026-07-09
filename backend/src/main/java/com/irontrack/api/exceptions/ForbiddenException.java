package com.irontrack.api.exceptions;

/**
 * Usuário autenticado tentando acessar recurso de outro usuário, ou conta
 * bloqueada por regra de negócio (01_ARQUITETURA_E_PADROES.md §4.1 — 403
 * Forbidden). {@code errorCode} é opcional e reflete o catálogo de
 * 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md (ex: {@code EMAIL_NOT_VERIFIED}).
 */
public class ForbiddenException extends RuntimeException {

    private final String errorCode;

    public ForbiddenException(String message) {
        this(message, null);
    }

    public ForbiddenException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
