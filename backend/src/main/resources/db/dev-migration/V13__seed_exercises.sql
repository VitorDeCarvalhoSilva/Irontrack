-- Seed de dados de desenvolvimento (07_ROADMAP_BACKEND.md §C.0, item 4;
-- 02_SCHEMA_SQLITE.md §3.2) — biblioteca padrão de exercícios de musculação
-- cobrindo peito, costas, ombros, bíceps, tríceps, pernas, glúteos e core,
-- expandida a partir da amostra de 5 exercícios documentada em
-- 02_SCHEMA_SQLITE.md §3.2 para uma lista mais representativa dos +50
-- exercícios mencionados em 00_PRD_IRONTRACK.md EP-02/Sprint 2.
--
-- Localizada em db/dev-migration/ (não em db/migration/) para que só rode
-- quando spring.flyway.locations inclui essa pasta — ativado exclusivamente
-- pelo perfil "dev" (application-dev.properties), nunca em produção
-- (07_ROADMAP_BACKEND.md §C.0, item 4: "restrito ao perfil dev... desde que
-- não rode em produção").
--
-- Critério de load_increment_kg (02_SCHEMA_SQLITE.md §3.2): ~2.5kg para
-- exercícios de membros superiores/isolados; ~5.0kg para exercícios
-- compostos de membros inferiores (maior tolerância neuromuscular a
-- incrementos). Nenhum exercício isométrico/sem carga (ex: prancha) foi
-- incluído — fora do escopo do domínio (01_ARQUITETURA_E_PADROES.md §7, ADR-016).

-- Peito
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0001-bench', 'Supino Reto com Barra', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0002-incline-bench', 'Supino Inclinado com Barra', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0003-decline-bench', 'Supino Declinado com Barra', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0004-dumbbell-bench', 'Supino Reto com Halteres', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0005-incline-dumbbell', 'Supino Inclinado com Halteres', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0006-chest-fly', 'Crucifixo com Halteres', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0007-cable-crossover', 'Crucifixo no Cross-Over', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0008-pec-deck', 'Peck Deck (Voador)', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0009-dips-chest', 'Paralelas (Mergulho para Peito)', 'Peito', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z');

-- Costas
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0011-row', 'Remada Curvada', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0014-pulldown', 'Puxada Frontal (Pulley)', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0015-pullup', 'Barra Fixa (Pull-up)', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0016-seated-row', 'Remada Sentada (Cabo)', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0017-t-bar-row', 'Remada Cavalinho (T-Bar)', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0018-one-arm-row', 'Remada Unilateral com Halter', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0019-deadlift', 'Levantamento Terra', 'Costas', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z'),
('exe-0020-hyperextension', 'Hiperextensão Lombar', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0021-straight-arm-pulldown', 'Pull-over no Pulley (Braços Estendidos)', 'Costas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z');

-- Ombros
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0012-ohp', 'Desenvolvimento Militar', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0022-dumbbell-shoulder-press', 'Desenvolvimento com Halteres', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0023-lateral-raise', 'Elevação Lateral', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0024-front-raise', 'Elevação Frontal', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0025-rear-delt-fly', 'Crucifixo Invertido (Deltoide Posterior)', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0026-upright-row', 'Remada Alta', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0027-face-pull', 'Face Pull', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0028-arnold-press', 'Desenvolvimento Arnold', 'Ombros', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z');

-- Bíceps
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0013-curl', 'Rosca Direta', 'Bíceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0029-alternating-dumbbell-curl', 'Rosca Alternada com Halteres', 'Bíceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0030-hammer-curl', 'Rosca Martelo', 'Bíceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0031-scott-curl', 'Rosca Scott', 'Bíceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0032-cable-curl', 'Rosca no Cabo', 'Bíceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0033-concentration-curl', 'Rosca Concentrada', 'Bíceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z');

-- Tríceps
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0034-triceps-pushdown', 'Tríceps Pulley (Corda)', 'Tríceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0035-skull-crusher', 'Tríceps Testa com Barra', 'Tríceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0036-close-grip-bench', 'Supino Fechado', 'Tríceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0037-overhead-triceps-extension', 'Tríceps Francês', 'Tríceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0038-triceps-dip', 'Mergulho para Tríceps (Banco)', 'Tríceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0039-cable-kickback', 'Tríceps Coice no Cabo', 'Tríceps', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z');

-- Pernas
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0010-squat', 'Agachamento Livre', 'Pernas', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z'),
('exe-0040-leg-press', 'Leg Press 45°', 'Pernas', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z'),
('exe-0041-leg-extension', 'Cadeira Extensora', 'Pernas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0042-leg-curl', 'Mesa Flexora', 'Pernas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0043-lunge', 'Afundo (Passada)', 'Pernas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0044-front-squat', 'Agachamento Frontal', 'Pernas', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z'),
('exe-0045-calf-raise-standing', 'Panturrilha em Pé', 'Pernas', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z'),
('exe-0046-calf-raise-seated', 'Panturrilha Sentado', 'Pernas', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0047-hack-squat', 'Agachamento Hack', 'Pernas', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z');

-- Glúteos
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0048-hip-thrust', 'Elevação Pélvica (Hip Thrust)', 'Glúteos', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z'),
('exe-0049-glute-bridge', 'Ponte de Glúteo', 'Glúteos', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0050-cable-glute-kickback', 'Glúteo no Cabo (Coice)', 'Glúteos', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0051-sumo-deadlift', 'Levantamento Terra Sumô', 'Glúteos', 0, NULL, 5.0, '2026-07-01T08:00:00.000Z');

-- Core
INSERT INTO exercise_library (id, name, primary_muscle, is_custom, user_id, load_increment_kg, created_at) VALUES
('exe-0052-cable-crunch', 'Abdominal na Polia (Cable Crunch)', 'Core', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0053-hanging-leg-raise', 'Elevação de Pernas na Barra', 'Core', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0054-decline-sit-up', 'Abdominal Supra no Banco Declinado', 'Core', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0055-russian-twist', 'Rotação de Tronco com Peso (Russian Twist)', 'Core', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z'),
('exe-0056-weighted-crunch-machine', 'Abdominal na Máquina', 'Core', 0, NULL, 2.5, '2026-07-01T08:00:00.000Z');
