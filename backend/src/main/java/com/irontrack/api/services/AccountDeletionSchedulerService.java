package com.irontrack.api.services;

/**
 * 07_ROADMAP_BACKEND.md §C.1, item 5 /
 * 11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md §B.4.
 */
public interface AccountDeletionSchedulerService {

    void deleteExpiredAccounts();
}
