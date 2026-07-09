-- 02_SCHEMA_SQLITE.md §2, tabela 9: stagnation_alerts
-- (06_LOGICA_DE_PROGRESSAO.md §D, 07_ROADMAP_BACKEND.md §C.4)
-- Materializa a detecção de estagnação (lógica já formalizada em 06 §D) como
-- alerta persistente. resolved_at NULL = alerta em aberto; snoozed_until
-- permite adiar a exibição sem marcar como resolvido.
CREATE TABLE stagnation_alerts (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    exercise_id TEXT NOT NULL,
    training_day_id TEXT NOT NULL,
    detected_at TEXT NOT NULL,
    snoozed_until TEXT,
    resolved_at TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise_library(id) ON DELETE CASCADE,
    FOREIGN KEY (training_day_id) REFERENCES training_days(id) ON DELETE CASCADE
);
CREATE INDEX idx_stagnation_alerts_user_active
ON stagnation_alerts (user_id, resolved_at);
