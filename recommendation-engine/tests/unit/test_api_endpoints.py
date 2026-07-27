"""
Testes dos endpoints da API.

As funções de rota são chamadas diretamente, com `state` substituído por dublês.
Isso é possível porque elas deixaram de ser `async def` — e evita carregar os
modelos e o dataset inteiros só para testar mapeamento e tratamento de erro.
"""

import numpy as np
import pandas as pd
import pytest
from fastapi import HTTPException

from src.api import main
from src.api.main import (
    ColdStartRequest,
    HistoryRequest,
    RatedMovie,
    recommend_cold_start,
    recommend_from_history,
    search_movies,
)


def make_recs(rows):
    return pd.DataFrame(rows)


class StubHybrid:
    def __init__(self, result=None, error=None):
        self.result = result if result is not None else pd.DataFrame()
        self.error = error
        self.last_call = {}

    def recommend_for_history(self, rated, top_n):
        self.last_call = {"rated": rated, "top_n": top_n}
        if self.error:
            raise self.error
        return self.result

    def recommend_cold_start(self, liked_movie_ids, top_n):
        self.last_call = {"liked": liked_movie_ids, "top_n": top_n}
        if self.error:
            raise self.error
        return self.result


@pytest.fixture
def state(monkeypatch):
    fake = {}
    monkeypatch.setattr(main, "state", fake)
    return fake


ONE_ROW = [{
    "movieId": 42,
    "score": 0.87,
    "title_clean": "Matrix",
    "genres": "Action|Sci-Fi",
    "year": 1999,
    "era": "blockbuster",
    "stage": "active",
}]


# ─── /recommend/history ───────────────────────────────────────────────────────


def test_history_maps_results(state):
    state["hybrid"] = StubHybrid(make_recs(ONE_ROW))

    response = recommend_from_history(
        HistoryRequest(ratings=[RatedMovie(movie_id=1, rating=5.0)], top_n=10)
    )

    assert response.stage == "active"
    assert len(response.results) == 1
    item = response.results[0]
    assert (item.movie_id, item.title, item.year) == (42, "Matrix", 1999)
    assert item.score == pytest.approx(0.87)


def test_history_forwards_ratings_as_tuples(state):
    stub = StubHybrid(make_recs(ONE_ROW))
    state["hybrid"] = stub

    recommend_from_history(HistoryRequest(
        ratings=[RatedMovie(movie_id=7, rating=5.0), RatedMovie(movie_id=8, rating=0.0)],
        top_n=5,
    ))

    assert stub.last_call["rated"] == [(7, 5.0), (8, 0.0)]


def test_history_empty_result_is_404(state):
    state["hybrid"] = StubHybrid(pd.DataFrame())

    with pytest.raises(HTTPException) as exc:
        recommend_from_history(HistoryRequest(ratings=[RatedMovie(movie_id=1, rating=5.0)]))

    assert exc.value.status_code == 404


def test_history_caps_top_n_at_max(state):
    stub = StubHybrid(make_recs(ONE_ROW))
    state["hybrid"] = stub

    recommend_from_history(
        HistoryRequest(ratings=[RatedMovie(movie_id=1, rating=5.0)], top_n=9999)
    )

    assert stub.last_call["top_n"] == main.MAX_TOP_N


def test_internal_error_does_not_leak_details(state):
    """A mensagem da exceção não deve chegar ao cliente."""
    state["hybrid"] = StubHybrid(error=RuntimeError("caminho /srv/secreto/modelo.joblib"))

    with pytest.raises(HTTPException) as exc:
        recommend_from_history(HistoryRequest(ratings=[RatedMovie(movie_id=1, rating=5.0)]))

    assert exc.value.status_code == 500
    assert "secreto" not in str(exc.value.detail)


def test_rating_outside_scale_is_rejected():
    with pytest.raises(ValueError):
        RatedMovie(movie_id=1, rating=7.0)


# ─── /recommend/cold-start ────────────────────────────────────────────────────


def test_cold_start_reports_its_stage(state):
    state["hybrid"] = StubHybrid(make_recs([{**ONE_ROW[0], "stage": None}]))

    response = recommend_cold_start(ColdStartRequest(liked_movie_ids=[1, 2], top_n=5))

    assert response.stage == "cold_start"
    assert response.user_id is None


def test_year_ausente_vira_none(state):
    row = {**ONE_ROW[0], "year": np.nan}
    state["hybrid"] = StubHybrid(make_recs([row]))

    response = recommend_cold_start(ColdStartRequest(liked_movie_ids=[1]))

    assert response.results[0].year is None


# ─── /movies/search ───────────────────────────────────────────────────────────


def test_search_treats_query_as_literal_text(state):
    """
    A busca usa regex=False: sem isso, um título com parêntese ou '+' vindo do
    usuário seria interpretado como expressão regular e podia estourar erro.
    """
    state["movies"] = pd.DataFrame([
        {"movieId": 1, "title_clean": "Spider-Man (2002)", "year": 2002, "genres": "Action"},
        {"movieId": 2, "title_clean": "Matrix", "year": 1999, "genres": "Sci-Fi"},
    ])

    results = search_movies(q="(2002)", limit=10)

    assert [r["movie_id"] for r in results] == [1]


def test_search_is_case_insensitive(state):
    state["movies"] = pd.DataFrame([
        {"movieId": 2, "title_clean": "Matrix", "year": 1999, "genres": "Sci-Fi"},
    ])

    assert len(search_movies(q="mAtRiX", limit=10)) == 1
