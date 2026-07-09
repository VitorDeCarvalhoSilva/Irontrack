-- 02_SCHEMA_SQLITE.md §2, tabela 10: push_subscriptions (07_ROADMAP_BACKEND.md §C.5)
-- Inscrição de push nativo (Expo Push Service) do dispositivo do usuário +
-- preferências de lembrete. Modelo mais simples que Web Push: um único
-- Expo Push Token por dispositivo, sem chaves de assinatura (p256dh/auth).
CREATE TABLE push_subscriptions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    expo_push_token TEXT NOT NULL,
    reminder_days TEXT,      -- ex: "MON,WED,FRI" — dias da semana com lembrete
    reminder_time TEXT,      -- ex: "18:30" (HH:MM, horário local do usuário)
    enabled INTEGER NOT NULL DEFAULT 1 CHECK(enabled IN (0, 1)),
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, expo_push_token)
);
