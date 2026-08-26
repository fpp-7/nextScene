-- V12: Filmes exibíveis e gêneros excluídos
-- ==========================================

-- ─── 1. Filmes exibíveis ──────────────────────────────────────────────────
--
-- O catálogo vem do MovieLens, que é internacional: há filmes sem tradução
-- pt-BR e sem elenco cadastrado no TMDB. Eles chegavam às prateleiras como
-- um card de título em cirílico, sinopse em branco e avatares vazios.
--
-- A decisão é do enriquecimento, não da consulta: o TMDB é a única fonte que
-- sabe se existe tradução (uma consulta com language=pt-BR devolve `overview`
-- vazio quando não existe) e se há elenco. Guardar o veredito numa coluna
-- evita repetir esse raciocínio em cada query de leitura.
--
-- DEFAULT TRUE porque a coluna significa "sabidamente inexibível", não
-- "aprovado": filme ainda não enriquecido tem status desconhecido e continua
-- aparecendo, como aparecia antes desta migration. Um banco recém-criado
-- ficaria com o catálogo vazio até o job alcançá-lo, o que é pior.
ALTER TABLE movie ADD COLUMN IF NOT EXISTS displayable BOOLEAN NOT NULL DEFAULT TRUE;

-- Backfill: aplica a mesma regra do job aos filmes que ele já processou.
-- Não zeramos enriched_at — o veredito é derivável do que já está no banco,
-- e devolver o catálogo inteiro à fila custaria milhares de chamadas ao TMDB
-- para reconstruir uma informação que já temos.
UPDATE movie
   SET displayable = FALSE
 WHERE enriched_at IS NOT NULL
   AND (synopsis IS NULL OR synopsis = ''
     OR cast_list IS NULL OR cast_list = '');

-- Toda leitura do catálogo passa a filtrar por esta coluna, quase sempre em
-- conjunto com uma ordenação por rating ou vote_count.
CREATE INDEX IF NOT EXISTS idx_movie_displayable_rating
    ON movie (displayable, rating DESC NULLS LAST);

-- ─── 2. Gêneros excluídos ─────────────────────────────────────────────────
--
-- O onboarding sempre ofereceu "excluir gênero" e o payload sempre trouxe o
-- campo `disliked` — que o backend descartava em silêncio. Quem excluía
-- Terror continuava recebendo Terror.
--
-- Tabela própria, espelhando app_user_genres_preference: são duas listas
-- independentes (não gostar de Terror não é o complemento de gostar de
-- Comédia), e um usuário pode não ter nenhuma das duas.
CREATE TABLE IF NOT EXISTS app_user_genres_excluded (
    app_user_id    UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    genres_excluded VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_app_user_genres_excluded_user
    ON app_user_genres_excluded (app_user_id);
