"""
NextScene — Bootstrap do motor de recomendação.

Garante que o dataset e os modelos existam. Idempotente: o que já estiver no
lugar é reaproveitado, então rodar de novo é barato.

Por que existe
--------------
O `data/` (918 MB) e os `models_saved/*.joblib` (26 MB) estão no .gitignore —
com razão, binários e datasets não pertencem ao repositório. Mas o Dockerfile
copiava os dois, então a imagem só buildava na máquina de quem já os tinha em
disco. Em qualquer clone limpo (CI, outro dev, servidor) o build falhava.

Agora o build reconstrói tudo a partir da fonte: baixa o MovieLens e treina os
modelos. Sem binários versionados e sem build que só funciona num lugar.

Uso:
    python scripts/bootstrap.py            # baixa o que faltar e treina
    python scripts/bootstrap.py --force    # re-treina mesmo com modelos prontos
"""

import argparse
import io
import logging
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("bootstrap")

MOVIELENS_URL = "https://files.grouplens.org/datasets/movielens/ml-latest-small.zip"
DATASET_DIR = ROOT / "data" / "raw" / "ml-latest-small"
MODELS_DIR = ROOT / "models_saved"

# Arquivos que o train_pipeline produz. Se algum faltar, o motor não sobe:
# HybridRecommender.load() levanta FileNotFoundError no startup da API.
REQUIRED_MODELS = (
    "feature_builder.joblib",
    "content_based.joblib",
    "collaborative.joblib",
    "hybrid_recommender.joblib",
)

REQUIRED_DATASET_FILES = ("movies.csv", "ratings.csv", "tags.csv", "links.csv")


def dataset_is_present() -> bool:
    return all((DATASET_DIR / name).exists() for name in REQUIRED_DATASET_FILES)


def models_are_present() -> bool:
    return all((MODELS_DIR / name).exists() for name in REQUIRED_MODELS)


def download_dataset() -> None:
    """Baixa e extrai o MovieLens small (~1 MB)."""
    logger.info("Baixando MovieLens small de %s ...", MOVIELENS_URL)
    DATASET_DIR.parent.mkdir(parents=True, exist_ok=True)

    with urllib.request.urlopen(MOVIELENS_URL, timeout=120) as response:
        payload = response.read()

    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        archive.extractall(DATASET_DIR.parent)

    if not dataset_is_present():
        raise RuntimeError(
            f"Dataset extraído mas incompleto em {DATASET_DIR}. "
            f"Esperado: {', '.join(REQUIRED_DATASET_FILES)}"
        )
    logger.info("Dataset pronto em %s", DATASET_DIR)


def train_models() -> None:
    """
    Executa o pipeline de treino.

    `skip_tmdb=True` de propósito: o enriquecimento via TMDB precisa de chave de
    API e de rede, e o motor funciona sem ele. Um build não pode depender de um
    serviço de terceiros nem de um segredo.
    """
    import asyncio

    from src.train_pipeline import run_pipeline

    logger.info("Treinando modelos (isso leva alguns segundos)...")
    asyncio.run(run_pipeline(evaluate=False, skip_tmdb=True))


def main() -> int:
    from src.utils.console import enable_utf8_stdout
    enable_utf8_stdout()

    parser = argparse.ArgumentParser(description="Bootstrap do motor de recomendação")
    parser.add_argument("--force", action="store_true", help="Re-treina mesmo que os modelos existam")
    args = parser.parse_args()

    if dataset_is_present():
        logger.info("Dataset já presente em %s — pulando download.", DATASET_DIR)
    else:
        download_dataset()

    if models_are_present() and not args.force:
        logger.info("Modelos já presentes em %s — pulando treino.", MODELS_DIR)
        return 0

    train_models()

    if not models_are_present():
        missing = [n for n in REQUIRED_MODELS if not (MODELS_DIR / n).exists()]
        logger.error("Treino terminou mas faltam modelos: %s", ", ".join(missing))
        return 1

    logger.info("Bootstrap concluído.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
