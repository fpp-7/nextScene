# NextScene — Motor de Recomendação

Serviço FastAPI que responde "o que essa pessoa provavelmente vai gostar", a
partir do comportamento de 200 mil usuários do MovieLens.

É **stateless**: não conhece os usuários do aplicativo e não guarda estado sobre
eles. O backend envia o histórico de avaliações a cada requisição, o motor
pontua e devolve. Ninguém precisa ser re-treinado quando alguém novo se cadastra.

Para subir o projeto inteiro, veja o [README da raiz](../README.md). Para a
arquitetura das três partes, [docs/project_overview.md](../docs/project_overview.md).

---

## Stack

- **ML:** scikit-learn, scikit-surprise (SVD), pandas, NumPy, SciPy
- **API:** FastAPI + Uvicorn
- **Dados:** MovieLens — `ml-latest-small` em desenvolvimento, `ml-latest` em produção
- **Metadados:** TMDB (opcional, no pipeline de treino)

---

## Os modelos

```
Histórico do usuário  (movieId, nota)
          │
          ▼
   HybridRecommender
          │
          ├── ItemItemModel      ← principal
          │   "quem gostou de A também gostou de B"
          │
          └── ContentBasedModel  ← fallback
              TF-IDF de tags + gêneros + era
```

### Item-item é o principal

A similaridade é calculada entre **filmes**, a partir do comportamento — não de
rótulo de gênero. Cada filme avaliado vota nos seus vizinhos, com peso pela
nota: curtido puxa para cima, rejeitado empurra para baixo, "já assisti" é
neutro. Quem aparece como vizinho de vários favoritos acumula pontuação.

Como a similaridade não depende de quem está pedindo, recomendar para alguém que
nunca esteve no treino exige apenas a lista do que essa pessoa avaliou.

**Custo:** a matriz completa seria 16 mil² floats, ~1 GB, e quase tudo é ruído
que nunca seria consultado. Guardando os 50 vizinhos mais próximos de cada filme
(`DEFAULT_NEIGHBORS` em `src/models/item_item.py`), o modelo cai para ~6 MB.

### Content-based é o fallback

Entra quando o item-item não tem o que dizer — histórico curto demais, ou filmes
avaliados que ficaram fora do índice por serem pouco avaliados no dataset.

Não é o principal porque as tags do MovieLens são esparsas: o vetor acaba
dominado pelo one-hot de gênero e o modelo degenera em casamento de rótulo. Com
o mesmo histórico de entrada, o item-item devolve 8–9 combinações de gênero
distintas em 10 sugestões; o content-based, 2.

### SVD: só para avaliar o modelo

O colaborativo por SVD (`src/models/collaborative.py`) **não atende os usuários
do aplicativo** — ele só pontua quem estava no conjunto de treino, e ninguém do
app está. Permanece atrás de `GET /api/v1/recommend/{user_id}`, útil para medir
o modelo contra usuários do MovieLens.

O `scikit-surprise` serializa o conjunto de treino inteiro dentro do pickle, e
carregá-lo custa ~10 GB de RAM. Por isso ele e o DataFrame de avaliações são
**carregados sob demanda**, na primeira chamada àquele endpoint:

| | Antes | Depois |
|---|---|---|
| Startup | 136 s | 2,3 s |
| RAM em operação | 11,7 GB | 275 MB |

Os pesos por estágio de usuário (`HYBRID_WEIGHTS` em `src/config.py`:
`cold_start` → `power_user`) valem apenas para esse caminho de avaliação, onde
CF e CB são de fato combinados. O caminho do aplicativo usa item-item com
content-based como fallback, não uma média ponderada dos dois.

### Piso de popularidade

Filmes com menos de 50 avaliações não entram nas sugestões. Sem esse piso, a
cauda longa domina: no `ml-latest` a mediana é 5 avaliações por filme, e esses
casam quase perfeitamente com qualquer perfil porque seu vetor é praticamente só
o gênero. Configurável por `MIN_RATINGS_FOR_RECOMMENDATION`.

---

## Estrutura

```
recommendation-engine/
├── scripts/
│   └── bootstrap.py        ← baixa o MovieLens e treina; roda no build da imagem
├── src/
│   ├── config.py           ← configuração central (paths, pesos, limiares)
│   ├── preprocessing/
│   │   ├── cleaner.py              ← limpeza e carga
│   │   ├── feature_engineering.py  ← TF-IDF, one-hot de gênero, eras
│   │   └── tmdb_enricher.py        ← metadados TMDB no pipeline de treino
│   ├── models/
│   │   ├── item_item.py    ← colaborativo item-item (principal)
│   │   ├── content_based.py
│   │   ├── collaborative.py        ← SVD
│   │   └── hybrid.py               ← orquestra os três
│   ├── api/
│   │   └── main.py         ← endpoints FastAPI
│   ├── utils/
│   └── train_pipeline.py   ← pipeline completo de treino
├── data/
│   ├── raw/                ← MovieLens original (fora do Git)
│   └── processed/          ← Parquet gerado pelo pipeline
├── models_saved/           ← *.joblib (fora do Git)
├── notebooks/              ← EDA e experimentos
└── tests/
```

