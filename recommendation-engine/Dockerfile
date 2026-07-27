FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code and data
COPY src/ ./src/
COPY data/ ./data/
COPY models_saved/ ./models_saved/

EXPOSE 8000

# Cada worker carrega os modelos e o dataset em memória — subir WORKERS aumenta
# a vazão proporcionalmente à RAM disponível. 2 é um default conservador.
ENV WORKERS=2

CMD ["sh", "-c", "uvicorn src.api.main:app --host 0.0.0.0 --port 8000 --workers ${WORKERS}"]
