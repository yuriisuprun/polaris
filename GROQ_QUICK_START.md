# Groq API Quick Start Guide

## 1. Get Your API Key (2 minutes)

1. Go to [console.groq.com](https://console.groq.com)
2. Click "Sign Up" and create a free account
3. Navigate to **API Keys** in the left sidebar
4. Click **Create API Key**
5. Copy your key (starts with `gsk_`)

## 2. Configure Your Environment

Create or update `.env` file in the project root:

```env
GROQ_API_KEY=gsk_your_key_here
GROQ_MODEL=llama-3.1-8b-instant
APP_ENV=dev
LOG_LEVEL=INFO
USE_MOCK_LLM=false
```

## 3. Install Dependencies

```bash
pip install -r requirements.txt
```

## 4. Run the Application

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Visit `http://localhost:8000/docs` to see the API documentation.

## 5. Test It Out

### Generate Flashcards
```bash
curl -X POST http://localhost:8000/generate_flashcards \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Photosynthesis",
    "text": "Photosynthesis is the process by which plants convert light energy into chemical energy...",
    "num_cards": 5
  }'
```

### Create a Quiz
```bash
curl -X POST http://localhost:8000/create_quiz \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Photosynthesis",
    "text": "Photosynthesis is the process by which plants convert light energy into chemical energy...",
    "num_questions": 3,
    "num_options": 4
  }'
```

### Explain a Concept
```bash
curl -X POST http://localhost:8000/explain_simple \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Photosynthesis",
    "text": "Photosynthesis is the process by which plants convert light energy into chemical energy...",
    "target_audience": "beginner"
  }'
```

## Recommended Models

| Model | Speed | Quality | Context | Best For |
|-------|-------|---------|---------|----------|
| `llama-3.1-8b-instant` | ⚡⚡⚡ | ⭐⭐⭐ | 131K | **Default choice** |
| `llama-3.3-70b-versatile` | ⚡⚡ | ⭐⭐⭐⭐ | 131K | Complex tasks |
| `groq/compound` | ⚡⚡⚡ | ⭐⭐⭐ | 131K | Lightweight |

## Troubleshooting

### "No valid Groq API key found"
- Check `.env` file has `GROQ_API_KEY=gsk_...`
- Verify the key is correct (copy from console.groq.com)
- Restart the application

### Rate limit errors (429)
- Free tier: 3 requests/minute (default 20s between requests)
- Increase `RATE_LIMIT_MIN_INTERVAL_S` in `.env` if needed
- Or upgrade to paid tier for higher limits

### JSON parsing errors
- Try a different model: `llama-3.3-70b-versatile`
- Check application logs for details
- Ensure text input is not empty

## Development Mode

To test without using API quota:

```env
USE_MOCK_LLM=true
```

This uses deterministic mock responses instead of calling Groq API.

## Next Steps

- Read [GROQ_MIGRATION.md](./GROQ_MIGRATION.md) for detailed migration info
- Check [README.md](./README.md) for full project documentation
- Explore [console.groq.com/docs](https://console.groq.com/docs) for API details

## Support

- **Groq Docs**: https://console.groq.com/docs
- **API Status**: https://status.groq.com
- **Community**: https://discord.gg/groq