`data/` e `models_saved/` estão no `.gitignore`. A imagem se reconstrói sozinha:
o `bootstrap.py` baixa o dataset e treina durante o build, então não há binário
versionado nem build que só funciona numa máquina.

---

## Rodando fora do Docker

```bash
pip install -r requirements.txt
```

```bash
cp .env.example .env
```

A `TMDB_API_KEY` só é usada pelo pipeline de treino, para enriquecer metadados —
o treino roda sem ela com `--skip-tmdb`.

Baixe o dataset e treine os modelos:

```bash
python scripts/bootstrap.py
```

Suba a API:

```bash
uvicorn src.api.main:app --reload
```

Documentação interativa em `http://localhost:8000/docs`.

### Re-treinar

```bash
python -m src.train_pipeline --skip-tmdb
```

| Flag | Efeito |
|---|---|
| `--evaluate` | Inclui cross-validation |
| `--skip-tmdb` | Pula o enriquecimento de metadados |
| `--reset-tmdb` | Re-executa o enriquecimento, ignorando o checkpoint |

`ENV=production` troca o dataset para o `ml-latest` completo (32 milhões de
avaliações, 335 MB de download).

### Re-treino com os ratings do app

O motor é stateless — não guarda estado sobre usuários do app — mas o modelo
em si pode incorporar o que eles avaliam, num espaço de ids unificado com o
MovieLens. O desenho completo está nos comentários de
`scripts/retrain_with_app_ratings.py`; aqui vai só o comando.

**Roda fora do container da API**, num ambiente com `requirements-dev.txt`
instalado — não com `requirements.txt`. O script usa `psycopg` para falar com
o Postgres do backend, e essa dependência foi deliberadamente mantida fora da
imagem de produção: a API do motor nunca precisa de acesso a banco, só este
script precisa.

```bash
pip install -r requirements-dev.txt

# Aponte DB_HOST/DB_PORT para onde o Postgres estiver acessível — pela porta
# publicada do compose (localhost:5433 em dev) ou o host real em produção.
python scripts/export_app_ratings.py          # lê o Postgres do backend, gera data/exports/app_ratings.csv
python scripts/retrain_with_app_ratings.py     # exporta + treina + promove, tudo de uma vez
```

`retrain_with_app_ratings.py` treina num diretório temporário — o motor
continua servindo a versão anterior dos artefatos enquanto o treino roda — e
promove cada `.joblib` por arquivo ao final, com a versão anterior guardada em
`models_saved/backup/`. Cada ciclo registra RMSE/MAE em
`models_saved/metrics_history.jsonl`.

Depois de promovido, **reinicie o container do motor** para carregar os
artefatos novos — `HybridRecommender.load()` lê uma vez no startup, e
`models_saved/` é o mesmo diretório nos dois lados (script e container), então
basta um `docker compose restart recommendation-engine`.

Para automatizar, um cron semanal no host (não dentro do container da API —
ele não tem `psycopg`; precisa de uma máquina ou job dedicado com
`requirements-dev.txt` instalado e rede até o Postgres):

```cron
# toda segunda às 3h — recomendação não precisa de sinal fresco de minutos,
# e o treino sobre o ml-latest é caro o bastante para não valer a pena rodar
# com mais frequência.
0 3 * * 1 cd /caminho/para/recommendation-engine && python scripts/retrain_with_app_ratings.py >> /var/log/nextscene-retrain.log 2>&1
```

Vale rodar isso só quando o volume de ratings do app justificar — com poucas
dezenas de usuários, o sinal deles some no meio de 32 milhões de linhas do
MovieLens e o re-treino não muda nada mensurável.

---

## Endpoints

`API_PREFIX` é `/api/v1`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/recommend/history` | **É o que o backend usa.** Recebe o histórico de avaliações e devolve sugestões |
| `POST` | `/api/v1/recommend/cold-start` | A partir de uma lista de filmes curtidos no onboarding |
| `GET` | `/api/v1/recommend/{user_id}` | Só para usuários do MovieLens — avaliação do modelo. Carrega o SVD sob demanda: a primeira chamada leva minutos |
| `GET` | `/api/v1/movies/search?q=&limit=` | Busca por título no dataset |
| `GET` | `/` | Healthcheck |

**O serviço não tem autenticação e não publica porta no compose.** Só o backend
precisa alcançá-lo, pela rede interna.

Os endpoints de recomendação são declarados como `def`, não `async def`: eles
executam pandas/NumPy/sklearn, que são CPU-bound. Dentro de `async def` rodariam
no event loop e travariam o servidor a cada requisição; como `def`, o FastAPI os
despacha para o threadpool.

---

## Testes

```bash
python -m pytest tests/ -q
```

40 testes cobrindo o item-item, o histórico no híbrido, a elegibilidade por piso
de popularidade, o pré-processamento e os endpoints da API.
