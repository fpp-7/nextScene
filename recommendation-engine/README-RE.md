# 🎬 NextScene

> Sistema de recomendação inteligente de filmes baseado em filtragem colaborativa híbrida.

## Stack

- **ML:** Scikit-learn, Scikit-Surprise (SVD), Pandas, NumPy
- **API:** FastAPI + Uvicorn
- **Dados:** MovieLens (small para dev, full para prod)
- **Integração:** TMDB API

## Arquitetura do Modelo

```
Usuário
   │
   ▼
HybridRecommender
   ├── ContentBasedModel  (TF-IDF tags + Gêneros + Era)
   └── CollaborativeModel (SVD)
         │
         ▼
   Pesos dinâmicos por estágio:
   cold_start  →  0% CF + 100% CB
   warming_up  → 30% CF +  70% CB
   active      → 60% CF +  40% CB
   power_user  → 75% CF +  25% CB
```

## Estrutura

```
nextscene/
├── notebooks/              ← EDA e experimentos
├── src/
│   ├── config.py           ← Configuração central
│   ├── preprocessing/
│   │   ├── cleaner.py      ← Limpeza e carga dos dados
│   │   └── feature_engineering.py  ← TF-IDF, one-hot, eras
│   ├── models/
│   │   ├── content_based.py
│   │   ├── collaborative.py
│   │   └── hybrid.py
│   ├── api/
│   │   └── main.py         ← FastAPI endpoints
│   └── train_pipeline.py   ← Pipeline completo
├── data/
│   ├── raw/                ← MovieLens original (não commitar)
│   └── processed/          ← Parquet gerado automaticamente
├── models_saved/           ← Modelos serializados (.joblib)
├── tests/
└── requirements.txt
```

## Setup

```bash
# 1. Instalar dependências
pip install -r requirements.txt

# 2. Configurar variáveis de ambiente
cp .env.example .env
# edite o .env com sua TMDB_API_KEY

# 3. Baixar datasets MovieLens
# https://grouplens.org/datasets/movielens/
# Extrair em data/raw/ml-latest-small  (dev)
# Extrair em data/raw/ml-latest        (prod)

# 4. Treinar os modelos
python -m src.train_pipeline

# 5. Iniciar a API
uvicorn src.api.main:app --reload
```

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/v1/recommend/{user_id}` | Recomendações para usuário existente |
| POST | `/api/v1/recommend/cold-start` | Recomendações para novo usuário |
| GET | `/api/v1/movies/search?q=` | Busca filmes por título |

Documentação interativa: `http://localhost:8000/docs`

## Testes

```bash
pytest tests/
```
