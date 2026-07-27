"""
Testes do caminho de recomendação por histórico do aplicativo.

É o método que substituiu o envio de `interactionCount` como se fosse o id de um
usuário do MovieLens, então vale travar o comportamento dele com testes.
"""

import numpy as np
import pandas as pd
import pytest

from src.models.hybrid import (
    DISLIKE_PENALTY,
    HybridRecommender,
    _get_user_stage,
)


class StubContentBased:
    """
    Dublê do ContentBasedModel: devolve um catálogo fixo e registra com que
    argumentos foi chamado, para os testes verificarem exclusões e limites.
    """

    def __init__(self, catalog_ids=(10, 20, 30, 40), scores=None):
        self.catalog_ids = list(catalog_ids)
        self.scores = scores or {mid: 1.0 - i * 0.1 for i, mid in enumerate(catalog_ids)}
        self.last_call = {}

    def recommend_for_user(self, liked_movie_ids, top_n, exclude_ids=None):
        self.last_call = {
            "liked": list(liked_movie_ids),
            "top_n": top_n,
            "exclude": list(exclude_ids or []),
        }
        excluded = set(exclude_ids or [])
        rows = [
            {
                "movieId": mid,
                "score": self.scores[mid],
                "title_clean": f"Filme {mid}",
                "genres": "Drama",
                "year": 2000,
                "era": "contemporary",
            }
            for mid in self.catalog_ids
            if mid not in excluded
        ]
        return pd.DataFrame(rows)

    def profile_scores(self, profile_movie_ids, candidate_movie_ids):
        # Penaliza apenas o primeiro candidato, para o efeito ficar observável.
        return np.array(
            [1.0 if i == 0 else 0.0 for i in range(len(list(candidate_movie_ids)))]
        )


@pytest.fixture
def recommender():
    return HybridRecommender(content_model=StubContentBased(), collaborative_model=object())


# ─── Estágios ─────────────────────────────────────────────────────────────────


@pytest.mark.parametrize(
    "n_ratings,expected",
    [(0, "cold_start"), (4, "cold_start"), (5, "warming_up"), (19, "warming_up"),
     (20, "active"), (49, "active"), (50, "power_user"), (500, "power_user")],
)
def test_user_stage_boundaries(n_ratings, expected):
    assert _get_user_stage(n_ratings) == expected


# ─── recommend_for_history ────────────────────────────────────────────────────


def test_empty_history_returns_empty(recommender):
    assert recommender.recommend_for_history(rated=[], top_n=5).empty


def test_history_without_likes_returns_empty(recommender):
    """Só rejeições não formam perfil — quem chama decide o fallback."""
    rated = [(10, 0.0), (20, 0.0)]
    assert recommender.recommend_for_history(rated=rated, top_n=5).empty


def test_excludes_everything_already_rated(recommender):
    """Filme já avaliado não pode voltar como recomendação."""
    rated = [(10, 5.0), (20, 2.5)]

    result = recommender.recommend_for_history(rated=rated, top_n=5)

    assert set(result["movieId"]) == {30, 40}
    assert recommender.cb.last_call["exclude"] == [10, 20]


def test_only_liked_movies_build_the_profile(recommender):
    """`seen` (2.5) e `dislike` (0.0) não entram no perfil; só o que passou de 3.5."""
    rated = [(10, 5.0), (20, 2.5), (30, 0.0)]

    recommender.recommend_for_history(rated=rated, top_n=5)

    assert recommender.cb.last_call["liked"] == [10]


def test_stage_is_reported_in_the_result(recommender):
    rated = [(10, 5.0)] + [(i, 5.0) for i in range(100, 125)]  # 26 avaliações

    result = recommender.recommend_for_history(rated=rated, top_n=5)

    assert result["stage"].iloc[0] == "active"


def test_disliked_movies_penalise_similar_candidates():
    """
    O primeiro candidato tem o maior score bruto, mas o stub o marca como
    parecido com um filme rejeitado — a penalização deve derrubá-lo do topo.
    """
    stub = StubContentBased(catalog_ids=(30, 40), scores={30: 0.90, 40: 0.80})
    recommender = HybridRecommender(content_model=stub, collaborative_model=object())

    rated = [(10, 5.0), (20, 0.0)]  # curtiu 10, rejeitou 20
    result = recommender.recommend_for_history(rated=rated, top_n=2)

    scores = dict(zip(result["movieId"], result["score"]))
    assert scores[30] == pytest.approx(0.90 - DISLIKE_PENALTY * 1.0)
    assert scores[40] == pytest.approx(0.80)
    assert result.iloc[0]["movieId"] == 40  # penalizado perdeu o topo


def test_without_dislikes_scores_are_untouched():
    stub = StubContentBased(catalog_ids=(30, 40), scores={30: 0.90, 40: 0.80})
    recommender = HybridRecommender(content_model=stub, collaborative_model=object())

    result = recommender.recommend_for_history(rated=[(10, 5.0)], top_n=2)

    assert result.iloc[0]["movieId"] == 30
    assert result.iloc[0]["score"] == pytest.approx(0.90)


def test_respects_top_n():
    stub = StubContentBased(catalog_ids=(1, 2, 3, 4, 5, 6))
    recommender = HybridRecommender(content_model=stub, collaborative_model=object())

    result = recommender.recommend_for_history(rated=[(99, 5.0)], top_n=3)

    assert len(result) == 3
