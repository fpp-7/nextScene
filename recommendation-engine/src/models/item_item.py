"""
NextScene — Filtragem Colaborativa Item-Item

"Quem gostou de A também gostou de B", calculado sobre a matriz de avaliações.

Por que existe
--------------
O modelo colaborativo por SVD só sabe pontuar usuários que estavam no treino, e
os usuários do aplicativo nunca estão. Na prática isso deixava as recomendações
apoiadas somente no content-based, que compara vetores TF-IDF de tags e gêneros.
Como as tags do MovieLens são esparsas, o vetor acabava dominado pelo one-hot de
gênero e o resultado era casamento de gênero: 9 de 10 sugestões saíam com a
mesma combinação exata ("Action, Crime, Drama, Thriller").

O item-item resolve isso porque a similaridade é entre **filmes**, derivada do
comportamento de 200 mil usuários. Recomendar para alguém novo exige apenas a
lista do que essa pessoa curtiu — nada de re-treino.

Custo
-----
Guardar a matriz completa seria 1 GB (16 mil² floats), mas quase tudo é ruído.
Mantendo os K vizinhos mais próximos de cada filme, o modelo cai para ~6 MB —
contra 2,3 GB do SVD, que serializa o conjunto de treino inteiro.
"""

import logging

import joblib
import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix
from sklearn.preprocessing import normalize

from src.config import MODELS_PATH, DEFAULT_TOP_N, MIN_RATINGS_FOR_RECOMMENDATION

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Vizinhos guardados por filme. 50 cobre bem o uso e mantém o modelo pequeno.
DEFAULT_NEIGHBORS = 50

# Itens processados por vez no cálculo. Limita o pico de memória: o produto de
# um bloco pela matriz inteira é denso e cresce com BLOCK * n_items.
BLOCK_SIZE = 512

# Escala 0–5 do MovieLens convertida para peso em [-1, 1]:
# like (5.0) → +1, seen (2.5) → 0, dislike (0.0) → -1.
NEUTRAL_RATING = 2.5


