"""
NextScene — Exporta os ratings do app para o formato do MovieLens.

Por que existe
---------------
O motor treina só sobre o MovieLens; o que os usuários do app avaliam nunca
volta para o modelo. Este script fecha essa lacuna
do lado dos dados: lê `rating` no Postgres do backend e escreve um CSV no
mesmo formato de `ratings.csv` (userId,movieId,rating,timestamp), pronto para
`train_pipeline.py --app-ratings` concatenar com o MovieLens.

Espaço de ids
-------------
Os `userId` do MovieLens são inteiros de 1 a ~200 mil; os usuários do app são
UUID. A tabela `movielens_user_map` (migration V11 do backend) faz a ponte:
cada usuário do app que tiver ao menos um rating ganha um inteiro a partir de
1.000.000, atribuído por uma sequência do Postgres — nunca colide com o
MovieLens, e nunca reaproveita um valor mesmo que o usuário seja removido.

Os `movieId` já são compartilhados: os dois lados usam o id do MovieLens
(`movie.movie_id` no backend).

Uso:
    python scripts/export_app_ratings.py                    # escreve em data/exports/app_ratings.csv
    python scripts/export_app_ratings.py --out caminho.csv
"""

import argparse
import csv
import logging
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("export_app_ratings")

DEFAULT_OUTPUT = ROOT / "data" / "exports" / "app_ratings.csv"

# Cada usuário do app com ao menos um rating e que ainda não tem entrada em
# movielens_user_map ganha uma agora. O DEFAULT da coluna (nextval da
# sequência) cuida de atribuir o inteiro — não é escolhido aqui.
ASSIGN_MISSING_MAPPINGS_SQL = """
    INSERT INTO movielens_user_map (app_user_id)
    SELECT DISTINCT r.user_id
    FROM rating r
    LEFT JOIN movielens_user_map m ON m.app_user_id = r.user_id
    WHERE m.app_user_id IS NULL
    ON CONFLICT (app_user_id) DO NOTHING
"""

# movie.movie_id pode ser NULL para filmes que a importação não conseguiu
# ligar a um id do MovieLens (não deveria acontecer hoje, mas um rating sobre
# um filme assim não tem para onde ir no treino, então é descartado aqui, não
# silenciosamente adiante no pipeline).
EXPORT_RATINGS_SQL = """
    SELECT
        m.ml_user_id  AS "userId",
        mv.movie_id   AS "movieId",
        r.score       AS "rating",
        EXTRACT(EPOCH FROM r.created_at)::BIGINT AS "timestamp"
    FROM rating r
    JOIN movielens_user_map m ON m.app_user_id = r.user_id
    JOIN movie mv ON mv.id = r.movie_id
    WHERE mv.movie_id IS NOT NULL
    ORDER BY m.ml_user_id, mv.movie_id
"""


def export_ratings(output_path: Path) -> int:
    import psycopg

    from src.config import DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, MOVIELENS_USER_ID_OFFSET

    output_path.parent.mkdir(parents=True, exist_ok=True)

    conninfo = f"host={DB_HOST} port={DB_PORT} dbname={DB_NAME} user={DB_USER} password={DB_PASSWORD}"
    logger.info(f"Conectando em {DB_HOST}:{DB_PORT}/{DB_NAME}...")

    with psycopg.connect(conninfo) as conn:
        with conn.cursor() as cur:
            cur.execute(ASSIGN_MISSING_MAPPINGS_SQL)
            new_mappings = cur.rowcount
            conn.commit()
            if new_mappings > 0:
                logger.info(f"{new_mappings} usuário(s) novo(s) ganharam ml_user_id.")

        with conn.cursor() as cur:
            cur.execute(EXPORT_RATINGS_SQL)
            rows = cur.fetchall()

    for row in rows:
        user_id = row[0]
        if user_id < MOVIELENS_USER_ID_OFFSET:
            raise RuntimeError(
                f"ml_user_id {user_id} abaixo do offset ({MOVIELENS_USER_ID_OFFSET}) — "
                "a sequência do Postgres não está configurada como a migration V11 espera."
            )

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["userId", "movieId", "rating", "timestamp"])
        writer.writerows(rows)

    logger.info(f"{len(rows)} ratings do app exportados para {output_path}.")
    return len(rows)


if __name__ == "__main__":
    from src.utils.console import enable_utf8_stdout
    enable_utf8_stdout()

    parser = argparse.ArgumentParser(description="Exporta os ratings do app para o formato MovieLens")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUTPUT,
                         help=f"Caminho do CSV de saída (padrão: {DEFAULT_OUTPUT})")
    args = parser.parse_args()

    count = export_ratings(args.out)
    if count == 0:
        logger.warning("Nenhum rating do app encontrado — nada para o re-treino incorporar ainda.")
