-- 02_SCHEMA_SQLITE.md §2, tabela 5: training_sessions
CREATE TABLE training_sessions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    training_day_id TEXT NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT,
    status TEXT NOT NULL DEFAULT 'IN_PROGRESS' CHECK(status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (training_day_id) REFERENCES training_days(id) ON DELETE RESTRICT
);

-- 02_SCHEMA_SQLITE.md §4: listagem de sessões do usuário por data (histórico)
CREATE INDEX idx_sessions_user_start
ON training_sessions (user_id, start_time DESC);

-- 02_SCHEMA_SQLITE.md §4: busca pelo "último treino do mesmo dia" (lastPerformance)
CREATE INDEX idx_sessions_user_day_start
ON training_sessions (user_id, training_day_id, start_time DESC);
