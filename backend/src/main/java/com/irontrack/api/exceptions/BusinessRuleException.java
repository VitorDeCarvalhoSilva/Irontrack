package com.irontrack.api.exceptions;

/**
 * Violação de regra de negócio (01_ARQUITETURA_E_PADROES.md §4.1 — 422 Unprocessable Entity).
 * {@code errorCode} é opcional e reflete o catálogo de
 * 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md — nenhuma exceção de negócio concreta é
 * lançada ainda nesta sprint; a classe já existe para as próximas sprints
 * estenderem/lançarem com o código correto sem precisar tocar no handler global.
 */
public class BusinessRuleException extends RuntimeException {

    private final String errorCode;

    public BusinessRuleException(String message) {
        this(message, null);
    }

    public BusinessRuleException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
