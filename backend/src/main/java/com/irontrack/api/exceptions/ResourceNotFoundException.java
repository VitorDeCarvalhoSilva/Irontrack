package com.irontrack.api.exceptions;

/**
 * Entidade inexistente no banco de dados (01_ARQUITETURA_E_PADROES.md §4.1 — 404 Not Found).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
