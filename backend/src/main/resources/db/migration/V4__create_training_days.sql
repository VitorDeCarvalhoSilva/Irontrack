-- 02_SCHEMA_SQLITE.md §2, tabela 3: training_days
CREATE TABLE training_days (
    id TEXT PRIMARY KEY,
    cycle_id TEXT NOT NULL,
    name TEXT NOT NULL,
    color_code TEXT NOT NULL DEFAULT '#000000',
    order_index INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (cycle_id) REFERENCES training_cycles(id) ON DELETE CASCADE,
    UNIQUE(cycle_id, order_index)
);
