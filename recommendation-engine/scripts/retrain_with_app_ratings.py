"""
NextScene — Re-treino incorporando os ratings do app.

Orquestra o ciclo completo:

    1. Exporta os ratings do app do Postgres (scripts/export_app_ratings.py).
    2. Treina num diretório temporário — nunca sobrescreve os artefatos em uso
       enquanto o treino roda, então o motor continua servindo normalmente.
    3. Registra RMSE/MAE em models_saved/metrics_history.jsonl, para dar para
       comparar re-treino com re-treino e perceber uma regressão.
    4. Promove os artefatos por arquivo, com os antigos guardados como backup
       — não apaga a versão anterior, então dá para reverter.

Uso:
    python scripts/retrain_with_app_ratings.py
    python scripts/retrain_with_app_ratings.py --skip-export  # usa o último CSV exportado

O motor precisa reiniciar depois para carregar os artefatos novos —
HybridRecommender.load() lê uma vez no startup e o SVD/DataFrame de avaliações
são carregados sob demanda, então recarregar em memória sem reiniciar o
processo exigiria uma API adicional que não existe hoje. Reiniciar o container
é mais simples e o custo (alguns segundos) é aceitável para um job semanal.
"""

import argparse
import asyncio
import json
import logging
import shutil
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("retrain")

def promote(tmp_models_dir: Path, real_models_dir: Path) -> None:
    """
    Move cada artefato do diretório temporário para o real, um arquivo por
    vez. `os.replace` é atômico por arquivo nos dois sistemas operacionais,
    mesmo quando o destino já existe — diferente de `os.rename`, que falha no
    Windows nesse caso. O motor nunca vê um `.joblib` pela metade.

    O artefato anterior vai para `models_saved/backup/`, sobrescrevendo o
    backup do ciclo passado — um nível de histórico é o suficiente; a métrica
    de cada ciclo já fica registrada em metrics_history.jsonl independente do
    arquivo em si.
    """
    backup_dir = real_models_dir / "backup"
    backup_dir.mkdir(exist_ok=True)

    for artifact in sorted(tmp_models_dir.glob("*.joblib")):
        real_path = real_models_dir / artifact.name
        if real_path.exists():
            shutil.move(str(real_path), str(backup_dir / artifact.name))
        artifact.replace(real_path)
        logger.info(f"  → {artifact.name} promovido.")


def append_metrics(real_models_dir: Path, metrics: dict, app_ratings_count: int, elapsed: float) -> None:
    """
    Fica em `real_models_dir`, não num caminho fixo: precisa acompanhar para
    onde os artefatos são de fato promovidos, senão o histórico registra
    métricas de um re-treino que não corresponde ao modelo em uso.
    """
    history_file = real_models_dir / "metrics_history.jsonl"
    history_file.parent.mkdir(parents=True, exist_ok=True)
    entry = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "app_ratings_count": app_ratings_count,
        "elapsed_seconds": round(elapsed, 1),
        **(metrics or {}),
    }
    with open(history_file, "a", encoding="utf-8") as f:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")
    logger.info(f"Métricas registradas em {history_file}.")


async def main(skip_export: bool, evaluate: bool) -> int:
    import src.config as config

    export_path = ROOT / "data" / "exports" / "app_ratings.csv"

    if not skip_export:
        from scripts.export_app_ratings import export_ratings
        count = export_ratings(export_path)
    else:
        if not export_path.exists():
            logger.error(f"--skip-export ativo, mas {export_path} não existe. Rode sem --skip-export uma vez.")
            return 1
        import pandas as pd
        count = len(pd.read_csv(export_path))
        logger.info(f"--skip-export ativo: reaproveitando {export_path} ({count} ratings).")

    if count == 0:
        logger.warning("Nenhum rating do app para incorporar — re-treino cancelado. "
                        "Os artefatos atuais continuam valendo.")
        return 0

    real_models_dir = config.MODELS_PATH
    tmp_models_dir = ROOT / "models_saved.tmp"
    if tmp_models_dir.exists():
        shutil.rmtree(tmp_models_dir)
    tmp_models_dir.mkdir(parents=True)

    # Treina num diretório separado — precisa ser setado ANTES de importar
    # train_pipeline e os módulos de modelo, que fixam MODELS_PATH no import.
    config.MODELS_PATH = tmp_models_dir
    from src.train_pipeline import run_pipeline

    start = time.time()
    metrics = await run_pipeline(evaluate=evaluate, skip_tmdb=True, app_ratings=export_path)
    elapsed = time.time() - start

    promote(tmp_models_dir, real_models_dir)
    append_metrics(real_models_dir, metrics, count, elapsed)
    shutil.rmtree(tmp_models_dir, ignore_errors=True)

    logger.info("✅ Re-treino concluído. Reinicie o motor para carregar os artefatos novos.")
    return 0


if __name__ == "__main__":
    from src.utils.console import enable_utf8_stdout
    enable_utf8_stdout()

    parser = argparse.ArgumentParser(description="Re-treina incorporando os ratings do app")
    parser.add_argument("--skip-export", action="store_true",
                         help="Reaproveita o último CSV exportado, sem consultar o Postgres de novo.")
    parser.add_argument("--no-evaluate", action="store_true",
                         help="Pula a cross-validation. Não recomendado: sem métrica, um re-treino "
                              "que piora o modelo passa despercebido.")
    args = parser.parse_args()

    sys.exit(asyncio.run(main(skip_export=args.skip_export, evaluate=not args.no_evaluate)))
