"""
NextScene — Modelo Colaborativo (SVD)
Filtragem colaborativa usando Singular Value Decomposition via Surprise.
"""

import logging
import joblib
import numpy as np
import pandas as pd
from surprise import SVD, Dataset, Reader, accuracy
from surprise.model_selection import cross_validate, train_test_split

from src.config import MODELS_PATH, DEFAULT_TOP_N

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CollaborativeModel:
    """
    Modelo de filtragem colaborativa baseado em SVD.

    Uso:
        model = CollaborativeModel()
        model.fit(ratings_df)
        recs = model.recommend(user_id=42, candidate_movie_ids=[...], top_n=10)
    """

    def __init__(self, n_factors: int = 100, n_epochs: int = 20, lr_all: float = 0.005, reg_all: float = 0.02):
        self.algo = SVD(
            n_factors=n_factors,
            n_epochs=n_epochs,
            lr_all=lr_all,
            reg_all=reg_all,
            random_state=42,
        )
        self._trainset = None
        self.known_users: set  = set()
        self.known_movies: set = set()

    # ─── Treino ───────────────────────────────────────────────────────────────

    def fit(self, ratings: pd.DataFrame) -> "CollaborativeModel":
        """
        Treina o SVD com o DataFrame de ratings completo.
        ratings deve ter colunas: userId, movieId, rating
        """
        logger.info("Treinando modelo SVD colaborativo...")
        reader    = Reader(rating_scale=(ratings["rating"].min(), ratings["rating"].max()))
        dataset   = Dataset.load_from_df(ratings[["userId", "movieId", "rating"]], reader)
        trainset  = dataset.build_full_trainset()

        self.algo.fit(trainset)
        self._trainset   = trainset
        self.known_users  = set(ratings["userId"].unique())
        self.known_movies = set(ratings["movieId"].unique())

        logger.info("SVD treinado com sucesso.")
        return self

    def evaluate(self, ratings: pd.DataFrame, cv_folds: int = 5) -> dict:
        """
        Avalia o modelo com cross-validation.
        Retorna RMSE e MAE médios.
        """
        logger.info(f"Avaliando com {cv_folds}-fold cross-validation...")
        reader  = Reader(rating_scale=(ratings["rating"].min(), ratings["rating"].max()))
        dataset = Dataset.load_from_df(ratings[["userId", "movieId", "rating"]], reader)

        results = cross_validate(self.algo, dataset, measures=["RMSE", "MAE"], cv=cv_folds, verbose=True)
        # float() nativo, não numpy.float64: json.dumps (usado no histórico de
        # métricas do re-treino) não serializa tipos numpy sem um encoder custom.
        return {
            "rmse_mean": float(results["test_rmse"].mean()),
            "rmse_std":  float(results["test_rmse"].std()),
            "mae_mean":  float(results["test_mae"].mean()),
            "mae_std":   float(results["test_mae"].std()),
        }

    # ─── Inferência ───────────────────────────────────────────────────────────

    def predict_rating(self, user_id: int, movie_id: int) -> float:
        """Prediz o rating que user_id daria ao movie_id."""
        prediction = self.algo.predict(str(user_id), str(movie_id))
        return prediction.est

    def recommend(
        self,
        user_id: int,
        candidate_movie_ids: list[int],
        top_n: int = DEFAULT_TOP_N,
        exclude_ids: list[int] | None = None,
    ) -> pd.DataFrame:
        """
        Recomenda filmes para um usuário a partir de uma lista de candidatos.

        Args:
            user_id: ID do usuário
            candidate_movie_ids: filmes para avaliar (ex: todos que o usuário não assistiu)
            top_n: quantas recomendações retornar
            exclude_ids: filmes a excluir explicitamente (ex: já assistidos)

        Returns:
            DataFrame com movieId e predicted_rating ordenado por score desc
        """
        if user_id not in self.known_users:
            logger.warning(f"Usuário {user_id} desconhecido — retornando lista vazia.")
            return pd.DataFrame(columns=["movieId", "predicted_rating"])

        exclude = set(exclude_ids or [])
        predictions = []

        for movie_id in candidate_movie_ids:
            if movie_id in exclude:
                continue
            pred = self.algo.predict(str(user_id), str(movie_id))
            predictions.append({"movieId": movie_id, "predicted_rating": pred.est})

        if not predictions:
            return pd.DataFrame(columns=["movieId", "predicted_rating"])

        result = pd.DataFrame(predictions)
        result = result.sort_values("predicted_rating", ascending=False).head(top_n)
        return result.reset_index(drop=True)

    def get_unrated_movies(self, user_id: int, ratings: pd.DataFrame) -> list[int]:
        """Retorna a lista de filmes que o usuário ainda não avaliou."""
        rated = set(ratings[ratings["userId"] == user_id]["movieId"].values)
        return list(self.known_movies - rated)

    # ─── Persistência ─────────────────────────────────────────────────────────

    def save(self):
        joblib.dump(self, MODELS_PATH / "collaborative.joblib")
        logger.info("CollaborativeModel salvo.")

    @classmethod
    def load(cls) -> "CollaborativeModel":
        path = MODELS_PATH / "collaborative.joblib"
        if not path.exists():
            raise FileNotFoundError("CollaborativeModel não encontrado. Execute o pipeline.")
        return joblib.load(path)
