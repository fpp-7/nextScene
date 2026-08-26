"""
Testes unitários — Pré-processamento
"""

import numpy as np
import pytest
import pandas as pd
from src.config import RATINGS_FILE
from src.preprocessing.cleaner import _extract_year, _clean_title, _assign_era, load_app_ratings, load_ratings


def test_extract_year_normal():
    assert _extract_year("Toy Story (1995)") == 1995


def test_extract_year_no_year():
    assert _extract_year("Untitled Film") is None


def test_extract_year_edge():
    assert _extract_year("Movie (2023)") == 2023


def test_clean_title():
    assert _clean_title("Toy Story (1995)") == "Toy Story"
    assert _clean_title("The Dark Knight (2008)") == "The Dark Knight"


def test_assign_era():
    assert _assign_era(1940) == "classical"
    assert _assign_era(1970) == "new_hollywood"
    assert _assign_era(1990) == "blockbuster"
    assert _assign_era(2010) == "contemporary"
    assert _assign_era(2020) == "recent"
    assert _assign_era(None) == "unknown"


# ─── load_app_ratings ──────────────────────────────────────────────────────────
# Regressão: o CSV exportado por
# scripts/export_app_ratings.py precisa entrar no treino no mesmo shape que
# load_ratings() devolve, para train_pipeline concatenar os dois sem fricção.


def test_load_app_ratings_matches_load_ratings_shape(tmp_path):
    csv_path = tmp_path / "app_ratings.csv"
    csv_path.write_text(
        "userId,movieId,rating,timestamp\n"
        "1000000,318,5.0,1700000000\n"
        "1000001,858,0.0,1700000100\n",
        encoding="utf-8",
    )

    app_df = load_app_ratings(csv_path)

    assert list(app_df.columns) == ["userId", "movieId", "rating", "timestamp"]
    assert app_df["userId"].dtype == np.int32
    assert app_df["movieId"].dtype == np.int32
    assert app_df["rating"].dtype == np.float32
    assert pd.api.types.is_datetime64_any_dtype(app_df["timestamp"])
    assert len(app_df) == 2


def test_load_app_ratings_ids_stay_above_the_movielens_offset(tmp_path):
    """
    Os userId do app precisam ficar fora da faixa que o MovieLens usa —
    é o que impede um usuário do app de ser confundido com um do dataset.
    """
    csv_path = tmp_path / "app_ratings.csv"
    csv_path.write_text(
        "userId,movieId,rating,timestamp\n"
        "1000000,318,5.0,1700000000\n",
        encoding="utf-8",
    )

    app_df = load_app_ratings(csv_path)

    assert app_df["userId"].min() >= 1_000_000


@pytest.mark.skipif(
    not RATINGS_FILE.exists(),
    reason="Requer o MovieLens em disco; data/ é gitignored e o CI não baixa o dataset.",
)
def test_load_app_ratings_can_be_concatenated_with_movielens_ratings(tmp_path):
    """
    O caminho que train_pipeline usa de verdade: concatenar os dois
    DataFrames precisa produzir uma única tabela consistente, sem colisão de
    userId entre o MovieLens (real, carregado do dataset small de teste) e o
    CSV do app.
    """
    csv_path = tmp_path / "app_ratings.csv"
    csv_path.write_text(
        "userId,movieId,rating,timestamp\n"
        "1000000,1,4.5,1700000000\n",
        encoding="utf-8",
    )

    movielens_df = load_ratings()
    app_df = load_app_ratings(csv_path)
    combined = pd.concat([movielens_df, app_df], ignore_index=True)

    assert len(combined) == len(movielens_df) + len(app_df)
    assert set(app_df["userId"]).isdisjoint(set(movielens_df["userId"]))
