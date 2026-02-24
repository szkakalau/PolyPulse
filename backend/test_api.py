import requests
from app.services.market_service import MarketService

ms = MarketService()
markets = ms.fetch_active_markets(limit=1)
if markets:
    m = markets[0]
    print(f"Market: {m.question}")
    # The 'tokens' list contains token_ids. CLOB usually trades via Token ID (Asset ID)
    if m.tokens:
        token_id = m.tokens[0].token_id
        print(f"Token ID: {token_id}")
        
        # Try fetching trades via Data API
        url = f"https://data-api.polymarket.com/trades?market={token_id}"
        print(f"Testing URL: {url}")
        try:
            session = requests.Session()
            session.trust_env = False
            r = session.get(url, timeout=10)
            print(f"Status: {r.status_code}")
            if r.status_code == 200:
                data = r.json()
                print(f"Trades found: {len(data)}")
                if len(data) > 0:
                    print(data[0])
            else:
                print(r.text)
        except Exception as e:
            print(e)
