# Study Helper MCP Server (FastAPI)

Production-ready MCP-style tool server for learning workflows:

- Flashcard generator
- Quiz generator
- Simple explanation tool
- Spaced repetition scheduler (SM-2 inspired)

## Requirements

- Python 3.11+ (Docker image uses `python:3.11-slim`)
- An OpenAI API key in `OPENAI_API_KEY`

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

## Docker

```bash
cd study-helper-mcp
cp .env.example .env
# edit .env and set OPENAI_API_KEY
docker-compose up --build
```

## API usage examples (curl)

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
cd study-helper-mcp
pytest -q
```

