-- V9: Refresh tokens
-- ===================
--
-- O access token dura 30 minutos e não havia como renová-lo: o usuário era
-- devolvido à tela de login a cada meia hora de uso.
--
-- Por que token opaco no banco, e não outro JWT
-- --------------------------------------------
-- JWT é auto-contido e, por isso, irrevogável sem uma lista de bloqueio. Um
-- refresh token precisa ser revogável — é ele que dá acesso de longa duração.
-- Guardando no banco, "sair da conta" passa a ter efeito real no servidor, e a
-- rotação permite detectar reuso.
--
-- Só o hash é armazenado: o token é aleatório e de alta entropia, então SHA-256
-- basta. Um hash lento como bcrypt custaria caro a cada renovação sem ganho
-- real, porque não há o que adivinhar por força bruta.

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

-- Sustenta a limpeza periódica dos expirados.
CREATE INDEX idx_refresh_token_expires ON refresh_token (expires_at);
