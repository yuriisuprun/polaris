# Groq API Migration

## Overview
The Memora MCP server has been successfully migrated from OpenAI API to Groq API. This migration provides access to faster, more cost-effective LLM models while maintaining the same functionality.

## Changes Made

### 1. Dependencies (`requirements.txt`)
- **Removed**: `openai==1.67.0`
- **Added**: `groq==0.12.0`

### 2. Configuration (`app/core/config.py`)
Updated all OpenAI-specific settings to Groq equivalents:

| Old Setting | New Setting | Default Value |
|---|---|---|
| `OPENAI_API_KEY` | `GROQ_API_KEY` | None |
| `OPENAI_MODEL` | `GROQ_MODEL` | `llama-3.1-8b-instant` |
| `OPENAI_TIMEOUT_S` | `GROQ_TIMEOUT_S` | 30 |
| `OPENAI_MAX_RETRIES` | `GROQ_MAX_RETRIES` | 3 |

### 3. LLM Client (`app/core/llm.py`)
- Replaced `AsyncOpenAI` with `AsyncGroq`
- Updated JSON schema format from OpenAI's `json_schema` to Groq's `json_object` format
- Removed OpenAI-specific error handling (e.g., `insufficient_quota` check)
- Updated all logging messages to reference Groq instead of OpenAI
- Updated mock client response messages

### 4. Environment Configuration (`.env.example`)
Updated example environment variables with Groq configuration:
```env
GROQ_API_KEY=gsk_......
GROQ_MODEL=llama-3.1-8b-instant
GROQ_TIMEOUT_S=30
GROQ_MAX_RETRIES=3
```

## Available Groq Models

Based on the provided model list, here are recommended models for different use cases:

### Fast & Efficient (Recommended for this project)
- **`llama-3.1-8b-instant`** - Fast, 131K context window, good for most tasks
- **`llama-3.3-70b-versatile`** - More capable, 131K context, 32K max completion tokens

### Specialized Models
- **`groq/compound-mini`** - Lightweight, 8K max completion tokens
- **`groq/compound`** - Balanced performance, 8K max completion tokens
- **`qwen/qwen3-32b`** - Alibaba's model, 40K max completion tokens

### Other Available Models
- `allam-2-7b` - SDAIA model
- `canopylabs/orpheus-v1-english` - Canopy Labs model
- `meta-llama/llama-4-scout-17b-16e-instruct` - Meta's scout model
- `openai/gpt-oss-*` - OpenAI OSS models (various sizes)
- `whisper-large-v3*` - Speech-to-text models

## Setup Instructions

### 1. Get a Groq API Key
1. Visit [console.groq.com](https://console.groq.com)
2. Sign up for a free account
3. Navigate to API Keys section
4. Create a new API key
5. Copy the key (starts with `gsk_`)

### 2. Update Environment
Create or update `.env` file:
```env
GROQ_API_KEY=gsk_your_api_key_here
GROQ_MODEL=llama-3.1-8b-instant
APP_ENV=dev
LOG_LEVEL=INFO
USE_MOCK_LLM=false
```

### 3. Install Dependencies
```bash
pip install -r requirements.txt
```

### 4. Run the Application
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Rate Limiting

Groq's free tier has rate limits:
- **3 requests per minute** (default: 20 seconds between requests)
- Configurable via `RATE_LIMIT_MIN_INTERVAL_S` environment variable

For paid tiers, you can reduce this interval:
```env
RATE_LIMIT_MIN_INTERVAL_S=1.0  # For paid tier with higher limits
```

## Fallback Behavior

The application includes a mock LLM client that automatically activates if:
1. No valid `GROQ_API_KEY` is provided
2. API calls fail after all retries are exhausted
3. `USE_MOCK_LLM=true` is set in environment

This ensures the application continues to work even without a valid API key, useful for development and testing.

## Testing

Run tests to verify the migration:
```bash
pytest -v
```

## Performance Comparison

### Groq Advantages
- **Faster inference**: Groq's LPU (Language Processing Unit) provides faster token generation
- **Lower cost**: Free tier available with generous limits
- **Simpler API**: Fewer configuration options needed
- **Better for structured output**: Native JSON schema support

### Considerations
- Model selection is more limited compared to OpenAI
- Some advanced features (like function calling) may differ
- Rate limits are stricter on free tier

## Troubleshooting

### "No valid Groq API key found"
- Ensure `GROQ_API_KEY` is set in `.env` file
- Verify the key starts with `gsk_`
- Check that the key is valid and not expired

### Rate limit errors (429)
- The application automatically retries with exponential backoff
- Increase `RATE_LIMIT_MIN_INTERVAL_S` if errors persist
- Consider upgrading to a paid Groq tier

### JSON parsing errors
- Ensure the model supports JSON schema mode
- Try a different model from the available list
- Check logs for detailed error messages

## Reverting to OpenAI (if needed)

To revert back to OpenAI:
1. Restore `openai==1.67.0` in `requirements.txt`
2. Update `app/core/config.py` to use `openai_*` settings
3. Restore original `app/core/llm.py` from git history
4. Update `.env` with `OPENAI_API_KEY`
5. Run `pip install -r requirements.txt`

## References

- [Groq Console](https://console.groq.com)
- [Groq API Documentation](https://console.groq.com/docs)
- [Available Models](https://console.groq.com/docs/models)
