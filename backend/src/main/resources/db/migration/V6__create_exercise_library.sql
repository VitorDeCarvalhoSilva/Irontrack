-- 02_SCHEMA_SQLITE.md §2, tabela 4: exercise_library
-- Biblioteca de exercícios de musculação (padrão + customizados pelo usuário).
CREATE TABLE exercise_library (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    primary_muscle TEXT NOT NULL,
    is_custom INTEGER NOT NULL DEFAULT 0 CHECK(is_custom IN (0, 1)),
    user_id TEXT,
    -- load_increment_kg: incremento de carga configurável por
    -- exercício (não uma tabela rígida "grupo muscular → incremento") — mais
    -- flexível, permite ajuste fino por movimento específico.
    load_increment_kg REAL NOT NULL DEFAULT 2.5,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
