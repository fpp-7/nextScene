"""
NextScene — Feature Engineering
Constrói a matriz de features para o modelo Content-Based.
Combina: TF-IDF das tags + gêneros (one-hot) + era cinematográfica.
"""

import logging
import joblib
import numpy as np
import pandas as pd
from scipy.sparse import hstack, csr_matrix
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import MultiLabelBinarizer

from src.config import DATA_PROCESSED_PATH, MODELS_PATH
from src.preprocessing.cleaner import load_movies, load_tags

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class FeatureBuilder:
    """
    Constrói e persiste a matriz de features content-based.

    Features combinadas:
      - TF-IDF das tags agregadas por filme       (peso: 0.45)
      - One-Hot Encoding dos gêneros              (peso: 0.35)
      - One-Hot Encoding da era cinematográfica   (peso: 0.20)
    """

    def __init__(self):
        self.tfidf        = TfidfVectorizer(max_features=500, stop_words="english")
        self.mlb_genres   = MultiLabelBinarizer()
        self.mlb_era      = MultiLabelBinarizer()
        self.feature_matrix = None
        self.movie_ids      = None

    # ─── Público ──────────────────────────────────────────────────────────────

    def build(self, movies: pd.DataFrame, tags: pd.DataFrame) -> csr_matrix:
        """
        Executa o pipeline completo de feature engineering.
        Retorna a matriz esparsa (n_filmes × n_features).
        """
        logger.info("Iniciando Feature Engineering...")

        movies = movies.copy()

        # 1. Agrega tags por filme
        tag_docs = self._aggregate_tags(movies, tags)
        movies["tag_doc"] = movies["movieId"].map(tag_docs).fillna("")

        # 2. TF-IDF nas tags
        logger.info("  → TF-IDF nas tags...")
        tfidf_matrix = self.tfidf.fit_transform(movies["tag_doc"])
        tfidf_matrix = tfidf_matrix * 0.45  # peso

        # 3. One-Hot nos gêneros
        logger.info("  → One-Hot nos gêneros...")
        genres_matrix = self.mlb_genres.fit_transform(movies["genres_list"])
        genres_matrix = csr_matrix(genres_matrix, dtype=np.float32) * 0.35

        # 4. One-Hot na era
        logger.info("  → One-Hot na era cinematográfica...")
        era_matrix = self.mlb_era.fit_transform(movies["era"].apply(lambda x: [x]))
        era_matrix = csr_matrix(era_matrix, dtype=np.float32) * 0.20

        # 5. Concatena tudo
        self.feature_matrix = hstack([tfidf_matrix, genres_matrix, era_matrix])
        self.movie_ids       = movies["movieId"].values

        logger.info(f"Feature matrix shape: {self.feature_matrix.shape}")
        return self.feature_matrix

    def save(self):
        """Persiste o FeatureBuilder treinado em disco."""
        joblib.dump(self, MODELS_PATH / "feature_builder.joblib")
        logger.info("FeatureBuilder salvo.")

    @classmethod
    def load(cls) -> "FeatureBuilder":
        """Carrega o FeatureBuilder do disco."""
        path = MODELS_PATH / "feature_builder.joblib"
        if not path.exists():
            raise FileNotFoundError("FeatureBuilder não encontrado. Execute o pipeline primeiro.")
        return joblib.load(path)

    def get_movie_index(self, movie_id: int) -> int:
        """Retorna o índice na feature matrix para um movieId."""
        idx = np.where(self.movie_ids == movie_id)[0]
        if len(idx) == 0:
            raise ValueError(f"movieId {movie_id} não encontrado na feature matrix.")
        return idx[0]

    # ─── Privado ──────────────────────────────────────────────────────────────

    @staticmethod
    def _aggregate_tags(movies: pd.DataFrame, tags: pd.DataFrame) -> dict:
        """
        Agrega todas as tags de um filme em um único documento de texto.
        Tags com mais ocorrências ficam mais representadas (repetidas).
        """
        # Conta frequência de cada tag por filme
        tag_counts = (
            tags.groupby(["movieId", "tag"])
            .size()
            .reset_index(name="count")
        )
        # Repete a tag proporcionalmente à frequência (max 5x)
        tag_counts["tag_weighted"] = tag_counts.apply(
            lambda row: " ".join([row["tag"]] * min(row["count"], 5)), axis=1
        )
        # Concatena tudo por filme
        tag_docs = (
            tag_counts.groupby("movieId")["tag_weighted"]
            .apply(" ".join)
            .to_dict()
        )
        return tag_docs


def build_and_save_features() -> FeatureBuilder:
    """
    Função de conveniência: carrega dados, constrói e salva as features.
    Use no pipeline de treinamento.
    """
    movies = load_movies()
    tags   = load_tags()

    builder = FeatureBuilder()
    builder.build(movies, tags)
    builder.save()
    return builder


if __name__ == "__main__":
    build_and_save_features()
    print("Feature Engineering concluído!")
