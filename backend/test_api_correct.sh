#!/bin/bash

# API Integration Testing Script with Correct Endpoints
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

# Test 3: Dashboard endpoints
echo "\n3. Testing Dashboard Endpoints:"
echo "--------------------------------"

# Dashboard stats
echo "- Dashboard Stats:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/dashboard/stats" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/dashboard/stats"
echo ""

# Dashboard alerts
echo "- Dashboard Alerts:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/dashboard/alerts" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/dashboard/alerts"
echo ""

# Dashboard whales
echo "- Dashboard Whales:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/dashboard/whales" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/dashboard/whales"
echo ""

# Test 4: Signals endpoints
echo "\n4. Testing Signals Endpoints:"
echo "-----------------------------"

# Get signals
echo "- Get Signals:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/signals" | jq '.[0:2]' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/signals" | head -30
echo ""

# Signals stats
echo "- Signals Stats:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/signals/stats" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/signals/stats"
echo ""

# Test 5: Whale endpoints
echo "\n5. Testing Whale Endpoints:"
echo "---------------------------"

# Get whales
echo "- Get Whales:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/whales" | jq '.[0:2]' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/whales" | head -30
echo ""

# Whale leaderboard
echo "- Whale Leaderboard:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/whales/leaderboard" | jq '.[0:2]' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/whales/leaderboard" | head -30
echo ""

# Test 6: Watchlist endpoints
echo "\n6. Testing Watchlist Endpoints:"
echo "--------------------------------"

# Get watchlist
echo "- Get Watchlist:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/watchlist" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/watchlist"
echo ""

# Test 7: Insights endpoints
echo "\n7. Testing Insights Endpoints:"
echo "--------------------------------"

# Signal credibility
echo "- Signal Credibility:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/insights/credibility" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/insights/credibility"
echo ""

# Delivery insights
echo "- Delivery Insights:"
curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/insights/delivery" | jq '.' 2>/dev/null || curl -s -H "Authorization: Bearer $TOKEN" "$BASE_URL/insights/delivery"
echo ""

echo "✅ API Integration Testing Completed"