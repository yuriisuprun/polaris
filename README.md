# Memora MCP Server (FastAPI + Groq)

Memora is a production-focused study helper API with:

- Flashcard generation
- Quiz generation
- Simple explanation generation
- Spaced repetition scheduling

## Requirements

- Python 3.11+
- `GROQ_API_KEY` (required)

## Configuration

- `APP_ENV`: `dev` (default) or `prod` / `production`
- `GROQ_API_KEY`: Groq API key (required)
- `GROQ_MODEL`: model name (default `llama-3.1-8b-instant`)
- `GROQ_TIMEOUT_S`: request timeout in seconds (default `30`)
- `GROQ_MAX_RETRIES`: retries for Groq rate limit errors (default `3`)
- `RATE_LIMIT_MIN_INTERVAL_S`: minimum interval between requests (default `20.0`)
- `LOG_LEVEL`: `DEBUG|INFO|WARNING|ERROR|CRITICAL` (default `INFO`)
- `STORAGE_PATH`: spaced repetition storage file (default `./data/storage.json`)

## Local Run

```bash
python -m venv .venv
source .venv/bin/activate  # PowerShell: .venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Create `.env`:

```bash
APP_ENV=dev
GROQ_API_KEY=your_key_here
GROQ_MODEL=llama-3.1-8b-instant
GROQ_TIMEOUT_S=30
GROQ_MAX_RETRIES=3
RATE_LIMIT_MIN_INTERVAL_S=20.0
LOG_LEVEL=INFO
STORAGE_PATH=./data/storage.json
```

Start:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Docs:

- `http://localhost:8000/docs`
- `http://localhost:8000/health`

## Docker

```bash
docker-compose up --build
```

## Test

```bash
pytest -q
```
