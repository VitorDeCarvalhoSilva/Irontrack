package com.irontrack.api.exceptions;

/**
 * Erro de negócio que não é uma falha de validação de Bean Validation, mas
 * ainda assim é `400 Bad Request` (ex: token de verificação inválido/expirado
 * — 01_ARQUITETURA_E_PADROES.md §4.1). {@code errorCode} é opcional e reflete
 * o catálogo de 14_CATALOGO_DE_ERROS_DE_NEGOCIO.md.
 */
public class BadRequestException extends RuntimeException {

    private final String errorCode;

    public BadRequestException(String message) {
        this(message, null);
    }

    public BadRequestException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
