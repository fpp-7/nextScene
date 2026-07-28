-- V8: Popularidade real
-- ======================
--
-- A tela Descobrir ordenava por nota média e chamava isso de "Em Alta". O
-- resultado eram os melhores filmes de todos os tempos — os cinco primeiros de
-- 1994, 1972, 1974, 1993 e 1957 — sempre na mesma ordem, porque a ordenação é
-- estática. O rótulo dizia uma coisa e a tela entregava outra.
--
-- Nota média e popularidade são coisas diferentes: um documentário obscuro com
-- 60 votos pode ter média 9,0, mas ninguém está assistindo. vote_count mede
-- quantas pessoas de fato avaliaram, que é o sinal de "em alta".
--
-- O job de enriquecimento já recebia este campo do TMDB, no mesmo payload da
-- sinopse e do pôster, e o descartava.

ALTER TABLE movie ADD COLUMN IF NOT EXISTS vote_count INTEGER;

CREATE INDEX IF NOT EXISTS idx_movie_vote_count ON movie (vote_count DESC NULLS LAST);

-- Sustenta a prateleira "Mais Recentes".
CREATE INDEX IF NOT EXISTS idx_movie_year ON movie (year DESC NULLS LAST);

-- Filmes já enriquecidos foram processados antes desta coluna existir. Sem
-- zerar a marca, justamente os mais relevantes — que o job priorizou primeiro —
-- ficariam com vote_count nulo e cairiam para o fim da ordenação por
-- popularidade, invertendo o resultado.
UPDATE movie SET enriched_at = NULL WHERE enriched_at IS NOT NULL;
