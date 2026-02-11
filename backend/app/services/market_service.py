import requests
from typing import List, Dict
from app.models.market import Market, Token

class MarketService:
    BASE_URL = "https://clob.polymarket.com"

    def fetch_active_markets(self, limit: int = 50) -> List[Market]:
        try:
            response = requests.get(
                f"{self.BASE_URL}/markets", 
                params={
                    "limit": limit, 
                    "active": "true", 
                    "order": "volume",
                    "closed": "false"
                }
            )
            response.raise_for_status()
            data = response.json()
            
            markets = []
            for item in data.get('data', []):
                tokens = []
                if 'tokens' in item:
                    for t in item['tokens']:
                        tokens.append(Token(
                            token_id=t.get('token_id', ''),
                            outcome=t.get('outcome', ''),
                            price=float(t.get('price', 0.0) or 0.0)
                        ))
                
                markets.append(Market(
                    condition_id=item.get('condition_id', ''),
                    question=item.get('question', ''),
                    tokens=tokens,
                    volume=float(item.get('volume', 0.0) or 0.0),
                    tags=item.get('tags', [])
                ))
            return markets
        except Exception as e:
            print(f"Error fetching markets: {e}")
            return []

    def get_market_prices(self, market_id: str):
        # Implementation for detailed book fetching if needed
        pass
