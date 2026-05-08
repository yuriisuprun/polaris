# Memora MCP Server (FastAPI)

Production-ready “tool server” for learning workflows (FastAPI HTTP API):

- Flashcard generator
- Quiz generator
- Simple explanation tool
- Spaced repetition scheduler (SM-2 inspired)

This server is designed to be used from:

- Your browser via Swagger UI (`/docs`)
- `curl` / Postman / Insomnia
- A small script (Python/JS) that calls the endpoints

## Requirements

- Python 3.11+ (Docker image uses `python:3.11-slim`)
- An OpenAI API key in `OPENAI_API_KEY` (required in production)

## Configuration

Environment variables:

- **`APP_ENV`**: `dev` (default) or `prod` / `production`
- **`OPENAI_API_KEY`**: OpenAI API key
- **`OPENAI_MODEL`**: model name (default `gpt-4.1-mini`)
- **`OPENAI_TIMEOUT_S`**: request timeout seconds (default `30`)
- **`OPENAI_MAX_RETRIES`**: retries for transient upstream failures (default `3`)
- **`LOG_LEVEL`**: `DEBUG|INFO|WARNING|ERROR|CRITICAL` (default `INFO`)
- **`STORAGE_PATH`**: JSON file path for spaced repetition state (default `./data/storage.json`)
- **`RATE_LIMIT_MIN_INTERVAL_S`**: minimum seconds between API requests for free tier compliance (default `20.0` = 3 req/min)
- **`RATE_LIMIT_MAX_RETRIES`**: number of retries for rate limit errors with exponential backoff (default `5`)

## Local setup

```bash
cd study-helper-mcp
python -m venv .venv
source .venv/bin/activate  # (Windows PowerShell: .venv\Scripts\Activate.ps1)
pip install -r requirements.txt
cp .env.example .env
```

Run:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Docs:

- Swagger UI: `http://localhost:8000/docs`
- Health: `http://localhost:8000/health`

## How to use it for studying (recommended workflow)

1. **Collect source material** into plain text:
   - Copy/paste notes, a textbook section, or an article snippet
   - Keep it focused (a few pages / a few thousand words works best)
2. **Explain first** (`/explain_simple`) to get a clean mental model:
   - Use `target_audience="beginner"` (default) or something more specific like `"high school"` / `"intermediate"`
3. **Generate flashcards** (`/generate_flashcards`) from the same text:
   - Start with `num_cards=10` and iterate
4. **Create a quiz** (`/create_quiz`) to test recall:
   - Start with `num_questions=5`, `num_options=4`
5. **Schedule reviews** (`/schedule_review`) after each recall attempt:
   - Use `difficulty` from `1` (easy) to `5` (hard)
   - Use a stable `user_id` and `item_id` so the server can keep repetition state

## Free Tier Support

This server is optimized to work with **OpenAI free tier** accounts:

- **Automatic rate limiting**: Enforces 20-second minimum between requests (3 req/min) by default
- **Exponential backoff retries**: Automatically retries rate-limited requests (429 errors) up to 5 times
- **Smart caching**: Caches responses for 5 minutes to reduce redundant API calls
- **Graceful fallback**: Falls back to mock responses if rate limits are exceeded after retries

### Adjusting for your plan

If you have a paid OpenAI plan with higher rate limits, adjust in `.env`:

```bash
# For paid tier with higher limits (e.g., 100 req/min)
RATE_LIMIT_MIN_INTERVAL_S=0.6  # ~100 requests per minute
RATE_LIMIT_MAX_RETRIES=3       # Fewer retries needed
```

## Docker

```bash
cd study-helper-mcp
cp .env.example .env
# edit .env and set OPENAI_API_KEY
docker-compose up --build
```

## API usage examples (curl)

Tip: every response includes:

- **`X-Request-ID`**: request trace id (you can also send `X-Request-ID` yourself)
- **`X-Response-Time-Ms`**: server processing time

Generate flashcards:

```bash
curl -X POST http://localhost:8000/generate_flashcards \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Photosynthesis",
    "text": "Photosynthesis converts light energy into chemical energy...",
    "num_cards": 10
  }'
```

Create a quiz:

```bash
curl -X POST http://localhost:8000/create_quiz \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "HTTP",
    "text": "HTTP is a stateless protocol used on the web...",
    "num_questions": 5,
    "num_options": 4
  }'
```

Explain simply:

```bash
curl -X POST http://localhost:8000/explain_simple \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Recursion",
    "text": "Recursion is when a function calls itself...",
    "target_audience": "beginner"
  }'
```

Schedule a review:

```bash
curl -X POST http://localhost:8000/schedule_review \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "user-123",
    "item_id": "flashcard-abc",
    "difficulty": 3,
    "previous_interval_days": 6,
    "ease_factor": 2.5
  }'
```

## Error responses

All errors are returned in a consistent envelope:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "request_id": "..."
  }
}
```

Validation errors include `error.details` with field-level information.

## Example: simple Python client

```python
import os
import requests

BASE = os.getenv("STUDY_HELPER_BASE_URL", "http://localhost:8000")

payload = {
    "topic": "Photosynthesis",
    "text": "Photosynthesis converts light energy into chemical energy...",
    "num_cards": 10,
}

r = requests.post(f"{BASE}/generate_flashcards", json=payload, timeout=60)
r.raise_for_status()
print(r.json())
```

## Render deployment (Docker runtime)

This repo includes `render.yaml` configured as a Docker web service.

Steps:

1. Push your repo to GitHub.
2. In Render, create a new **Web Service** from that repo.
3. Ensure the service root directory is `study-helper-mcp/` (Render uses `render.yaml`).
4. Add env var **OPENAI_API_KEY** in Render.
5. Deploy. The service will run `uvicorn app.main:app --host 0.0.0.0 --port 8000`.

## Testing

```bash
pytest -q
```

