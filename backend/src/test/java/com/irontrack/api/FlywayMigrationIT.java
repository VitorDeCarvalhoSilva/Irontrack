package com.irontrack.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica, contra um SQLite real (10_ESTRATEGIA_DE_TESTES.md §C — nunca H2),
 * que as migrations Flyway (07_ROADMAP_BACKEND.md §C.0, item 3) criaram todas
 * as tabelas de 02_SCHEMA_SQLITE.md §2, na ordem de dependência de FK
 * documentada, e que PRAGMA foreign_keys está ativo por conexão
 * (02_SCHEMA_SQLITE.md §2).
 */
@SpringBootTest
class FlywayMigrationIT {

    private static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "refresh_tokens",
            "training_cycles",
            "training_days",
            "training_day_exercises",
            "exercise_library",
            "training_sessions",
            "session_exercises",
            "exercise_sets",
            "exercise_set_techniques",
            "stagnation_alerts",
            "push_subscriptions"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveCriarTodasAsTabelasDoSchema() {
        List<String> existingTables = jdbcTemplate.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' "
                        + "AND name NOT LIKE 'flyway_%'",
                String.class);

        assertThat(existingTables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
    }

    @Test
    void devePermitirForeignKeysAtivadoPorConexao() {
        Integer foreignKeysStatus = jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class);

        assertThat(foreignKeysStatus).isEqualTo(1);
    }
}
