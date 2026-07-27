"""
NextScene — Modelo Content-Based
Recomenda filmes com base na similaridade de features (tags, gêneros, era).
"""

import logging
import joblib
import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity

from src.config import MODELS_PATH, DEFAULT_TOP_N
from src.preprocessing.feature_engineering import FeatureBuilder

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Teto do cache de similaridade por filme (ver recommend_by_movie).
SIMILARITY_CACHE_SIZE = 512

# Sentinela de score para itens que não podem ser recomendados.
# A similaridade de cosseno sobre TF-IDF não-negativo nunca é negativa, então
# -1 identifica com segurança o que foi excluído.
EXCLUDED = -1.0


class ContentBasedModel:
    """
    Modelo de filtragem por conteúdo baseado em similaridade de cosseno.

    Uso:
        model = ContentBasedModel()
        model.fit(movies_df, feature_builder)
        recs = model.recommend_by_movie(movie_id=1, top_n=10)
        recs = model.recommend_for_user(liked_movie_ids=[1, 2, 3], top_n=10)
    """

    def __init__(self):
        self.feature_builder: FeatureBuilder | None = None
        self.movies: pd.DataFrame | None = None
        self._similarity_cache: dict = {}
        self._eligible_mask: np.ndarray | None = None

    # ─── Piso de popularidade ─────────────────────────────────────────────────

    def set_eligible_movies(self, movie_ids) -> None:
        """
        Restringe as recomendações aos filmes informados.

        Aplicado como máscara booleana sobre o vetor de scores — O(n) e sem
        percorrer listas de exclusão, que seriam dezenas de milhares de ids.

        Passar None remove a restrição.
        """
        if movie_ids is None:
            self._eligible_mask = None
            return

        catalog_ids = np.asarray(self.feature_builder.movie_ids)
        eligible = np.fromiter(movie_ids, dtype=catalog_ids.dtype)
        self._eligible_mask = np.isin(catalog_ids, eligible)
        logger.info(
            "Piso de popularidade: %d de %d filmes elegíveis.",
            int(self._eligible_mask.sum()), len(catalog_ids),
        )

    def _apply_eligibility(self, scores: np.ndarray) -> np.ndarray:
        """Marca com o sentinela os filmes fora do conjunto elegível."""
        # getattr: modelos serializados antes deste campo não o possuem.
        mask = getattr(self, "_eligible_mask", None)
        if mask is None:
            return scores
        return np.where(mask, scores, EXCLUDED)

    @staticmethod
    def _top_indices(scores: np.ndarray, top_n: int) -> np.ndarray:
        """
        Índices dos maiores scores, **descartando** os marcados como excluídos.

        Antes o corte era `np.argsort(scores)[::-1][:top_n]` direto: como filmes
        excluídos apenas recebiam score -1 em vez de sair da lista, eles voltavam
        ao resultado sempre que não houvesse candidatos válidos suficientes. Com
        um catálogo grande isso passava despercebido, mas o filme que o usuário
        acabou de avaliar podia reaparecer como recomendação.
        """
        candidates = np.flatnonzero(scores > EXCLUDED)
        if candidates.size == 0:
            return candidates
        ordered = candidates[np.argsort(scores[candidates])[::-1]]
        return ordered[:top_n]

    # ─── Treino ───────────────────────────────────────────────────────────────

    def fit(self, movies: pd.DataFrame, feature_builder: FeatureBuilder) -> "ContentBasedModel":
        """Associa o modelo ao feature builder e ao DataFrame de filmes."""
        self.feature_builder = feature_builder
        self.movies = movies[["movieId", "title_clean", "genres", "year", "era"]].copy()
        logger.info("ContentBasedModel pronto.")
        return self

    # ─── Inferência ───────────────────────────────────────────────────────────

    def recommend_by_movie(
        self,
        movie_id: int,
        top_n: int = DEFAULT_TOP_N,
        exclude_ids: list[int] | None = None,
    ) -> pd.DataFrame:
        """
        Retorna os top_n filmes mais similares a um filme específico.
        """
        idx = self.feature_builder.get_movie_index(movie_id)

        # Usa cache para evitar recomputar similaridades repetidas.
        # Limitado: cada entrada é um vetor de tamanho n_movies e o modelo vive
        # por todo o ciclo do processo — sem teto, isso vaza memória.
        if movie_id not in self._similarity_cache:
            movie_vector = self.feature_builder.feature_matrix[idx]
            scores = cosine_similarity(movie_vector, self.feature_builder.feature_matrix).flatten()
            if len(self._similarity_cache) >= SIMILARITY_CACHE_SIZE:
                self._similarity_cache.pop(next(iter(self._similarity_cache)))  # FIFO
            self._similarity_cache[movie_id] = scores

        scores = self._similarity_cache[movie_id].copy()

        # Exclui o próprio filme e ids indesejados
        exclude = set(exclude_ids or []) | {movie_id}
        for ex_id in exclude:
            try:
                ex_idx = self.feature_builder.get_movie_index(ex_id)
                scores[ex_idx] = EXCLUDED
            except ValueError:
                pass

        top_indices   = self._top_indices(scores, top_n)
        top_movie_ids = self.feature_builder.movie_ids[top_indices]
        top_scores    = scores[top_indices]

        return self._build_result_df(top_movie_ids, top_scores)

    def recommend_for_user(
        self,
        liked_movie_ids: list[int],
        top_n: int = DEFAULT_TOP_N,
        exclude_ids: list[int] | None = None,
    ) -> pd.DataFrame:
        """
        Recomenda com base em uma lista de filmes que o usuário gostou.
        Faz a média dos vetores de feature para criar um 'perfil do usuário'.
        """
        if not liked_movie_ids:
            raise ValueError("liked_movie_ids não pode ser vazio.")

        # Constrói o perfil do usuário como média dos vetores dos filmes curtidos
        indices = [
            self.feature_builder.get_movie_index(mid)
            for mid in liked_movie_ids
            if mid in self.feature_builder.movie_ids
        ]
        if not indices:
            raise ValueError("Nenhum dos filmes fornecidos está no índice.")

        user_profile = np.asarray(self.feature_builder.feature_matrix[indices].mean(axis=0))
        scores = cosine_similarity(user_profile, self.feature_builder.feature_matrix).flatten()
        scores = self._apply_eligibility(scores)

        # Exclui filmes já assistidos
        exclude = set(exclude_ids or []) | set(liked_movie_ids)
        for ex_id in exclude:
            try:
                ex_idx = self.feature_builder.get_movie_index(ex_id)
                scores[ex_idx] = EXCLUDED
            except ValueError:
                pass

        top_indices   = self._top_indices(scores, top_n)
        top_movie_ids = self.feature_builder.movie_ids[top_indices]
        top_scores    = scores[top_indices]

        return self._build_result_df(top_movie_ids, top_scores)

    def profile_scores(
        self,
        profile_movie_ids: list[int],
        candidate_movie_ids,
    ) -> np.ndarray:
        """
        Similaridade de cada candidato ao perfil médio de `profile_movie_ids`.

        Diferente de `recommend_for_user`, não ordena nem corta: devolve o score
        de cada candidato na mesma ordem recebida. Usado para penalizar filmes
        parecidos com os que o usuário rejeitou.
        """
        indices = [
            self.feature_builder.get_movie_index(mid)
            for mid in profile_movie_ids
            if mid in self.feature_builder.movie_ids
        ]
        if not indices:
            return np.zeros(len(candidate_movie_ids))

        profile = np.asarray(self.feature_builder.feature_matrix[indices].mean(axis=0))
        sims    = cosine_similarity(profile, self.feature_builder.feature_matrix).flatten()

        out = []
        for mid in candidate_movie_ids:
            try:
                out.append(float(sims[self.feature_builder.get_movie_index(mid)]))
            except (ValueError, KeyError, IndexError):
                out.append(0.0)
        return np.asarray(out)

    # ─── Persistência ─────────────────────────────────────────────────────────

    def save(self):
        joblib.dump(self, MODELS_PATH / "content_based.joblib")
        logger.info("ContentBasedModel salvo.")

    @classmethod
    def load(cls) -> "ContentBasedModel":
        path = MODELS_PATH / "content_based.joblib"
        if not path.exists():
            raise FileNotFoundError("ContentBasedModel não encontrado. Execute o pipeline.")
        return joblib.load(path)

    # ─── Helpers ──────────────────────────────────────────────────────────────

    def _build_result_df(self, movie_ids: np.ndarray, scores: np.ndarray) -> pd.DataFrame:
        """Monta o DataFrame de resultado com metadados dos filmes."""
        result = pd.DataFrame({"movieId": movie_ids, "score": scores})
        result = result.merge(self.movies, on="movieId", how="left")
        return result[["movieId", "title_clean", "genres", "year", "era", "score"]].reset_index(drop=True)
