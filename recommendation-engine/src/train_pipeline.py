"""
NextScene — Pipeline de Treinamento
Executa o pipeline completo: pré-processamento → features → treino → avaliação → save.

Uso:
    python -m src.train_pipeline
    python -m src.train_pipeline --evaluate   # inclui cross-validation
"""

import argparse
import logging
import time

from src.preprocessing.cleaner import load_ratings, load_movies, load_tags, dataset_summary
from src.preprocessing.feature_engineering import FeatureBuilder
from src.models.content_based import ContentBasedModel
from src.models.collaborative import CollaborativeModel
from src.models.hybrid import HybridRecommender

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


def run_pipeline(evaluate: bool = False):
    start = time.time()
    logger.info("=" * 60)
    logger.info("  NEXTSCENE — Pipeline de Treinamento")
    logger.info("=" * 60)

    # ── 1. Carrega dados ──────────────────────────────────────────────────────
    logger.info("\n[1/5] Carregando dados...")
    dataset_summary()
    ratings = load_ratings()
    movies  = load_movies()
    tags    = load_tags()

    # ── 2. Feature Engineering ────────────────────────────────────────────────
    logger.info("\n[2/5] Construindo features content-based...")
    builder = FeatureBuilder()
    builder.build(movies, tags)
    builder.save()

    # ── 3. Modelo Content-Based ───────────────────────────────────────────────
    logger.info("\n[3/5] Treinando modelo Content-Based...")
    cb_model = ContentBasedModel()
    cb_model.fit(movies, builder)
    cb_model.save()

    # ── 4. Modelo Colaborativo ────────────────────────────────────────────────
    logger.info("\n[4/5] Treinando modelo Colaborativo (SVD)...")
    cf_model = CollaborativeModel()
    cf_model.fit(ratings)

    if evaluate:
        logger.info("  Avaliando com cross-validation (pode demorar)...")
        metrics = cf_model.evaluate(ratings, cv_folds=5)
        logger.info(f"  RMSE: {metrics['rmse_mean']:.4f} ± {metrics['rmse_std']:.4f}")
        logger.info(f"  MAE : {metrics['mae_mean']:.4f} ± {metrics['mae_std']:.4f}")

    cf_model.save()

    # ── 5. Modelo Híbrido ─────────────────────────────────────────────────────
    logger.info("\n[5/5] Montando Hybrid Recommender...")
    hybrid = HybridRecommender(cb_model, cf_model)
    hybrid.save()

    elapsed = time.time() - start
    logger.info(f"\n✅ Pipeline concluído em {elapsed:.1f}s")
    logger.info("   Modelos salvos em /models_saved/")

    # ── Teste rápido ──────────────────────────────────────────────────────────
    logger.info("\n── Teste rápido ──")
    sample_user = ratings["userId"].iloc[0]
    recs = hybrid.recommend(user_id=sample_user, ratings=ratings, movies=movies, top_n=5)
    logger.info(f"Top 5 para usuário {sample_user}:")
    print(recs[["title_clean", "genres", "year", "score", "stage"]].to_string(index=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="NextScene Training Pipeline")
    parser.add_argument("--evaluate", action="store_true", help="Inclui cross-validation")
    args = parser.parse_args()
    run_pipeline(evaluate=args.evaluate)
