"""
NextScene — API FastAPI
Expõe os endpoints de recomendação.
"""

import logging
from contextlib import asynccontextmanager
from typing import Annotated

import pandas as pd
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field

from src.config import API_PREFIX, DEFAULT_TOP_N, MAX_TOP_N
from src.models.hybrid import HybridRecommender
from src.preprocessing.cleaner import load_movies, load_ratings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ─── Estado global (carregado uma vez no startup) ──────────────────────────────
state: dict = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Carrega modelos e dados na inicialização da API."""
    logger.info("🎬 NextScene API iniciando...")
    state["hybrid"]  = HybridRecommender.load()
    state["movies"]  = load_movies()
    state["ratings"] = load_ratings()
    logger.info("✅ Modelos carregados.")
    yield
    state.clear()


app = FastAPI(
    title="NextScene API",
    description="Sistema de recomendação inteligente de filmes",
    version="0.1.0",
    lifespan=lifespan,
)


# ─── Schemas ──────────────────────────────────────────────────────────────────

class RecommendationItem(BaseModel):
    movie_id:  int
    title:     str
    genres:    str
    year:      int | None
    era:       str
    score:     float


class RecommendationResponse(BaseModel):
    user_id:  int | None
    stage:    str | None
    top_n:    int
    results:  list[RecommendationItem]


class ColdStartRequest(BaseModel):
    liked_movie_ids: list[int]
    top_n: int = DEFAULT_TOP_N


class RatedMovie(BaseModel):
    movie_id: int
    rating:   float = Field(ge=0, le=5)


class HistoryRequest(BaseModel):
    """Histórico de avaliações vindo do banco do aplicativo."""
    ratings: list[RatedMovie]
    top_n:   int = DEFAULT_TOP_N


# ─── Endpoints ────────────────────────────────────────────────────────────────

def _to_items(recs: pd.DataFrame) -> list[RecommendationItem]:
    """Converte o DataFrame de recomendações no schema de resposta."""
    return [
        RecommendationItem(
            movie_id=int(row["movieId"]),
            title=str(row["title_clean"]),
            genres=str(row["genres"]),
            year=int(row["year"]) if pd.notna(row.get("year")) else None,
            era=str(row["era"]),
            score=float(row["score"]),
        )
        for _, row in recs.iterrows()
    ]


@app.get("/")
async def root():
    return {"message": "NextScene API 🎬", "docs": "/docs"}


# NOTA: os endpoints abaixo são `def` (síncronos), não `async def`. Eles executam
# pandas/numpy/sklearn, que são CPU-bound e bloqueantes — dentro de `async def`
# rodariam no event loop e travariam o servidor inteiro a cada requisição.
# Como `def`, o FastAPI os despacha para o threadpool e mantém a API responsiva.


@app.post(f"{API_PREFIX}/recommend/history", response_model=RecommendationResponse)
def recommend_from_history(body: HistoryRequest):
    """
    Recomendações a partir do histórico de avaliações de um usuário do aplicativo.

    É este o endpoint que o backend deve usar para usuários reais: o histórico
    vem do Postgres a cada requisição, então o motor não precisa conhecer o
    usuário nem manter estado sobre ele.
    """
    top_n  = min(body.top_n, MAX_TOP_N)
    hybrid: HybridRecommender = state["hybrid"]

    try:
        recs = hybrid.recommend_for_history(
            rated=[(r.movie_id, r.rating) for r in body.ratings],
            top_n=top_n,
        )
    except Exception:
        logger.exception("Falha ao gerar recomendações a partir do histórico")
        raise HTTPException(status_code=500, detail="Erro ao gerar recomendações.")

    if recs.empty:
        raise HTTPException(status_code=404, detail="Nenhuma recomendação encontrada para este histórico.")

    stage = recs["stage"].iloc[0] if "stage" in recs.columns else None
    return RecommendationResponse(user_id=None, stage=stage, top_n=top_n, results=_to_items(recs))


@app.get(f"{API_PREFIX}/recommend/{{user_id}}", response_model=RecommendationResponse)
def recommend_for_user(
    user_id: int,
    top_n: Annotated[int, Query(ge=1, le=MAX_TOP_N)] = DEFAULT_TOP_N,
):
    """
    Recomendações para um usuário **do dataset MovieLens** (avaliação do modelo).

    Não use para usuários do aplicativo: os IDs vêm de datasets diferentes e não
    se correspondem. Para usuários reais, use `/recommend/history`.
    """
    hybrid:  HybridRecommender = state["hybrid"]
    movies:  pd.DataFrame       = state["movies"]
    ratings: pd.DataFrame       = state["ratings"]

    try:
        recs = hybrid.recommend(
            user_id=user_id,
            ratings=ratings,
            movies=movies,
            top_n=top_n,
        )
    except Exception:
        logger.exception("Falha ao gerar recomendações para o usuário %s", user_id)
        raise HTTPException(status_code=500, detail="Erro ao gerar recomendações.")

    if recs.empty:
        raise HTTPException(status_code=404, detail=f"Nenhuma recomendação encontrada para usuário {user_id}.")

    stage = recs["stage"].iloc[0] if "stage" in recs.columns else None
    return RecommendationResponse(user_id=user_id, stage=stage, top_n=top_n, results=_to_items(recs))


@app.post(f"{API_PREFIX}/recommend/cold-start", response_model=RecommendationResponse)
def recommend_cold_start(body: ColdStartRequest):
    """
    Recomendações para novos usuários (onboarding).
    Recebe uma lista de filmes favoritos e retorna sugestões similares.
    """
    top_n = min(body.top_n, MAX_TOP_N)
    hybrid: HybridRecommender = state["hybrid"]

    try:
        recs = hybrid.recommend_cold_start(
            liked_movie_ids=body.liked_movie_ids,
            top_n=top_n,
        )
    except Exception:
        logger.exception("Falha ao gerar recomendações de cold start")
        raise HTTPException(status_code=500, detail="Erro ao gerar recomendações.")

    if recs.empty:
        raise HTTPException(status_code=404, detail="Nenhuma recomendação encontrada.")

    return RecommendationResponse(user_id=None, stage="cold_start", top_n=top_n, results=_to_items(recs))


@app.get(f"{API_PREFIX}/movies/search")
def search_movies(
    q: Annotated[str, Query(min_length=2)],
    limit: Annotated[int, Query(ge=1, le=20)] = 10,
):
    """Busca filmes por título (para o onboarding/autocomplete)."""
    movies: pd.DataFrame = state["movies"]
    # regex=False: a query vem do usuário e não deve ser interpretada como regex.
    mask    = movies["title_clean"].str.contains(q, case=False, na=False, regex=False)
    results = movies[mask].head(limit)

    return [
        {"movie_id": int(r["movieId"]), "title": r["title_clean"], "year": r.get("year"), "genres": r["genres"]}
        for _, r in results.iterrows()
    ]
