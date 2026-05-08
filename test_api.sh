#!/bin/bash

# Test script for Memora API with proper UTF-8 encoding

# Create a temporary JSON file with proper UTF-8 encoding
cat > /tmp/flashcard_request.json << 'EOF'
{
  "topic": "Italiano",
  "text": "Il passato prossimo si usa per azioni concluse nel passato. Si forma con il presente del verbo ausiliare (avere o essere) e il participio passato del verbo principale. Ad esempio: Ho mangiato una mela. Sono andato a scuola. Il passato prossimo è il tempo più usato nella conversazione per parlare di azioni passate. Si usa anche per azioni recenti o che hanno conseguenze nel presente.",
  "num_cards": 10
}
EOF

# Send the request using curl with the file
echo "Testing flashcard generation..."
curl -X POST http://localhost:8000/generate_flashcards \
  -H "Content-Type: application/json; charset=utf-8" \
  -d @/tmp/flashcard_request.json \
  -s | jq .

# Clean up
rm /tmp/flashcard_request.json
