import requests
import sys

BASE_URL = "http://localhost:8000"

def test_leaderboard():
    # 1. Register/Login to get token
    email = "test_leaderboard_v2@example.com"
    password = "password123"
    
    # Try login first
    try:
        session = requests.Session()
        session.trust_env = False  # Disable reading proxies from env
        
        r = session.post(f"{BASE_URL}/token", data={"username": email, "password": password})
        if r.status_code != 200:
            print(f"Login failed: {r.status_code}, trying register...")
            # Register if login fails
            r = session.post(f"{BASE_URL}/register", json={"email": email, "password": password})
            if r.status_code == 200:
                print("Registered user.")
                # Login again
                r = session.post(f"{BASE_URL}/token", data={"username": email, "password": password})
        
        if r.status_code == 200:
            token = r.json()["access_token"]
            print("Got token.")
            
            # 2. Get Leaderboard
            headers = {"Authorization": f"Bearer {token}"}
            r = session.get(f"{BASE_URL}/dashboard/leaderboard", headers=headers)
            print(f"Leaderboard Status: {r.status_code}")
            if r.status_code == 200:
                data = r.json()
                print(f"Leaderboard items: {len(data)}")
                if len(data) > 0:
                    print("Sample item:", data[0])
                else:
                    print("Leaderboard is empty (expected if DB is empty, but we populated it earlier)")
            else:
                print("Failed to get leaderboard:", r.text)
        else:
            print("Failed to login:", r.text)
            
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    test_leaderboard()
