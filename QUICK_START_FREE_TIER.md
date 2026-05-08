# Quick Start: Free Tier OpenAI Support

## TL;DR

The server now works with OpenAI free tier by:
- **Throttling requests** to 3 per minute (20 seconds apart)
- **Retrying rate limits** automatically with exponential backoff
- **Caching responses** for 5 minutes to reduce API calls
- **Falling back to mock** responses if rate limits persist

## Setup (5 minutes)

### 1. Update `.env`
```bash
# Copy from .env.example (already has the new settings)
cp .env.example .env

# Add your OpenAI API key
OPENAI_API_KEY=sk-proj-your-key-here
```

### 2. Start the server
```bash
docker-compose up --build
```

### 3. Test it
```bash
curl -X POST http://localhost:8000/generate_flashcards \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Italiano",
    "text": "Il passato prossimo si usa per azioni concluse nel passato.",
    "num_cards": 10
  }'
```

## Configuration

### For Free Tier (Default)
```bash
RATE_LIMIT_MIN_INTERVAL_S=20.0    # 3 requests/minute
RATE_LIMIT_MAX_RETRIES=5           # Retry up to 5 times
```

### For Paid Tier
```bash
# Adjust based on your plan's rate limits
RATE_LIMIT_MIN_INTERVAL_S=1.0      # For ~60 requests/minute
RATE_LIMIT_MAX_RETRIES=3
```

## What to Expect

### First Request
- Takes 1-2 seconds (API call to OpenAI)
- Response is cached for 5 minutes

### Second Request (Same topic/text)
- Takes ~10ms (instant from cache)

### Rapid Requests
- First: Immediate
- Second: Waits ~20 seconds (free tier throttle)
- Third: Waits ~20 seconds

### If Rate Limited
- Automatically retries with delays: 2s, 4s, 8s, 16s, 32s
- Falls back to mock responses if all retries fail

## Monitoring

### Check if it's working
```bash
docker-compose logs --follow | grep -i "rate limit"
```

### Expected log messages
```
# Successful request
"message": "LLM request successful"

# Cache hit
"message": "Cache hit for LLM request"

# Rate limited (will retry)
"message": "OpenAI rate limited (429). Retrying in 2.0s"

# Quota exhausted (falls back to mock)
"message": "OpenAI quota exhausted, falling back to mock client"
```

## Common Issues

| Issue | Solution |
|-------|----------|
| Getting mock responses | Check OpenAI API key is valid and has quota |
| Requests too slow | Reduce `RATE_LIMIT_MIN_INTERVAL_S` if on paid tier |
| Still getting 429 errors | Increase `RATE_LIMIT_MAX_RETRIES` or check quota |
| Cache not working | Check logs for "Cache hit" messages |

## API Endpoints

All endpoints work the same as before:

- `POST /generate_flashcards` - Generate study flashcards
- `POST /create_quiz` - Create multiple-choice quizzes
- `POST /explain_simple` - Simplify complex topics
- `POST /schedule_review` - Schedule spaced repetition reviews

## Example Workflow

```bash
# 1. Generate flashcards (waits for API)
curl -X POST http://localhost:8000/generate_flashcards \
  -H "Content-Type: application/json" \
  -d '{"topic":"Photosynthesis","text":"...","num_cards":10}'

# 2. Wait 20 seconds (free tier throttle)

# 3. Create quiz (waits for API)
curl -X POST http://localhost:8000/create_quiz \
  -H "Content-Type: application/json" \
  -d '{"topic":"Photosynthesis","text":"...","num_questions":5}'

# 4. Wait 20 seconds

# 5. Explain topic (waits for API)
curl -X POST http://localhost:8000/explain_simple \
  -H "Content-Type: application/json" \
  -d '{"topic":"Photosynthesis","text":"...","target_audience":"beginner"}'
```

## Performance Tips

1. **Reuse the same topic/text** - Responses are cached for 5 minutes
2. **Batch requests** - Space them out by 20+ seconds on free tier
3. **Use mock mode for testing** - Set `USE_MOCK_LLM=true` to skip API entirely
4. **Upgrade to paid tier** - If you need faster requests

## Need Help?

- Check `FREE_TIER_SUPPORT.md` for detailed documentation
- Review logs: `docker-compose logs --follow`
- Check OpenAI account: https://platform.openai.com/account/billing/overview
- Verify API key is valid and has available quota

## What Changed?

- ✅ Request throttling (20 seconds between requests)
- ✅ Exponential backoff retries for rate limits
- ✅ Smart 5-minute response caching
- ✅ Better error handling and logging
- ✅ Configurable for different OpenAI plans
- ✅ Graceful fallback to mock responses

All existing endpoints work exactly the same - just more reliable on free tier!
