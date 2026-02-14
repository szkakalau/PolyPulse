import requests
import json

def test_params():
    # Use the token ID from previous output: Will Joe Biden win... No
    token_id = "60777290337556307846082122611643867373415691927705756558171303096586770149710"
    slug = "will-joe-biden-win-the-us-2024-democratic-presidential-nomination"
    
    print(f"Testing with Token ID: {token_id}")
    
    # Try 'asset'
    url = f"https://data-api.polymarket.com/trades?asset={token_id}&limit=1"
    print(f"Trying asset=: {url}")
    r = requests.get(url)
    if r.status_code == 200 and len(r.json()) > 0:
        print(f"Success! Asset in response: {r.json()[0].get('asset')}")
    else:
        print(f"Failed or empty. Status: {r.status_code}, Len: {len(r.json())}")

    # Try 'token_id'
    url = f"https://data-api.polymarket.com/trades?token_id={token_id}&limit=1"
    print(f"Trying token_id=: {url}")
    r = requests.get(url)
    if r.status_code == 200 and len(r.json()) > 0:
        print(f"Success! Asset in response: {r.json()[0].get('asset')}")
    else:
        print(f"Failed or empty. Status: {r.status_code}, Len: {len(r.json())}")

    # Try 'slug'
    url = f"https://data-api.polymarket.com/trades?slug={slug}&limit=1"
    print(f"Trying slug=: {url}")
    r = requests.get(url)
    if r.status_code == 200 and len(r.json()) > 0:
        print(f"Success! Slug in response: {r.json()[0].get('slug')}")
    else:
        print(f"Failed or empty. Status: {r.status_code}, Len: {len(r.json())}")

if __name__ == "__main__":
    test_params()
