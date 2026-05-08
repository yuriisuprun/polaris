# Curl UTF-8 Encoding Issue - Solution

## Problem
When using `curl` on Windows (MINGW64) to send JSON with special characters (Italian accents, Unicode), the request fails with:
```
{"error":{"code":"HTTP_ERROR","message":"There was an error parsing the body"}}
```

This is a **curl encoding issue on Windows**, not an application issue.

## Root Cause
- Curl on Windows doesn't properly encode UTF-8 characters when passing JSON via `-d` parameter
- The JSON gets corrupted during transmission
- FastAPI receives malformed JSON and returns 400 Bad Request

## Solutions

### Solution 1: Use a JSON File (Recommended for curl)
Create a file with proper UTF-8 encoding and pass it to curl:

```bash
# Create request.json with UTF-8 encoding
cat > request.json << 'EOF'
{
  "topic": "Italiano",
  "text": "Il passato prossimo si usa per azioni concluse nel passato. Si forma con il presente del verbo ausiliare (avere o essere) e il participio passato del verbo principale. Ad esempio: Ho mangiato una mela. Sono andato a scuola. Il passato prossimo è il tempo più usato nella conversazione per parlare di azioni passate. Si usa anche per azioni recenti o che hanno conseguenze nel presente.",
  "num_cards": 10
}
EOF

# Send request using the file
curl -X POST http://localhost:8000/generate_flashcards \
  -H "Content-Type: application/json; charset=utf-8" \
  -d @request.json
```

### Solution 2: Use PowerShell (Recommended for Windows)
PowerShell handles UTF-8 encoding properly:

```powershell
$requestBody = @{
    topic = "Italiano"
    text = "Il passato prossimo si usa per azioni concluse nel passato. Si forma con il presente del verbo ausiliare (avere o essere) e il participio passato del verbo principale. Ad esempio: Ho mangiato una mela. Sono andato a scuola. Il passato prossimo è il tempo più usato nella conversazione per parlare di azioni passate. Si usa anche per azioni recenti o che hanno conseguenze nel presente."
    num_cards = 10
} | ConvertTo-Json

$utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($requestBody)

Invoke-WebRequest -Uri "http://localhost:8000/generate_flashcards" `
  -Method POST `
  -Headers @{"Content-Type"="application/json; charset=utf-8"} `
  -Body $utf8Bytes `
  -UseBasicParsing | Select-Object -ExpandProperty Content
```

### Solution 3: Use Postman or Insomnia
GUI tools handle encoding automatically and are recommended for testing APIs with special characters.

### Solution 4: Use Python requests
```python
import requests
import json

data = {
    "topic": "Italiano",
    "text": "Il passato prossimo si usa per azioni concluse nel passato...",
    "num_cards": 10
}

response = requests.post(
    "http://localhost:8000/generate_flashcards",
    json=data,
    headers={"Content-Type": "application/json; charset=utf-8"}
)

print(json.dumps(response.json(), indent=2, ensure_ascii=False))
```

## Why This Happens
- **Curl on Windows**: Uses the system's default code page (often not UTF-8)
- **PowerShell**: Natively supports UTF-8 encoding
- **JSON files**: Can be saved with explicit UTF-8 encoding
- **Python/Postman**: Handle encoding automatically

## Verification
The application is working correctly. The issue is purely with how curl sends the data on Windows.

✅ **Application Status**: Working correctly
✅ **Groq API Integration**: Functional
✅ **JSON Schema Handling**: Correct
❌ **Curl on Windows with UTF-8**: Encoding issue (not application issue)

## Recommendation
For Windows users:
1. Use **PowerShell** for testing (recommended)
2. Use **JSON files** with curl
3. Use **Postman/Insomnia** for GUI testing
4. Avoid passing JSON directly via curl `-d` with special characters

## Test Files Provided
- `test_api.ps1` - PowerShell test script (recommended)
- `test_api.sh` - Bash script for Linux/Mac
- `request.json` - Sample JSON file for curl
