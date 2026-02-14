import requests
import time
import json

BASE_URL = "http://localhost:8000"
SESSION = requests.Session()
SESSION.trust_env = False  # Ignore proxy env vars

def print_step(step):
    print(f"\n{'='*20} {step} {'='*20}")

def test_integration():
    email = "integration_test@example.com"
    password = "password123"

    # 1. Register
    print_step("1. Authentication")
    print(f"Registering {email}...")
    r = SESSION.post(f"{BASE_URL}/register", json={"email": email, "password": password})
    if r.status_code == 200:
        print("Registration successful.")
    elif r.status_code == 400 and "already registered" in r.text:
        print("User already registered.")
    else:
        print(f"Registration failed: {r.status_code} {r.text}")
        return

    # 2. Login
    print(f"Logging in...")
    # OAuth2 expects form data
    r = SESSION.post(f"{BASE_URL}/token", data={"username": email, "password": password})
    if r.status_code == 200:
        token = r.json()["access_token"]
        print("Login successful. Token received.")
        headers = {"Authorization": f"Bearer {token}"}
    else:
        print(f"Login failed: {r.status_code} {r.text}")
        return

    # 2.5 Get Me
    print("Fetching User Info...")
    r = SESSION.get(f"{BASE_URL}/users/me", headers=headers)
    if r.status_code == 200:
        print(f"User Info: {r.json()}")
    else:
        print(f"Failed to fetch user info: {r.status_code} {r.text}")

    # 3. Dashboard Stats
    print_step("2. Dashboard")
    print("Fetching Dashboard Stats...")
    r = SESSION.get(f"{BASE_URL}/dashboard/stats", headers=headers)
    if r.status_code == 200:
        stats = r.json()
        print(f"Stats: {stats}")
        if "alerts_24h" in stats:
             print("Verified alerts_24h field present.")
    else:
        print(f"Failed to fetch stats: {r.status_code} {r.text}")

    # 4. Alerts (Dashboard)
    print("Fetching Alerts...")
    r = SESSION.get(f"{BASE_URL}/dashboard/alerts", headers=headers)
    if r.status_code == 200:
        alerts = r.json()
        print(f"Alerts count: {len(alerts)}")
    else:
        print(f"Failed to fetch alerts: {r.status_code} {r.text}")

    # 5. Whale Radar
    print_step("3. Whale Radar")
    print("Fetching Whale Activity...")
    r = SESSION.get(f"{BASE_URL}/dashboard/whales", headers=headers)
    if r.status_code == 200:
        whales = r.json()
        print(f"Whale activities: {len(whales)}")
        if whales:
            print(f"Sample whale: {whales[0]}")
    else:
        print(f"Failed to fetch whales: {r.status_code} {r.text}")

    # 6. Leaderboard
    print_step("4. Leaderboard")
    print("Fetching Leaderboard...")
    r = SESSION.get(f"{BASE_URL}/dashboard/leaderboard", headers=headers)
    if r.status_code == 200:
        leaders = r.json()
        print(f"Leaderboard items: {len(leaders)}")
        if leaders:
            print(f"Top Trader: {leaders[0]}")
    else:
        print(f"Failed to fetch leaderboard: {r.status_code} {r.text}")

if __name__ == "__main__":
    try:
        test_integration()
    except Exception as e:
        print(f"Integration Test Failed with Exception: {e}")