class ItemItemModel:
    """
    Similaridade item-item por cosseno sobre o vetor de avaliações de cada filme.

    Uso:
        model = ItemItemModel()
        model.fit(ratings_df)
        recs = model.recommend([(318, 5.0), (858, 5.0)], top_n=10)
    """

    def __init__(self, n_neighbors: int = DEFAULT_NEIGHBORS,
                 min_ratings: int = MIN_RATINGS_FOR_RECOMMENDATION):
        self.n_neighbors = n_neighbors
        self.min_ratings = min_ratings
        self.movie_ids: np.ndarray | None = None      # id de cada linha
        self.neighbors: np.ndarray | None = None      # (n_items, K) índices
        self.similarities: np.ndarray | None = None   # (n_items, K) scores
        self._index: dict[int, int] = {}              # movieId → linha

    # ─── Treino ───────────────────────────────────────────────────────────────

    def fit(self, ratings: pd.DataFrame) -> "ItemItemModel":
        """
        Calcula os K vizinhos de cada filme.

        `ratings` precisa ter as colunas userId, movieId e rating.
        """
        counts = ratings.groupby("movieId").size()
        eligible = counts[counts >= self.min_ratings].index
        subset = ratings[ratings["movieId"].isin(set(eligible))]

        logger.info(
            "Item-item: %d filmes elegíveis (>= %d avaliações) de %d, sobre %d avaliações.",
            len(eligible), self.min_ratings, len(counts), len(subset),
        )

        items = pd.Categorical(subset["movieId"])
        users = pd.Categorical(subset["userId"])
        self.movie_ids = np.asarray(items.categories, dtype=np.int32)
        self._index = {int(m): i for i, m in enumerate(self.movie_ids)}

        n_items = len(self.movie_ids)
        matrix = csr_matrix(
            (subset["rating"].astype(np.float32), (items.codes, users.codes)),
            shape=(n_items, len(users.categories)),
        )
        # Com as linhas normalizadas, o produto interno é o cosseno.
        matrix = normalize(matrix)

        k = min(self.n_neighbors, n_items - 1)
        neighbors = np.zeros((n_items, k), dtype=np.int32)
        similarities = np.zeros((n_items, k), dtype=np.float32)

        for start in range(0, n_items, BLOCK_SIZE):
            stop = min(start + BLOCK_SIZE, n_items)
            block = np.asarray((matrix[start:stop] @ matrix.T).todense())

            # Um filme é sempre o vizinho mais próximo de si mesmo.
            for row, item in enumerate(range(start, stop)):
                block[row, item] = -1.0

            top = np.argpartition(-block, k, axis=1)[:, :k]
            scores = np.take_along_axis(block, top, axis=1)
            order = np.argsort(-scores, axis=1)

            neighbors[start:stop] = np.take_along_axis(top, order, axis=1)
            similarities[start:stop] = np.take_along_axis(scores, order, axis=1)

        self.neighbors = neighbors
        self.similarities = similarities
        logger.info("Item-item treinado: %d filmes × %d vizinhos.", n_items, k)
        return self

    # ─── Inferência ───────────────────────────────────────────────────────────

    def recommend(
        self,
        rated: list[tuple[int, float]],
        top_n: int = DEFAULT_TOP_N,
        exclude_ids: list[int] | None = None,
    ) -> pd.DataFrame:
        """
        Soma ponderada das similaridades dos filmes avaliados.

        Cada filme avaliado "vota" nos seus vizinhos, com peso proporcional à
        nota: curtido puxa para cima, rejeitado empurra para baixo. Filmes que
        aparecem como vizinhos de vários favoritos acumulam pontuação — é daí
        que vem a diferença em relação ao content-based, que só olha o rótulo.

        Devolve um DataFrame com movieId e score, ordenado do melhor para o pior.
        """
        if self.movie_ids is None:
            raise RuntimeError("Modelo não treinado.")
        if not rated:
            return pd.DataFrame(columns=["movieId", "score"])

        accumulated = np.zeros(len(self.movie_ids), dtype=np.float32)
        contributors = 0

        for movie_id, rating in rated:
            row = self._index.get(int(movie_id))
            if row is None:
                continue  # filme fora do índice (pouco avaliado ou desconhecido)

            weight = (rating - NEUTRAL_RATING) / NEUTRAL_RATING
            if weight == 0:
                continue  # "já assisti" não indica preferência

            np.add.at(accumulated, self.neighbors[row], self.similarities[row] * weight)
            contributors += 1

        if contributors == 0:
            return pd.DataFrame(columns=["movieId", "score"])

        excluded = set(exclude_ids or []) | {int(m) for m, _ in rated}
        for movie_id in excluded:
            row = self._index.get(int(movie_id))
            if row is not None:
                accumulated[row] = -np.inf

        # Só faz sentido sugerir o que recebeu voto positivo.
        candidates = np.flatnonzero(accumulated > 0)
        if candidates.size == 0:
            return pd.DataFrame(columns=["movieId", "score"])

        ordered = candidates[np.argsort(-accumulated[candidates])][:top_n]
        return pd.DataFrame({
            "movieId": self.movie_ids[ordered],
            "score": accumulated[ordered],
        })

    def knows(self, movie_id: int) -> bool:
        return int(movie_id) in self._index

    # ─── Persistência ─────────────────────────────────────────────────────────

    def save(self):
        joblib.dump(self, MODELS_PATH / "item_item.joblib")
        logger.info("ItemItemModel salvo.")

    @classmethod
    def load(cls) -> "ItemItemModel":
        path = MODELS_PATH / "item_item.joblib"
        if not path.exists():
            raise FileNotFoundError("ItemItemModel não encontrado. Execute o pipeline.")
        return joblib.load(path)
