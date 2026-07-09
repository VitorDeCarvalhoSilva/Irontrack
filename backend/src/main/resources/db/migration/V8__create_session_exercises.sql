-- 02_SCHEMA_SQLITE.md §2, tabela 6: session_exercises
CREATE TABLE session_exercises (
    id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    exercise_id TEXT NOT NULL,
    -- training_day_exercise_id: rastreabilidade de origem do template (06_LOGICA_DE_PROGRESSAO.md §1).
    -- Nullable e ON DELETE SET NULL: se o item do template for depois editado/removido do dia de
    -- treino, esta linha vira NULL sem afetar o histórico já copiado (session_exercises é imutável).
    training_day_exercise_id TEXT,
    order_index INTEGER NOT NULL CHECK(order_index >= 0),
    -- target_sets / target_reps_min / target_reps_max: SNAPSHOT CONGELADO da meta de
    -- training_day_exercises no exato instante em que POST /sessions/start roda — nunca
    -- relidos de training_day_exercises depois desse ponto. Garante que editar o template
    -- de um dia não altera retroativamente sessões já iniciadas/concluídas. Nullable para
    -- não quebrar sessões futuras sem template associado (caso hipotético fora do fluxo atual).
    target_sets INTEGER,
    target_reps_min INTEGER,
    target_reps_max INTEGER,
    notes TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (session_id) REFERENCES training_sessions(id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise_library(id) ON DELETE RESTRICT,
    FOREIGN KEY (training_day_exercise_id) REFERENCES training_day_exercises(id) ON DELETE SET NULL,
    UNIQUE(session_id, order_index)
);

-- 02_SCHEMA_SQLITE.md §4: sequência rápida de exercícios de uma sessão específica
CREATE INDEX idx_session_exercises_session
ON session_exercises (session_id, order_index);

-- 02_SCHEMA_SQLITE.md §4: histórico completo de execuções de um exercício da biblioteca
CREATE INDEX idx_session_exercises_exercise
ON session_exercises (exercise_id);
