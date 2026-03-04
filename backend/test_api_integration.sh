#!/bin/bash

# API Integration Testing Script
BASE_URL="http://localhost:8000"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxNzcyNjE3MzUxfQ.QhhigyJSJQ7ZqT4U1PZ-am8Kut_WcNOyPrpIJeDwxQ4"

echo "🔗 API Endpoint Integration Testing"
echo "===================================="

# Test 1: Public endpoints
echo "\n1. Testing Public Endpoints:"
echo "----------------------------"

# Health check
echo "- Health Check:"
curl -s "$BASE_URL/health" | jq '.status' 2>/dev/null || curl -s "$BASE_URL/health"
echo ""

# Test 2: User endpoints (protected)
echo "\n2. Testing User Endpoints:"
echo "---------------------------"

# Get current user
echo "- Get Current User:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/users/me" | jq '.email' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/users/me"
echo ""

# Test 3: Signals endpoints
echo "\n3. Testing Signals Endpoints:"
echo "-----------------------------"

# Get signals
echo "- Get Signals:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/signals" | jq '.[0:2]' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/signals" | head -50
echo ""

# Test 4: Whale endpoints
echo "\n4. Testing Whale Endpoints:"
echo "---------------------------"

# Get whale trades
echo "- Get Whale Trades:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/whales/trades" | jq '.[0:2]' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/whales/trades" | head -50
echo ""

# Test 5: Market endpoints
echo "\n5. Testing Market Endpoints:"
echo "----------------------------"

# Get markets
echo "- Get Markets:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/markets" | jq '.[0:2]' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/markets" | head -50
echo ""

# Test 6: Watchlist endpoints
echo "\n6. Testing Watchlist Endpoints:"
echo "--------------------------------"

# Get watchlist
echo "- Get Watchlist:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/watchlist" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/watchlist"
echo ""

# Test 7: Analytics endpoints
echo "\n7. Testing Analytics Endpoints:"
echo "--------------------------------"

# Get metrics
echo "- Get Metrics:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/analytics/metrics" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/analytics/metrics"
echo ""

echo "✅ API Integration Testing Completed"