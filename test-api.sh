#!/bin/bash

# Test script for Shopping Chat Agent API
# Usage: ./test-api.sh [base-url]

BASE_URL=${1:-http://localhost:8080}

echo "Testing Shopping Chat Agent API at $BASE_URL"
echo "=============================================="
echo ""

# Test health endpoint
echo "1. Testing health endpoint..."
curl -s "$BASE_URL/api/chat/health" | jq .
echo ""
echo ""

# Test chat endpoint - price query
echo "2. Testing chat endpoint - price query..."
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me phones under 30000"}' | jq .
echo ""
echo ""

# Test chat endpoint - brand query
echo "3. Testing chat endpoint - brand query..."
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me Samsung phones"}' | jq .
echo ""
echo ""

# Test chat endpoint - recommendation
echo "4. Testing chat endpoint - recommendation..."
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "Recommend a good phone with fast charging"}' | jq .
echo ""
echo ""

# Test chat endpoint - comparison
echo "5. Testing chat endpoint - comparison..."
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": "Compare OnePlus 12R and Pixel 8a"}' | jq .
echo ""
echo ""

# Test chat endpoint - validation error
echo "6. Testing chat endpoint - validation (empty message)..."
curl -s -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message": ""}' | jq .
echo ""
echo ""

echo "Testing complete!"

