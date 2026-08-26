FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY src/ ./src/
COPY scripts/ ./scripts/

# Baixa o MovieLens e treina os modelos dentro da imagem.
#
# A versão anterior fazia `COPY data/ ./data/` e `COPY models_saved/ ...`, mas
# ambos estão no .gitignore: a imagem só buildava em quem já tinha os arquivos
# em disco, e em clone limpo o build quebrava. Além disso, o `data/` local tem
# 918 MB e ia inteiro para o contexto de build a cada execução.
RUN python scripts/bootstrap.py

# Usuário sem privilégios, como no Dockerfile do backend. Criado depois do
# bootstrap porque o treino escreve em /app/data e /app/models_saved; a partir
# daqui o processo só lê esses diretórios.
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser     && chown -R appuser:appuser /app
USER appuser

EXPOSE 8000

# Cada worker carrega os modelos e o dataset em memória — subir WORKERS aumenta
# a vazão proporcionalmente à RAM disponível. 2 é um default conservador.
ENV WORKERS=2

CMD ["sh", "-c", "uvicorn src.api.main:app --host 0.0.0.0 --port 8000 --workers ${WORKERS}"]
