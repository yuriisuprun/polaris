# Test script for Memora API with proper UTF-8 encoding

# Create request body with Italian text
$requestBody = @{
    topic = "Italiano"
    text = "Il passato prossimo si usa per azioni concluse nel passato. Si forma con il presente del verbo ausiliare (avere o essere) e il participio passato del verbo principale. Ad esempio: Ho mangiato una mela. Sono andato a scuola. Il passato prossimo è il tempo più usato nella conversazione per parlare di azioni passate. Si usa anche per azioni recenti o che hanno conseguenze nel presente."
    num_cards = 10
} | ConvertTo-Json

# Convert to UTF-8 bytes
$utf8Bytes = [System.Text.Encoding]::UTF8.GetBytes($requestBody)

# Send request
Write-Host "Testing flashcard generation with Italian text..."
$response = Invoke-WebRequest -Uri "http://localhost:8000/generate_flashcards" `
  -Method POST `
  -Headers @{"Content-Type"="application/json; charset=utf-8"} `
  -Body $utf8Bytes `
  -UseBasicParsing

# Parse and display response
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
