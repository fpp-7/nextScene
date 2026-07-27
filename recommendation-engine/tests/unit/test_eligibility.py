"""
Testes do piso de popularidade do modelo content-based.

Sem ele, o ranqueamento por similaridade de cosseno entrega a cauda longa: no
ml-latest, 62% dos filmes têm menos de 10 avaliações, e o vetor TF-IDF deles é
praticamente só o one-hot de gênero — o que casa quase perfeitamente com o
perfil do usuário e ganha dos filmes populares.
"""

import numpy as np
import pandas as pd
import pytest
from scipy.sparse import csr_matrix

from src.models.content_based import ContentBasedModel


class StubFeatureBuilder:
    """Catálogo de 5 filmes com vetores de feature controlados."""

    def __init__(self):
        self.movie_ids = np.array([1, 2, 3, 4, 5])
        # Esparsa como na produção: o FeatureBuilder real usa csr_matrix, e o
        # `.mean(axis=0)` dele devolve 2-D — com ndarray sairia 1-D e o teste
        # exercitaria um caminho que não existe.
        #
        # Filmes 4 e 5 são "obscuros": vetor idêntico ao perfil buscado, então
        # venceriam a similaridade se nada os filtrasse.
        self.feature_matrix = csr_matrix(
            np.array(
                [
                    [1.0, 0.5],
                    [0.9, 0.4],
                    [0.8, 0.3],
                    [1.0, 0.0],
                    [1.0, 0.0],
                ]
            )
        )

    def get_movie_index(self, movie_id):
        matches = np.where(self.movie_ids == movie_id)[0]
        if len(matches) == 0:
            raise ValueError(f"desconhecido: {movie_id}")
        return int(matches[0])


@pytest.fixture
def model():
    cb = ContentBasedModel()
    cb.feature_builder = StubFeatureBuilder()
    cb.movies = pd.DataFrame(
        {
            "movieId": [1, 2, 3, 4, 5],
            "title_clean": [f"Filme {i}" for i in range(1, 6)],
            "genres": ["Drama"] * 5,
            "year": [2000] * 5,
            "era": ["contemporary"] * 5,
        }
    )
    return cb


def test_sem_filtro_todos_os_filmes_concorrem(model):
    recs = model.recommend_for_user(liked_movie_ids=[1], top_n=4)

    assert set(recs["movieId"]) == {2, 3, 4, 5}


def test_filtro_remove_os_inelegiveis(model):
    model.set_eligible_movies([1, 2, 3])

    recs = model.recommend_for_user(liked_movie_ids=[1], top_n=4)

    assert set(recs["movieId"]) == {2, 3}
    assert 4 not in recs["movieId"].values
    assert 5 not in recs["movieId"].values


def test_filtro_pode_ser_removido(model):
    model.set_eligible_movies([1, 2])
    assert set(model.recommend_for_user(liked_movie_ids=[1], top_n=4)["movieId"]) == {2}

    model.set_eligible_movies(None)

    assert set(model.recommend_for_user(liked_movie_ids=[1], top_n=4)["movieId"]) == {2, 3, 4, 5}


def test_modelo_antigo_sem_o_campo_continua_funcionando(model):
    """Modelos serializados antes deste campo não devem quebrar ao carregar."""
    del model._eligible_mask

    recs = model.recommend_for_user(liked_movie_ids=[1], top_n=2)

    assert len(recs) == 2


def test_filme_ja_avaliado_nao_volta_mesmo_sendo_elegivel(model):
    model.set_eligible_movies([1, 2, 3])

    recs = model.recommend_for_user(liked_movie_ids=[2], top_n=4, exclude_ids=[3])

    assert 2 not in recs["movieId"].values
    assert 3 not in recs["movieId"].values
