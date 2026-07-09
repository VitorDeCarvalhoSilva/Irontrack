-- 02_SCHEMA_SQLITE.md §2, tabela 8: exercise_set_techniques
-- Múltiplas técnicas de intensificação por série (relação N:N). O campo
-- `exercise_sets.notes` continua existindo para observações em texto livre
-- (ex: "pausa técnica de 2 seg"), mas a técnica em si — usada pelo motor de
-- progressão para decidir se uma série "conta" normalmente para avaliação de
-- teto de reps — vive exclusivamente aqui, nunca inferida do texto livre.
CREATE TABLE exercise_set_techniques (
    exercise_set_id TEXT NOT NULL,
    technique TEXT NOT NULL CHECK(technique IN (
        'FALHA', 'DROP_SET', 'REST_PAUSE', 'PAUSA', 'NEGATIVA_FORCADA'
    )),
    PRIMARY KEY (exercise_set_id, technique),
    FOREIGN KEY (exercise_set_id) REFERENCES exercise_sets(id) ON DELETE CASCADE
);
