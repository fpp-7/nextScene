-- V4: Remove os filmes-mock inseridos pelo V2__seed_movies.sql
-- ============================================================
-- O V2 semeou 9 filmes vindos do mock-data do frontend, com posters do Unsplash
-- e movie_id 1..9 — exatamente a faixa de IDs do catálogo MovieLens importado
-- pelo MovieImporter. Essa colisão fazia o importer apagar movie/rating/watch_list
-- inteiros a cada startup para "resolver" o conflito.
--
-- Aqui removemos apenas as linhas do seed (identificadas pelo poster do Unsplash),
-- deixando o catálogo real assumir esses IDs. As FKs de rating e watch_list são
-- ON DELETE CASCADE: avaliações que apontem para esses 9 mocks somem junto, o que
-- é o resultado desejado — elas se referem a filmes que não existem no catálogo.

DELETE FROM movie WHERE poster_url LIKE '%unsplash%';
