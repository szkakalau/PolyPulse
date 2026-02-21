import os
import requests
from typing import List, Dict, Optional
from app.models.market import Market, Token

class MarketService:
    BASE_URL = "https://clob.polymarket.com"

    def _request_get(self, url: str, params: Dict) -> requests.Response:
        mode = os.getenv("POLYMARKET_PROXY_MODE", "auto").lower()
        if mode == "proxy":
            modes = [True]
        elif mode == "direct":
            modes = [False]
        else:
            modes = [True, False]
        last_exc: Optional[Exception] = None
        for use_proxy in modes:
            try:
                session = requests.Session()
                session.trust_env = use_proxy
                response = session.get(
                    url,
                    params=params,
                    headers={"User-Agent": "Mozilla/5.0"},
                    timeout=10
                )
                response.raise_for_status()
                return response
            except Exception as e:
                last_exc = e
        if last_exc:
            raise last_exc
        raise RuntimeError("Request failed")

    def fetch_active_markets(self, limit: int = 50) -> List[Market]:
        try:
            response = self._request_get(
                f"{self.BASE_URL}/markets",
                params={
                    "limit": limit,
                    "active": "true",
                    "order": "volume",
                    "closed": "false"
                }
            )
            data = response.json()
            
            markets = []
            for item in data.get('data', []):
                tokens = []
                if 'tokens' in item:
                    for t in item['tokens']:
                        tokens.append(Token(
                            token_id=t.get('token_id', ''),
                            outcome=t.get('outcome', ''),
                            price=float(t.get('price', 0.0) or 0.0),
                            winner=t.get('winner', False)
                        ))
                
                markets.append(Market(
                    condition_id=item.get('condition_id', ''),
                    question=item.get('question', ''),
                    tokens=tokens,
                    volume=float(item.get('volume', 0.0) or 0.0),
                    tags=item.get('tags') or [],
                    market_slug=item.get('slug', '')
                ))
            return markets
        except Exception as e:
            print(f"Error fetching markets: {e}")
            return []

    def fetch_closed_markets(self, limit: int = 10) -> List[Market]:
        try:
            response = self._request_get(
                f"{self.BASE_URL}/markets",
                params={
                    "limit": limit,
                    "active": "false",
                    "closed": "true",
                    "order": "volume",
                    "ascending": "false"
                }
            )
            data = response.json()
            
            markets = []
            for item in data.get('data', []):
                tokens = []
                if 'tokens' in item:
                    for t in item['tokens']:
                        tokens.append(Token(
                            token_id=t.get('token_id', ''),
                            outcome=t.get('outcome', ''),
                            price=float(t.get('price', 0.0) or 0.0),
                            winner=t.get('winner', False)
                        ))
                
                markets.append(Market(
                    condition_id=item.get('condition_id', ''),
                    question=item.get('question', ''),
                    tokens=tokens,
                    volume=float(item.get('volume', 0.0) or 0.0),
                    tags=item.get('tags') or [],
                    market_slug=item.get('slug', '')
                ))
            return markets
        except Exception as e:
            print(f"Error fetching closed markets: {e}")
            return []

    def get_market_prices(self, market_id: str):
        # Implementation for detailed book fetching if needed
        pass
