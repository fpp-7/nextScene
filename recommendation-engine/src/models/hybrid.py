"""
NextScene — Motor de Recomendação Híbrido
Combina Content-Based + Collaborative com pesos dinâmicos por perfil do usuário.
"""

import logging
import joblib
import numpy as np
import pandas as pd

from src.config import MODELS_PATH, HYBRID_WEIGHTS, DEFAULT_TOP_N
from src.models.content_based import ContentBasedModel
from src.models.collaborative import CollaborativeModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def _get_user_stage(n_ratings: int) -> str:
    """Determina o estágio do usuário com base no número de ratings."""
    if n_ratings < 5:
        return "cold_start"
    elif n_ratings < 20:
        return "warming_up"
    elif n_ratings < 50:
        return "active"
    else:
        return "power_user"


class HybridRecommender:
    """
    Sistema de recomendação híbrido com pesos adaptativos.

    Pesos por estágio (configuráveis em config.py):
      cold_start  (0–4 ratings)   → 100% Content-Based
      warming_up  (5–19 ratings)  → 30% CF + 70% CB
      active      (20–49 ratings) → 60% CF + 40% CB
      power_user  (50+ ratings)   → 75% CF + 25% CB

    Uso:
        recommender = HybridRecommender(cb_model, cf_model)
        recs = recommender.recommend(user_id=42, ratings_df=ratings, top_n=10)

        # Cold start (novo usuário com filmes favoritos do onboarding):
        recs = recommender.recommend_cold_start(liked_movie_ids=[1,2,3], top_n=10)
    """

    def __init__(self, content_model: ContentBasedModel, collaborative_model: CollaborativeModel):
        self.cb = content_model
        self.cf = collaborative_model

    # ─── Recomendação Principal ────────────────────────────────────────────────

    def recommend(
        self,
        user_id: int,
        ratings: pd.DataFrame,
        movies: pd.DataFrame,
        top_n: int = DEFAULT_TOP_N,
    ) -> pd.DataFrame:
        """
        Gera recomendações híbridas para um usuário existente.
        Ajusta automaticamente os pesos com base no histórico do usuário.
        """
        user_ratings = ratings[ratings["userId"] == user_id]
        n_ratings    = len(user_ratings)
        stage        = _get_user_stage(n_ratings)
        weights      = HYBRID_WEIGHTS[stage]

        logger.info(f"Usuário {user_id} | {n_ratings} ratings | estágio: {stage} | pesos: {weights}")

        watched_ids = user_ratings["movieId"].tolist()
        liked_ids   = user_ratings[user_ratings["rating"] >= 3.5]["movieId"].tolist()

        # ── Content-Based score ──────────────────────────────────────────────
        cb_scores = {}
        if weights["content"] > 0 and liked_ids:
            cb_recs = self.cb.recommend_for_user(
                liked_movie_ids=liked_ids,
                top_n=top_n * 3,
                exclude_ids=watched_ids,
            )
            for _, row in cb_recs.iterrows():
                cb_scores[row["movieId"]] = row["score"]

        # ── Collaborative score ──────────────────────────────────────────────
        cf_scores = {}
        if weights["collaborative"] > 0 and user_id in self.cf.known_users:
            candidates = self.cf.get_unrated_movies(user_id, ratings)
            # Limita candidatos para performance (filmes conhecidos pelo CB também)
            if cb_scores:
                candidates = list(set(candidates) | set(cb_scores.keys()))
            cf_recs = self.cf.recommend(
                user_id=user_id,
                candidate_movie_ids=candidates,
                top_n=top_n * 3,
                exclude_ids=watched_ids,
            )
            # Normaliza predicted_rating para [0, 1]
            if not cf_recs.empty:
                max_r = cf_recs["predicted_rating"].max()
                min_r = cf_recs["predicted_rating"].min()
                rng   = max_r - min_r if max_r != min_r else 1
                for _, row in cf_recs.iterrows():
                    cf_scores[row["movieId"]] = (row["predicted_rating"] - min_r) / rng

        # ── Combina scores ───────────────────────────────────────────────────
        all_movie_ids = set(cb_scores.keys()) | set(cf_scores.keys())
        combined = []
        for movie_id in all_movie_ids:
            cb = cb_scores.get(movie_id, 0.0)
            cf = cf_scores.get(movie_id, 0.0)
            hybrid_score = weights["content"] * cb + weights["collaborative"] * cf
            combined.append({"movieId": movie_id, "score": hybrid_score, "stage": stage})

        if not combined:
            logger.warning(f"Nenhuma recomendação gerada para usuário {user_id}.")
            return pd.DataFrame()

        result = pd.DataFrame(combined)
        result = result.sort_values("score", ascending=False).head(top_n)
        result = result.merge(
            movies[["movieId", "title_clean", "genres", "year", "era"]],
            on="movieId", how="left"
        )
        return result.reset_index(drop=True)

    def recommend_cold_start(
        self,
        liked_movie_ids: list[int],
        top_n: int = DEFAULT_TOP_N,
    ) -> pd.DataFrame:
        """
        Recomendações para novos usuários (onboarding).
        Usa 100% Content-Based com base nos filmes favoritos informados.
        """
        logger.info(f"Cold start com {len(liked_movie_ids)} filmes seed.")
        return self.cb.recommend_for_user(
            liked_movie_ids=liked_movie_ids,
            top_n=top_n,
            exclude_ids=liked_movie_ids,
        )

    # ─── Persistência ─────────────────────────────────────────────────────────

    def save(self):
        joblib.dump(self, MODELS_PATH / "hybrid_recommender.joblib")
        logger.info("HybridRecommender salvo.")

    @classmethod
    def load(cls) -> "HybridRecommender":
        path = MODELS_PATH / "hybrid_recommender.joblib"
        if not path.exists():
            raise FileNotFoundError("HybridRecommender não encontrado. Execute o pipeline.")
        return joblib.load(path)
