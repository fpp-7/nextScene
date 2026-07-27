"""
NextScene — Configuração Central
Lê variáveis do .env e expõe constantes usadas em todo o projeto.
"""

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

# ─── Paths ────────────────────────────────────────────────────────────────────
ROOT_DIR = Path(__file__).resolve().parent.parent

ENV = os.getenv("ENV", "development")

# Seleciona automaticamente small ou full baseado no ambiente
if ENV == "development":
    MOVIELENS_PATH = ROOT_DIR / os.getenv("MOVIELENS_SMALL_PATH", "data/raw/ml-latest-small")
else:
    MOVIELENS_PATH = ROOT_DIR / os.getenv("MOVIELENS_FULL_PATH", "data/raw/ml-latest")

# O cache Parquet é separado por dataset.
#
# Antes era um diretório único: alternar ENV entre development e production
# mudava o CSV de origem, mas load_ratings() encontrava o Parquet da execução
# anterior e o devolvia. Treinar "com o dataset completo" continuava usando as
# 100 mil avaliações do small, sem nenhum aviso.
DATA_PROCESSED_PATH = ROOT_DIR / "data" / "processed" / MOVIELENS_PATH.name
MODELS_PATH         = ROOT_DIR / "models_saved"
DATA_RAW_PATH = MOVIELENS_PATH.parent  # aponta para data/raw/

# Garante que os diretórios existam
DATA_PROCESSED_PATH.mkdir(parents=True, exist_ok=True)
MODELS_PATH.mkdir(parents=True, exist_ok=True)

# ─── Arquivos do MovieLens ─────────────────────────────────────────────────────
RATINGS_FILE = MOVIELENS_PATH / "ratings.csv"
MOVIES_FILE  = MOVIELENS_PATH / "movies.csv"
TAGS_FILE    = MOVIELENS_PATH / "tags.csv"
LINKS_FILE   = MOVIELENS_PATH / "links.csv"

# ─── TMDB ─────────────────────────────────────────────────────────────────────
TMDB_API_KEY  = os.getenv("TMDB_API_KEY", "")
TMDB_BASE_URL = os.getenv("TMDB_BASE_URL", "https://api.themoviedb.org/3")

# ─── JWT ──────────────────────────────────────────────────────────────────────
SECRET_KEY                   = os.getenv("SECRET_KEY", "change-me-in-production")
ALGORITHM                    = os.getenv("ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES  = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", 30))

# ─── Modelo Híbrido — Pesos ───────────────────────────────────────────────────
# Ajustados dinamicamente pelo número de ratings do usuário (ver hybrid.py)
HYBRID_WEIGHTS = {
    "cold_start":   {"collaborative": 0.0,  "content": 1.0},   # 0–4 ratings
    "warming_up":   {"collaborative": 0.3,  "content": 0.7},   # 5–19 ratings
    "active":       {"collaborative": 0.6,  "content": 0.4},   # 20–49 ratings
    "power_user":   {"collaborative": 0.75, "content": 0.25},  # 50+ ratings
}

# ─── Feature Engineering ──────────────────────────────────────────────────────
# Eras cinematográficas para bucketing do ano de lançamento
CINEMA_ERAS = {
    "classical":    (0,    1959),
    "new_hollywood":(1960, 1979),
    "blockbuster":  (1980, 1999),
    "contemporary": (2000, 2015),
    "recent":       (2016, 9999),
}

# ─── Piso de popularidade ─────────────────────────────────────────────────────
# Filmes com pouquíssimas avaliações não entram nas recomendações.
#
# O modelo content-based pontua por similaridade de cosseno sobre TF-IDF. Filmes
# obscuros têm vetor esparso, dominado pelo one-hot de gênero, e por isso casam
# quase perfeitamente com o perfil do usuário — enquanto filmes populares têm
# tags ricas que diluem a similaridade. Sem piso, quem avaliava Shawshank e
# Dark Knight recebia "Deadly Crossing" (5 avaliações) e "Savage Salvation" (7).
#
# No ml-latest a mediana é 5 avaliações por filme e 62% têm menos de 10, então o
# problema domina o resultado. Com 50, sobram 16.034 filmes elegíveis (19% do
# catálogo) — ainda mais que o dataset small inteiro.
MIN_RATINGS_FOR_RECOMMENDATION = int(os.getenv("MIN_RATINGS_FOR_RECOMMENDATION", 50))

# ─── API ──────────────────────────────────────────────────────────────────────
API_PREFIX      = "/api/v1"
DEFAULT_TOP_N   = 10
MAX_TOP_N       = 50
