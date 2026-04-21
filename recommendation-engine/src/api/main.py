"""
NextScene — API FastAPI
Expõe os endpoints de recomendação.
"""

from contextlib import asynccontextmanager
from typing import Annotated

import pandas as pd
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel

from src.config import API_PREFIX, DEFAULT_TOP_N, MAX_TOP_N
from src.models.hybrid import HybridRecommender
from src.preprocessing.cleaner import load_movies, load_ratings

# ─── Estado global (carregado uma vez no startup) ──────────────────────────────
state: dict = {}


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Carrega modelos e dados na inicialização da API."""
    print("🎬 NextScene API iniciando...")
    state["hybrid"]  = HybridRecommender.load()
    state["movies"]  = load_movies()
    state["ratings"] = load_ratings()
    print("✅ Modelos carregados.")
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


# ─── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/")
async def root():
    return {"message": "NextScene API 🎬", "docs": "/docs"}


@app.get(f"{API_PREFIX}/recommend/{{user_id}}", response_model=RecommendationResponse)
async def recommend_for_user(
    user_id: int,
    top_n: Annotated[int, Query(ge=1, le=MAX_TOP_N)] = DEFAULT_TOP_N,
):
    """
    Gera recomendações personalizadas para um usuário existente.
    Os pesos CF/CB são ajustados automaticamente pelo histórico do usuário.
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
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    if recs.empty:
        raise HTTPException(status_code=404, detail=f"Nenhuma recomendação encontrada para usuário {user_id}.")

    stage = recs["stage"].iloc[0] if "stage" in recs.columns else None

    results = [
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

    return RecommendationResponse(user_id=user_id, stage=stage, top_n=top_n, results=results)


@app.post(f"{API_PREFIX}/recommend/cold-start", response_model=RecommendationResponse)
async def recommend_cold_start(body: ColdStartRequest):
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
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    if recs.empty:
        raise HTTPException(status_code=404, detail="Nenhuma recomendação encontrada.")

    results = [
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

    return RecommendationResponse(user_id=None, stage="cold_start", top_n=top_n, results=results)


@app.get(f"{API_PREFIX}/movies/search")
async def search_movies(
    q: Annotated[str, Query(min_length=2)],
    limit: Annotated[int, Query(ge=1, le=20)] = 10,
):
    """Busca filmes por título (para o onboarding/autocomplete)."""
    movies: pd.DataFrame = state["movies"]
    mask    = movies["title_clean"].str.contains(q, case=False, na=False)
    results = movies[mask].head(limit)

    return [
        {"movie_id": int(r["movieId"]), "title": r["title_clean"], "year": r.get("year"), "genres": r["genres"]}
        for _, r in results.iterrows()
    ]
