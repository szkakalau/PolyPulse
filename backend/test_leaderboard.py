import requests
import json
import sys
import os
from datetime import datetime

# Add current directory to path so we can import app
sys.path.append(os.getcwd())

from app.services.market_service import MarketService
from app.database import save_whale_trade

def fetch_recent_closed_markets():
    ms = MarketService()
    try:
        print("Fetching closed markets using MarketService...")
        markets = ms.fetch_closed_markets(limit=10)
        
        for m in markets:
            print(f"Market: {m.question}")
            
            for t in m.tokens:
                if t.winner:
                    print(f"  WINNER: {t.outcome} (ID: {t.token_id})")
                    # Try to fetch trades for this winner
                    check_trades(t.token_id, m.question, t.outcome, m.market_slug)
            print("-" * 40)
            
    except Exception as e:
        print(f"Error: {e}")

def check_trades(token_id, market_question, outcome, market_slug):
    # Use 'asset' parameter as verified by test_params.py
    url = f"https://data-api.polymarket.com/trades"
    params = {
        "asset": token_id,
        "limit": 50 # Fetch last 50 trades
    }
    
    try:
        # Force no proxy for Data API as well
        r = requests.get(url, params=params, proxies={"http": None, "https": None})
        trades = r.json()
        print(f"  Trades found: {len(trades)}")
        
        count = 0
        for t in trades:
            try:
                size = float(t.get('size', 0))
                price = float(t.get('price', 0))
                value_usd = size * price
                
                # Filter for "Whale" size (> $1000)
                if value_usd >= 1000:
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
                
        if count > 0:
            print(f"  Saved {count} whale trades to DB")
            
    except Exception as e:
        print(f"  Error fetching trades: {e}")

if __name__ == "__main__":
    fetch_recent_closed_markets()
