-- V13: Remove o contador denormalizado de interações
-- ===================================================
--
-- `interaction_count` era incrementado a cada avaliação e exposto no
-- UserResponse. Só que `GET /users/me/stats` já responde a mesma pergunta
-- contando a tabela `rating` — a fonte da verdade. Manter as duas era guardar
-- um número que só pode divergir: uma avaliação removida decrementa a contagem
-- real e não o contador, e qualquer escrita fora do RatingService o desatualiza.
--
-- Nenhuma tela do app lia o campo. Some do banco, da entidade e do DTO.

ALTER TABLE app_user DROP COLUMN IF EXISTS interaction_count;
