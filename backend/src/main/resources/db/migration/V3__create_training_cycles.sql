-- 02_SCHEMA_SQLITE.md §2, tabela 2: training_cycles
CREATE TABLE training_cycles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 0 CHECK(is_active IN (0, 1)),
    start_date TEXT NOT NULL,
    end_date TEXT,
    -- archived_at: distingue arquivamento explícito (DELETE /cycles/{id})
    -- de um ciclo simplesmente não-ativo no momento (is_active = 0 sem archived_at setado)
    archived_at TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Garante um único ciclo ativo por usuário — índice único parcial
CREATE UNIQUE INDEX idx_cycles_user_single_active
ON training_cycles (user_id) WHERE is_active = 1;

-- 02_SCHEMA_SQLITE.md §4: otimiza a busca por ciclos de treino ativos de um usuário
CREATE INDEX idx_cycles_user_active
ON training_cycles (user_id, is_active);
