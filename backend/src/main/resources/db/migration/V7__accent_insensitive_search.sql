-- V7: Busca insensível a acentos
-- ===============================
--
-- Com os títulos em português, "Poderoso Chefao" não encontrava "O Poderoso
-- Chefão": ILIKE compara byte a byte e não normaliza acentuação. Digitar sem
-- acento é comum, então a busca precisa tolerar as duas formas.

CREATE EXTENSION IF NOT EXISTS unaccent;

-- A função unaccent() é declarada STABLE, e o Postgres não aceita função STABLE
-- em índice. Este wrapper a expõe como IMMUTABLE, prática consagrada para este
-- caso: o resultado só mudaria se o dicionário do unaccent fosse alterado, o
-- que não acontece em operação normal. Se um dia isso mudar, os índices abaixo
-- precisam ser reconstruídos.
--
-- O dicionário é passado explicitamente e o search_path é fixado para que a
-- função não dependa do contexto de quem a chama.
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
STRICT
PARALLEL SAFE
SET search_path = public, pg_catalog
AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

-- Índices sobre a forma sem acento — são estes que a busca passa a usar.
CREATE INDEX IF NOT EXISTS idx_movie_title_unaccent_trgm
    ON movie USING GIN (immutable_unaccent(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_movie_title_pt_unaccent_trgm
    ON movie USING GIN (immutable_unaccent(COALESCE(title_pt, '')) gin_trgm_ops);

-- Os índices sobre a forma acentuada deixam de ser usados pela busca e só
-- custariam escrita na importação do catálogo.
DROP INDEX IF EXISTS idx_movie_title_trgm;
DROP INDEX IF EXISTS idx_movie_title_pt_trgm;
