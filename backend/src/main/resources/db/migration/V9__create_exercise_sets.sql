-- 02_SCHEMA_SQLITE.md §2, tabela 7: exercise_sets
CREATE TABLE exercise_sets (
    id TEXT PRIMARY KEY,
    session_exercise_id TEXT NOT NULL,
    -- client_generated_id: UUID gerado no cliente, globalmente único.
    -- É o mecanismo que permite ao backend tratar reenvios da fila de sincronização
    -- offline (04_FRONTEND_UI_COMPONENTES.md §E.2) como idempotentes — um segundo
    -- INSERT com o mesmo client_generated_id falha a constraint UNIQUE abaixo, e o
    -- serviço deve tratar isso como "já persistido, retornar o registro existente",
    -- nunca como erro genérico.
    client_generated_id TEXT,
    set_number INTEGER NOT NULL CHECK(set_number > 0),
    weight REAL,                       -- Carga utilizada (kg)
    reps INTEGER,                      -- Repetições executadas
    reps_target INTEGER,               -- Meta pontual de repetições para esta série
    rpe INTEGER CHECK(rpe IS NULL OR (rpe >= 1 AND rpe <= 10)),
    notes TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (session_exercise_id) REFERENCES session_exercises(id) ON DELETE CASCADE,
    UNIQUE(session_exercise_id, set_number),
    UNIQUE(client_generated_id)
);

-- 02_SCHEMA_SQLITE.md §4: junção veloz para exibir cargas/repetições de treinos anteriores
CREATE INDEX idx_exercise_sets_session_exercise
ON exercise_sets (session_exercise_id);
