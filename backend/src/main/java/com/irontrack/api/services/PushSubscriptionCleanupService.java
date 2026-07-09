package com.irontrack.api.services;

/**
 * Remoção de {@code push_subscriptions} na exclusão de conta
 * (03_CONTRATOS_API.md §2.10, 07_ROADMAP_BACKEND.md §C.1, item 4). Isolado
 * em sua própria interface (em vez de {@code JdbcTemplate} direto em
 * {@code UserServiceImpl}) para manter a dependência de {@code UserService}
 * testável por mock, já que {@code PushSubscription}/{@code
 * PushSubscriptionRepository} completos só chegam na Sprint 5.
 */
public interface PushSubscriptionCleanupService {

    void deleteAllForUser(String userId);
}
