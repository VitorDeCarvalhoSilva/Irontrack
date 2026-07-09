-- 02_SCHEMA_SQLITE.md §2, tabela 1: users
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    -- Verificação de e-mail e reset de senha: sempre hash, nunca o token em texto plano
    email_verified_at TEXT,
    email_verification_token_hash TEXT,
    email_verification_expires_at TEXT,
    password_reset_token_hash TEXT,
    password_reset_expires_at TEXT,
    -- deletion_requested_at (11_POLITICA_DE_PRIVACIDADE_E_RETENCAO_DE_DADOS.md §D):
    -- NULL = conta ativa; preenchido = em período de carência de exclusão (30 dias)
    deletion_requested_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
