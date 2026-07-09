package com.irontrack.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test da Sprint 0: o contexto Spring só sobe com sucesso se as
 * migrations Flyway rodarem sem erro contra o SQLite de teste
 * (10_ESTRATEGIA_DE_TESTES.md §C) e a configuração de segurança/Swagger
 * estiver correta.
 */
@SpringBootTest
class IrontrackApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
