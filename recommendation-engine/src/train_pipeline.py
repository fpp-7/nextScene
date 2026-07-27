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
from src.preprocessing.tmdb_enricher import run_enricher, TMDB_METADATA_FILE # Import TMDB enricher and file path
from src.models.content_based import ContentBasedModel
from src.models.collaborative import CollaborativeModel
from src.models.hybrid import HybridRecommender

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)


async def run_pipeline(evaluate: bool = False, skip_tmdb: bool = False, reset_tmdb: bool = False):
    start = time.time()
    logger.info("=" * 60)
    logger.info("  NEXTSCENE — Pipeline de Treinamento")
    logger.info("=" * 60)

    # ── 1. Carrega dados ──────────────────────────────────────────────────────
    logger.info("\n[1/6] Carregando dados...")
    dataset_summary()
    ratings = load_ratings()
    movies  = load_movies()
    tags    = load_tags()

    # ── 2. Enriquecimento TMDB (opcional) ─────────────────────────────────────
    logger.info("\n[2/6] Verificando enriquecimento TMDB...")
    if skip_tmdb:
        logger.info("  → Ignorando enriquecimento TMDB (--skip-tmdb ativo).")
    elif TMDB_METADATA_FILE.exists() and not reset_tmdb:
        logger.info(f"  → {TMDB_METADATA_FILE.name} já existe. Pulando enriquecimento TMDB. Use --reset-tmdb para re-executar.")
    else:
        if reset_tmdb:
            logger.info("  → --reset-tmdb ativo. Re-executando enriquecimento TMDB.")
        else:
            logger.info("  → TMDB metadata file não encontrado. Iniciando enriquecimento TMDB.")
        await run_enricher(reset=reset_tmdb)

    # ── 3. Feature Engineering ────────────────────────────────────────────────
    logger.info("\n[3/6] Construindo features content-based...")
    builder = FeatureBuilder()
    builder.build(movies, tags)
    builder.save()

    # ── 4. Modelo Content-Based ───────────────────────────────────────────────
    logger.info("\n[4/6] Treinando modelo Content-Based...")
    cb_model = ContentBasedModel()
    cb_model.fit(movies, builder)
    cb_model.save()

    # ── 5. Modelo Colaborativo ────────────────────────────────────────────────
    logger.info("\n[5/6] Treinando modelo Colaborativo (SVD)...")
    cf_model = CollaborativeModel()
    cf_model.fit(ratings)

    if evaluate:
        logger.info("  Avaliando com cross-validation (pode demorar)...")
        metrics = cf_model.evaluate(ratings, cv_folds=5)
        logger.info(f"  RMSE: {metrics['rmse_mean']:.4f} ± {metrics['rmse_std']:.4f}")
        logger.info(f"  MAE : {metrics['mae_mean']:.4f} ± {metrics['mae_std']:.4f}")

    cf_model.save()

    # ── 6. Modelo Híbrido ─────────────────────────────────────────────────────
    logger.info("\n[6/6] Montando Hybrid Recommender...")
    hybrid = HybridRecommender(cb_model, cf_model)
    hybrid.save()

    elapsed = time.time() - start
    logger.info(f"\n✅ Pipeline concluído em {elapsed:.1f}s")
    logger.info("   Modelos salvos em /models_saved/")
    logger.info(f"   TMDB enrichment: {'ativo' if builder.tmdb_used else 'inativo'}")

    # ── Teste rápido ──────────────────────────────────────────────────────────
    logger.info("\n── Teste rápido ──")
    sample_user = ratings["userId"].iloc[2]
    recs = hybrid.recommend(user_id=sample_user, ratings=ratings, movies=movies, top_n=5)
    logger.info(f"Top 5 para usuário {sample_user}:")
    print(recs[["title_clean", "genres", "year", "score", "stage"]].to_string(index=False))


if __name__ == "__main__":
    # O pipeline imprime setas e emojis; sem isto o console cp1252 do Windows
    # derruba o processo com UnicodeEncodeError no meio do treino.
    from src.utils.console import enable_utf8_stdout
    enable_utf8_stdout()

    parser = argparse.ArgumentParser(description="NextScene Training Pipeline")
    parser.add_argument("--evaluate", action="store_true", help="Inclui cross-validation")
    parser.add_argument("--skip-tmdb", action="store_true", help="Pula a etapa de enriquecimento de metadados TMDB.")
    parser.add_argument("--reset-tmdb", action="store_true", help="Força a re-execução do enriquecimento TMDB, ignorando o checkpoint.")
    args = parser.parse_args()

    # Use asyncio.run para chamar a função async
    import asyncio
    asyncio.run(run_pipeline(evaluate=args.evaluate, skip_tmdb=args.skip_tmdb, reset_tmdb=args.reset_tmdb))
