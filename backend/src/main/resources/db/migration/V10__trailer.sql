-- V10: Trailer de verdade
-- ========================
--
-- "Assistir Trailer" abria uma busca no YouTube pelo título do filme — podia
-- trazer review, reação ou o filme errado. O TMDB já devolve os vídeos
-- oficiais, e o job de enriquecimento já conversa com ele; faltava guardar a
-- chave do vídeo.

ALTER TABLE movie ADD COLUMN IF NOT EXISTS trailer_key VARCHAR(32);

-- Filmes já enriquecidos foram processados antes desta coluna existir e nunca
-- voltariam à fila sozinhos. Diferente da V8, não zeramos o catálogo inteiro:
-- só os filmes que já aparecem nas telas (vote_count preenchido) voltam para
-- reprocessamento — a cauda longa, que ninguém vê, fica para quando o job
-- alcançá-la naturalmente.
UPDATE movie SET enriched_at = NULL WHERE enriched_at IS NOT NULL AND vote_count IS NOT NULL;
