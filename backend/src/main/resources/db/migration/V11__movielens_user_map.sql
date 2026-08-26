-- V11: Mapa de ids para o re-treino
-- ===================================
--
-- O motor de recomendação treina sobre o MovieLens, cujos userId são inteiros
-- de 1 a ~200 mil. Os usuários do app são UUID. Para o re-treino incluir os
-- ratings do app no mesmo espaço de ids do SVD e do item-item, cada usuário
-- do app precisa de um userId inteiro que nunca colida com um do MovieLens.
--
-- A sequência começa em 1.000.000 — folga confortável acima do maior userId
-- do ml-latest (200.948) — e nunca reaproveita um valor, mesmo que a
-- correspondência de um usuário seja removida (não há caso de uso para isso
-- hoje, mas a sequência não reciclar é a garantia mais barata contra colisão).
--
-- A tabela é preenchida sob demanda pelo script de exportação
-- (recommendation-engine/scripts/export_app_ratings.py), não em cada cadastro:
-- só usuários com pelo menos um rating no momento do re-treino precisam de um
-- id — a maioria nunca chega a ter um.

CREATE SEQUENCE movielens_user_id_seq START WITH 1000000 INCREMENT BY 1;

CREATE TABLE movielens_user_map (
    app_user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    ml_user_id  BIGINT NOT NULL UNIQUE DEFAULT nextval('movielens_user_id_seq'),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
