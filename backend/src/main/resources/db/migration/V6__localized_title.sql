-- V6: Título localizado em português
-- ===================================
--
-- O catálogo vem do MovieLens, que traz apenas títulos em inglês
-- ("Shawshank Redemption, The"). O job de enriquecimento já consulta o TMDB com
-- language=pt-BR e recebe o título traduzido no mesmo payload que a sinopse e o
-- pôster — mas até agora descartava esse campo.
--
-- O título original **não** é sobrescrito: o motor de recomendação trabalha com
-- ele, e quem busca por "The Godfather" deve continuar encontrando o filme.
-- A interface passa a exibir title_pt quando existir.

ALTER TABLE movie ADD COLUMN IF NOT EXISTS title_pt VARCHAR(500);

-- A busca passa a considerar as duas colunas (ver MovieRepository.searchByTitle).
CREATE INDEX IF NOT EXISTS idx_movie_title_pt_trgm ON movie USING GIN (title_pt gin_trgm_ops);

-- Filmes já enriquecidos foram processados antes de title_pt existir e ficariam
-- sem tradução para sempre, porque o job só busca os que têm enriched_at nulo.
-- Zerar a marca os devolve à fila; o custo é repetir as chamadas ao TMDB dos que
-- já passaram, o que é pequeno perto do catálogo inteiro.
UPDATE movie SET enriched_at = NULL WHERE enriched_at IS NOT NULL;
