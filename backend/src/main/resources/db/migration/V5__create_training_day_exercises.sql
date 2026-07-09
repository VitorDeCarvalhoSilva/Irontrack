-- 02_SCHEMA_SQLITE.md §2, tabela 3.1: training_day_exercises (template do dia de treino)
-- Vincula um training_day aos exercícios que o compõem, com metas planejadas.
-- É o dado de entrada que falta para o motor de sobrecarga
-- progressiva (06_LOGICA_DE_PROGRESSAO.md) avaliar "atingiu o teto de repetições".
-- A faixa target_reps_min/target_reps_max existe porque o produto pede metas do
-- tipo "3x8-12" (faixa), enquanto exercise_sets.reps_target continua sendo um
-- único inteiro — o valor específico que o usuário deve buscar NAQUELA série
-- concreta dentro da faixa (definido pela lógica de progressão em 06, não pelo
-- template). O template define a faixa; a sessão/série concreta recebe uma
-- meta pontual dentro dela.
--
-- Nota: referencia exercise_library(id), criada apenas na migration seguinte
-- (V6) — válido em SQLite, que não valida a existência da tabela referenciada
-- no momento do CREATE TABLE, apenas quando PRAGMA foreign_keys=ON avalia a
-- constraint em INSERT/UPDATE/DELETE. Ordem preservada conforme
-- 02_SCHEMA_SQLITE.md §2 (tabela 3.1 antes da tabela 4).
CREATE TABLE training_day_exercises (
    id TEXT PRIMARY KEY,
    training_day_id TEXT NOT NULL,
    exercise_id TEXT NOT NULL,
    order_index INTEGER NOT NULL CHECK(order_index >= 0),
    target_sets INTEGER NOT NULL CHECK(target_sets > 0),
    target_reps_min INTEGER NOT NULL CHECK(target_reps_min > 0),
    target_reps_max INTEGER NOT NULL CHECK(target_reps_max >= target_reps_min),
    notes TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (training_day_id) REFERENCES training_days(id) ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES exercise_library(id) ON DELETE RESTRICT,
    UNIQUE(training_day_id, order_index)
);
