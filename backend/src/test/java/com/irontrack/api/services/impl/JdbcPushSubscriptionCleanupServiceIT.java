package com.irontrack.api.services.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobertura de integração contra SQLite real (10_ESTRATEGIA_DE_TESTES.md §C)
 * do único ponto que ainda usa SQL bruto via {@code JdbcTemplate}
 * (03_CONTRATOS_API.md §2.10) — a lógica de decisão fica em
 * {@code UserServiceTest} (mock da interface), este teste confirma que o
 * DELETE de fato remove as linhas certas no banco.
 */
@SpringBootTest
class JdbcPushSubscriptionCleanupServiceIT {

    @Autowired
    private JdbcPushSubscriptionCleanupService cleanupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveRemoverApenasAsPushSubscriptionsDoUsuarioInformado() {
        insertUser("usr-alvo");
        insertUser("usr-outro");
        insertPushSubscription("sub-1", "usr-alvo", "expo-token-alvo-1");
        insertPushSubscription("sub-2", "usr-alvo", "expo-token-alvo-2");
        insertPushSubscription("sub-3", "usr-outro", "expo-token-outro");

        cleanupService.deleteAllForUser("usr-alvo");

        Integer remainingForTarget = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_subscriptions WHERE user_id = ?", Integer.class, "usr-alvo");
        Integer remainingForOther = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM push_subscriptions WHERE user_id = ?", Integer.class, "usr-outro");

        assertThat(remainingForTarget).isZero();
        assertThat(remainingForOther).isEqualTo(1);
    }

    private void insertUser(String id) {
        String now = Instant.now().toString();
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                id, "Usuário Teste " + id, id + "@email.com", "hash", now, now);
    }

    private void insertPushSubscription(String id, String userId, String expoPushToken) {
        String now = Instant.now().toString();
        jdbcTemplate.update(
                "INSERT INTO push_subscriptions (id, user_id, expo_push_token, enabled, created_at) "
                        + "VALUES (?, ?, ?, 1, ?)",
                id, userId, expoPushToken, now);
    }
}
