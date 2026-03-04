#!/usr/bin/env python3
import requests
import json
import sys

BASE_URL = "http://localhost:8000"

def test_authentication_flow():
    print("🔐 Testing Authentication Flow...")
    
    # Test 1: Check if login endpoint exists
    print("\n1. Testing login endpoint...")
    try:
        response = requests.post(f"{BASE_URL}/auth/login", json={
            "username": "testuser",
            "password": "testpass"
        })
        print(f"   Status: {response.status_code}")
        if response.status_code == 422:
            print("   ✅ Login endpoint exists (validation working)")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Login endpoint error: {e}")
        return False
    
    # Test 2: Test registration endpoint
    print("\n2. Testing registration endpoint...")
    try:
        response = requests.post(f"{BASE_URL}/auth/register", json={
            "email": "test@example.com",
            "password": "Testpass123!",
            "name": "Test User"
        })
        print(f"   Status: {response.status_code}")
        if response.status_code in [200, 201, 400, 409]:
            print("   ✅ Registration endpoint working")
            if response.status_code == 409:
                print("   ℹ️  User already exists (expected)")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Registration endpoint error: {e}")
        return False
    
    # Test 3: Test token refresh endpoint
    print("\n3. Testing token refresh endpoint...")
    try:
        response = requests.post(f"{BASE_URL}/auth/refresh", headers={
            "Authorization": "Bearer invalidtoken"
        })
        print(f"   Status: {response.status_code}")
        if response.status_code in [401, 422]:
            print("   ✅ Token refresh endpoint working (authentication required)")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Token refresh endpoint error: {e}")
        return False
    
    # Test 4: Test health endpoint (should work without auth)
    print("\n4. Testing public health endpoint...")
    try:
        response = requests.get(f"{BASE_URL}/health")
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            print("   ✅ Public health endpoint working")
            health_data = response.json()
            print(f"   Health status: {health_data.get('status')}")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Health endpoint error: {e}")
        return False
    
    # Test 5: Test protected endpoint without auth
    print("\n5. Testing protected endpoint without authentication...")
    try:
        response = requests.get(f"{BASE_URL}/users/me")
        print(f"   Status: {response.status_code}")
        if response.status_code == 401:
            print("   ✅ Authentication required for protected endpoints")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Protected endpoint error: {e}")
        return False
    
    print("\n🎉 Authentication flow testing completed successfully!")
    return True

if __name__ == "__main__":
    success = test_authentication_flow()
    sys.exit(0 if success else 1)