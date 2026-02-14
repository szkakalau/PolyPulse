import requests
import sys
import os
from datetime import datetime

# Add current directory to path so we can import app
sys.path.append(os.getcwd())

from app.database import save_whale_trade

WINNER_TOKENS = [
    {"id": "63091090051089570059675982328375731315493169331993358528823242988118415010764", "question": "NBA: Mavericks vs. 76ers", "outcome": "Mavericks", "slug": "nba-mavericks-vs-76ers"},
    {"id": "41599347646852443876339077697554725560792338847587623564870641846841964733124", "question": "NBA: Jazz vs. Spurs", "outcome": "Spurs", "slug": "nba-jazz-vs-spurs"},
    {"id": "59861330705732192422554448405194660909823547837401419491334073049300548017670", "question": "Price of $BLUR > $0.50?", "outcome": "Yes", "slug": "price-of-blur-above-050"},
    {"id": "60777290337556307846082122611643867373415691927705756558171303096586770149710", "question": "Biden 2024 Dem Nom?", "outcome": "No", "slug": "biden-2024-dem-nom"},
    {"id": "5703868841146927779583861617901356196807880429247630041420370557304716035367", "question": "UFC 285: Nickal vs Pickett", "outcome": "Yes", "slug": "ufc-285-nickal-vs-pickett"}
]

def check_trades(token_id, market_question, outcome, market_slug):
    url = f"https://data-api.polymarket.com/trades"
    params = {
        "asset": token_id,
        "limit": 50
    }
    
    try:
        # Force no proxy
        r = requests.get(url, params=params, proxies={"http": None, "https": None})
        trades = r.json()
        print(f"Fetching trades for {market_question} ({outcome})... found {len(trades)}")
        
        count = 0
        for t in trades:
            try:
                size = float(t.get('size', 0))
                price = float(t.get('price', 0))
                value_usd = size * price
                
                # Lower threshold for testing to ensure we get some data
                if value_usd >= 100:
                    trade_record = {
                        "market_question": market_question,
                        "outcome": outcome,
                        "side": t.get('side', 'BUY'), 
                        "size": size,
                        "price": price,
                        "value_usd": value_usd,
                        "timestamp": t.get('timestamp'),
                        "maker_address": t.get('proxyWallet') or t.get('maker_address', 'Unknown'),
                        "market_slug": market_slug
                    }
                    save_whale_trade(trade_record)
                    count += 1
            except Exception as e:
                continue
                
        print(f"  Saved {count} trades to DB")
            
    except Exception as e:
        print(f"  Error fetching trades: {e}")

if __name__ == "__main__":
    print("Populating Smart Money Data...")
    for item in WINNER_TOKENS:
        check_trades(item["id"], item["question"], item["outcome"], item["slug"])
    print("Done.")
