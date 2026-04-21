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

DATA_PROCESSED_PATH = ROOT_DIR / "data" / "processed"
MODELS_PATH         = ROOT_DIR / "models_saved"

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

# ─── API ──────────────────────────────────────────────────────────────────────
API_PREFIX      = "/api/v1"
DEFAULT_TOP_N   = 10
MAX_TOP_N       = 50
