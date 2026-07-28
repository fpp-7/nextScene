"""
Testes do modelo colaborativo item-item.

É ele que passa a atender os usuários do aplicativo. O SVD só pontua quem estava
no treino, e o content-based, com as tags esparsas do MovieLens, degenerava em
casamento de gênero — 9 de 10 sugestões saíam com a mesma combinação exata.
"""

import numpy as np
import pandas as pd
import pytest

from src.models.item_item import ItemItemModel


def ratings_df(rows):
    return pd.DataFrame(rows, columns=["userId", "movieId", "rating"])


@pytest.fixture
def model():
    """
    Dois grupos de gosto que não se cruzam.

    Usuários 1–4 avaliam bem 10, 11 e 12; usuários 5–8 avaliam bem 20, 21 e 22.
    Nenhum sinal de gênero está envolvido — só comportamento.
    """
    rows = []
    for user in range(1, 5):
        for movie in (10, 11, 12):
            rows.append((user, movie, 5.0))
    for user in range(5, 9):
        for movie in (20, 21, 22):
            rows.append((user, movie, 5.0))

    return ItemItemModel(n_neighbors=3, min_ratings=2).fit(ratings_df(rows))


def test_recomenda_do_mesmo_grupo_de_gosto(model):
    """Quem curtiu 10 deve receber 11 e 12, não os do outro grupo."""
    recs = model.recommend(rated=[(10, 5.0)], top_n=5)

    assert set(recs["movieId"]) == {11, 12}


def test_nao_recomenda_o_que_ja_foi_avaliado(model):
    recs = model.recommend(rated=[(10, 5.0), (11, 5.0)], top_n=5)

    assert 10 not in recs["movieId"].values
    assert 11 not in recs["movieId"].values
    assert set(recs["movieId"]) == {12}


def test_respeita_exclude_ids(model):
    recs = model.recommend(rated=[(10, 5.0)], top_n=5, exclude_ids=[12])

    assert set(recs["movieId"]) == {11}


def test_varios_favoritos_acumulam_pontuacao(model):
    """Filme vizinho de dois favoritos pontua mais que vizinho de um só."""
    um = model.recommend(rated=[(10, 5.0)], top_n=5)
    dois = model.recommend(rated=[(10, 5.0), (11, 5.0)], top_n=5)

    score_um = float(um[um.movieId == 12]["score"].iloc[0])
    score_dois = float(dois[dois.movieId == 12]["score"].iloc[0])
    assert score_dois > score_um


def test_rejeicao_empurra_para_baixo(model):
    """Rejeitar um filme derruba os parecidos com ele."""
    recs = model.recommend(rated=[(20, 0.0), (10, 5.0)], top_n=10)

    # Os vizinhos do rejeitado não podem aparecer com pontuação positiva.
    assert 21 not in recs["movieId"].values
    assert 22 not in recs["movieId"].values


def test_ja_assisti_e_neutro(model):
    """`seen` (2.5) não expressa preferência e não deve gerar recomendação."""
    recs = model.recommend(rated=[(10, 2.5)], top_n=5)

    assert recs.empty


def test_historico_vazio_devolve_vazio(model):
    assert model.recommend(rated=[], top_n=5).empty


def test_filme_desconhecido_e_ignorado(model):
    """Filme fora do índice não quebra; quem chamou cai no fallback."""
    assert model.recommend(rated=[(99999, 5.0)], top_n=5).empty


def test_filtra_filmes_pouco_avaliados():
    """O piso de popularidade tira a cauda longa do índice."""
    rows = [(u, 10, 5.0) for u in range(1, 6)]      # 5 avaliações
    rows += [(u, 11, 5.0) for u in range(1, 6)]
    rows += [(1, 99, 5.0)]                           # 1 avaliação

    m = ItemItemModel(n_neighbors=2, min_ratings=5).fit(ratings_df(rows))

    assert m.knows(10)
    assert not m.knows(99)


def test_resultado_vem_ordenado(model):
    recs = model.recommend(rated=[(10, 5.0)], top_n=5)

    scores = recs["score"].tolist()
    assert scores == sorted(scores, reverse=True)


def test_nao_guarda_a_matriz_completa(model):
    """
    O modelo mantém apenas K vizinhos por filme. Guardar a matriz densa seria
    1 GB no catálogo real; assim são poucos megabytes.
    """
    n_items = len(model.movie_ids)

    assert model.neighbors.shape == (n_items, min(3, n_items - 1))
    assert model.similarities.shape == model.neighbors.shape
