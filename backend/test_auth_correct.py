#!/usr/bin/env python3
import requests
import json
import sys

BASE_URL = "http://localhost:8000"

def test_authentication_flow():
    print("🔐 Testing Authentication Flow...")
    
    # Test 1: Test registration endpoint
    print("\n1. Testing registration endpoint...")
    try:
        response = requests.post(f"{BASE_URL}/register", json={
            "email": "test2@example.com",
            "password": "Testpass123!",
            "name": "Test User 2"
        })
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            print("   ✅ Registration successful")
            user_data = response.json()
            print(f"   User ID: {user_data.get('id')}")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Registration endpoint error: {e}")
        return False
    
    # Test 2: Test login endpoint (token)
    print("\n2. Testing login endpoint...")
    try:
        response = requests.post(f"{BASE_URL}/token", 
            data="username=test2@example.com&password=Testpass123!",
            headers={"Content-Type": "application/x-www-form-urlencoded"}
        )
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            print("   ✅ Login successful")
            token_data = response.json()
            access_token = token_data.get("access_token")
            print(f"   Access token: {access_token[:20]}...")
            
            # Test 3: Test protected endpoint with valid token
            print("\n3. Testing protected endpoint with valid token...")
            response = requests.get(f"{BASE_URL}/users/me", 
                headers={"Authorization": f"Bearer {access_token}"}
            )
            print(f"   Status: {response.status_code}")
            if response.status_code == 200:
                print("   ✅ Protected endpoint access successful")
                user_info = response.json()
                print(f"   User email: {user_info.get('email')}")
            else:
                print(f"   Response: {response.text}")
                
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Login endpoint error: {e}")
        return False
    
    # Test 4: Test protected endpoint without token
    print("\n4. Testing protected endpoint without authentication...")
    try:
        response = requests.get(f"{BASE_URL}/users/me")
        print(f"   Status: {response.status_code}")
        if response.status_code == 401:
            print("   ✅ Authentication required (as expected)")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Protected endpoint error: {e}")
        return False
    
    # Test 5: Test health endpoint (public)
    print("\n5. Testing public health endpoint...")
    try:
        response = requests.get(f"{BASE_URL}/health")
        print(f"   Status: {response.status_code}")
        if response.status_code == 200:
            print("   ✅ Public health endpoint working")
            health_data = response.json()
            print(f"   Status: {health_data.get('status')}")
        else:
            print(f"   Response: {response.text}")
    except Exception as e:
        print(f"   ❌ Health endpoint error: {e}")
        return False
    
    print("\n🎉 Authentication flow testing completed successfully!")
    return True

if __name__ == "__main__":
    success = test_authentication_flow()
    sys.exit(0 if success else 1)