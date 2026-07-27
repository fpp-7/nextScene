-- V5: Índices de busca + controle do enriquecimento TMDB
-- =======================================================

-- ─── Busca por título ──────────────────────────────────────────────────────
-- LIKE '%termo%' não usa índice B-tree: era varredura completa do catálogo
-- (~9.7k filmes hoje, muito mais se o dataset full for adotado) a cada busca.
-- O índice GIN com trigramas atende tanto ILIKE quanto ordenação por similarity().
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_movie_title_trgm ON movie USING GIN (title gin_trgm_ops);

-- Mesmo problema no filtro por gênero, que também usa LIKE.
CREATE INDEX IF NOT EXISTS idx_movie_genres_trgm ON movie USING GIN (genres gin_trgm_ops);

-- O índice B-tree antigo em genres nunca era usado pelo LIKE '%...%'.
DROP INDEX IF EXISTS idx_movie_genres;

-- ─── Ordenação por nota ────────────────────────────────────────────────────
-- Usado pelo filme em destaque e pelos fallbacks de recomendação.
CREATE INDEX IF NOT EXISTS idx_movie_rating ON movie (rating DESC NULLS LAST);

-- ─── Enriquecimento TMDB ───────────────────────────────────────────────────
-- Marca quando o filme foi enriquecido, para o job em background saber o que
-- ainda falta e não repetir trabalho a cada execução.
ALTER TABLE movie ADD COLUMN IF NOT EXISTS enriched_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_movie_pending_enrichment
    ON movie (tmdb_id)
    WHERE enriched_at IS NULL AND tmdb_id IS NOT NULL;
