import requests
import time
from datetime import datetime

BASE_URL = "https://clob.polymarket.com"

def fetch_markets():
    print(f"[{datetime.now()}] Fetching markets...")
    try:
        session = requests.Session()
        session.trust_env = False
        response = session.get(f"{BASE_URL}/markets", params={"limit": 5, "active": "true", "order": "volume"})
        response.raise_for_status()
        data = response.json()
        
        for market in data['data']:
            print(f"- {market['question']} (ID: {market['condition_id']})")
            if 'volume' in market:
                print(f"  Volume: {market['volume']}")
            
            if 'tags' in market:
                print(f"  Tags: {market['tags']}")
            
            # Print tokens/outcomes
            if 'tokens' in market:
                for token in market['tokens']:
                    print(f"  * {token['outcome']}: {token.get('price', 'N/A')}")
            print("-" * 20)
            
    except Exception as e:
        print(f"Error fetching markets: {e}")

if __name__ == "__main__":
    fetch_markets()
