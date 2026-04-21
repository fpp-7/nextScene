"""
NextScene — Pré-processamento e Limpeza de Dados
Carrega os CSVs do MovieLens, limpa e persiste em Parquet para uso eficiente.
"""

import re
import logging
from pathlib import Path

import numpy as np
import pandas as pd

from src.config import (
    RATINGS_FILE, MOVIES_FILE, TAGS_FILE, LINKS_FILE,
    DATA_PROCESSED_PATH, CINEMA_ERAS
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ─── Loaders ──────────────────────────────────────────────────────────────────

def load_ratings(force_reload: bool = False) -> pd.DataFrame:
    """
    Carrega ratings. Na primeira vez lê o CSV e salva como Parquet.
    Nas chamadas seguintes, carrega o Parquet (muito mais rápido).
    """
    parquet_path = DATA_PROCESSED_PATH / "ratings.parquet"

    if parquet_path.exists() and not force_reload:
        logger.info("Carregando ratings do Parquet...")
        return pd.read_parquet(parquet_path)

    logger.info("Primeira carga — lendo ratings.csv...")
    df = pd.read_csv(RATINGS_FILE, dtype={
        "userId":  np.int32,
        "movieId": np.int32,
        "rating":  np.float32,
    })
    df["timestamp"] = pd.to_datetime(df["timestamp"], unit="s")
    df.to_parquet(parquet_path, index=False)
    logger.info(f"Ratings salvos em Parquet. Shape: {df.shape}")
    return df


def load_movies(force_reload: bool = False) -> pd.DataFrame:
    """
    Carrega filmes com título limpo, ano extraído e gêneros como lista.
    """
    parquet_path = DATA_PROCESSED_PATH / "movies.parquet"

    if parquet_path.exists() and not force_reload:
        logger.info("Carregando movies do Parquet...")
        df = pd.read_parquet(parquet_path)
        # Reconstrói a coluna genres_list (listas não persistem bem em Parquet)
        df["genres_list"] = df["genres"].str.split("|")
        return df

    logger.info("Primeira carga — lendo movies.csv...")
    df = pd.read_csv(MOVIES_FILE)
    df = _clean_movies(df)
    df.to_parquet(parquet_path, index=False)
    logger.info(f"Movies salvos em Parquet. Shape: {df.shape}")
    df["genres_list"] = df["genres"].str.split("|")
    return df


def load_tags(force_reload: bool = False) -> pd.DataFrame:
    """
    Carrega e normaliza as tags dos usuários.
    """
    parquet_path = DATA_PROCESSED_PATH / "tags.parquet"

    if parquet_path.exists() and not force_reload:
        logger.info("Carregando tags do Parquet...")
        return pd.read_parquet(parquet_path)

    logger.info("Primeira carga — lendo tags.csv...")
    df = pd.read_csv(TAGS_FILE, dtype={
        "userId":  np.int32,
        "movieId": np.int32,
    })
    df["tag"]       = df["tag"].str.lower().str.strip()
    df["timestamp"] = pd.to_datetime(df["timestamp"], unit="s")
    df = df.dropna(subset=["tag"])
    df.to_parquet(parquet_path, index=False)
    logger.info(f"Tags salvas em Parquet. Shape: {df.shape}")
    return df


def load_links() -> pd.DataFrame:
    """Carrega o arquivo de links (ponte para TMDB/IMDB)."""
    return pd.read_csv(LINKS_FILE, dtype={
        "movieId": np.int32,
        "imdbId":  str,
        "tmdbId":  pd.Int32Dtype(),
    })


# ─── Helpers internos ─────────────────────────────────────────────────────────

def _extract_year(title: str) -> int | None:
    """Extrai o ano do título no formato 'Movie Title (YYYY)'."""
    match = re.search(r"\((\d{4})\)\s*$", title)
    return int(match.group(1)) if match else None


def _clean_title(title: str) -> str:
    """Remove o ano e espaços extras do título."""
    return re.sub(r"\s*\(\d{4}\)\s*$", "", title).strip()


def _assign_era(year: int | None) -> str:
    """Mapeia o ano para uma era cinematográfica."""
    if year is None:
        return "unknown"
    for era, (start, end) in CINEMA_ERAS.items():
        if start <= year <= end:
            return era
    return "unknown"


def _clean_movies(df: pd.DataFrame) -> pd.DataFrame:
    """Pipeline completo de limpeza do DataFrame de filmes."""
    df = df.copy()

    # Extrai ano e limpa título
    df["year"]          = df["title"].apply(_extract_year)
    df["title_clean"]   = df["title"].apply(_clean_title)
    df["era"]           = df["year"].apply(_assign_era)

    # Remove filmes sem gênero definido
    df = df[df["genres"] != "(no genres listed)"].copy()

    # Gêneros como string separada por pipe (salva bem em Parquet)
    # A coluna genres_list é adicionada em load_movies() após a leitura

    logger.info(f"Filmes após limpeza: {len(df)} (removidos sem gênero)")
    return df


# ─── Dataset summary ──────────────────────────────────────────────────────────

def dataset_summary():
    """Imprime um resumo rápido do dataset carregado."""
    ratings = load_ratings()
    movies  = load_movies()
    tags    = load_tags()

    print("\n" + "="*50)
    print("  NEXTSCENE — Dataset Summary")
    print("="*50)
    print(f"  Ratings : {len(ratings):>10,} linhas")
    print(f"  Filmes  : {len(movies):>10,} únicos")
    print(f"  Usuários: {ratings['userId'].nunique():>10,} únicos")
    print(f"  Tags    : {len(tags):>10,} linhas")
    print(f"  Período : {ratings['timestamp'].min().year} → {ratings['timestamp'].max().year}")
    print(f"  Rating médio : {ratings['rating'].mean():.2f}")
    print(f"  Esparsidade  : {1 - len(ratings) / (ratings['userId'].nunique() * ratings['movieId'].nunique()):.4%}")
    print("="*50 + "\n")


if __name__ == "__main__":
    dataset_summary()
