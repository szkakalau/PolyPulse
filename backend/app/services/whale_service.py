import requests
import logging
from typing import List, Dict, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from app.services.market_service import MarketService
from datetime import datetime
from app.database import save_whale_trade, get_leaderboard_stats

logger = logging.getLogger(__name__)

class WhaleService:
    DATA_API_URL = "https://data-api.polymarket.com/trades"
    
    def __init__(self):
        self.market_service = MarketService()

    def fetch_whale_activity(self, min_value_usd: float = 1000.0, limit: int = 20) -> List[Dict]:
        """
        Fetches recent large trades (Whale Activity) from top active markets.
        """
        markets = self.market_service.fetch_active_markets(limit=20)
        all_whales = []

        with ThreadPoolExecutor(max_workers=5) as executor:
            future_to_market = {}
            for market in markets:
                for token in market.tokens:
                     # We need to pass the market object to associate trade with market info
                     future_to_market[executor.submit(self._fetch_trades_for_token, token.token_id)] = (market, token)

            for future in as_completed(future_to_market):
                market, token = future_to_market[future]
                try:
                    trades = future.result()
                    for trade in trades:
                        try:
                            size = float(trade.get('size', 0))
                            price = float(trade.get('price', 0))
                            value_usd = size * price
                            
                            if value_usd >= min_value_usd:
                                all_whales.append({
                                    "market_question": market.question,
                                    "outcome": token.outcome,
                                    "side": trade.get('side', 'BUY'),
                                    "size": size,
                                    "price": price,
                                    "value_usd": value_usd,
                                    "timestamp": trade.get('timestamp'),
                                    "maker_address": trade.get('maker_address', ''),
                                    "market_slug": market.market_slug
                                })
                        except Exception:
                            continue
                except Exception as e:
                    logger.error(f"Error processing trades for {market.question}: {e}")

        # Sort by timestamp desc
        all_whales.sort(key=lambda x: x['timestamp'], reverse=True)
        return all_whales[:limit]

    def analyze_smart_money(self):
        """
        Analyzes closed markets to identify winners and fetch their trades.
        """
        try:
            markets = self.market_service.fetch_closed_markets(limit=10)
            
            for m in markets:
                for t in m.tokens:
                    if t.winner:
                        # Try to fetch trades for this winner using asset param
                        trades = self._fetch_trades_for_token(t.token_id, use_asset_param=True)
                        for trade in trades:
                            try:
                                size = float(trade.get('size', 0))
                                price = float(trade.get('price', 0))
                                value_usd = size * price
                                
                                if value_usd >= 1000: # Whale threshold
                                    whale_trade = {
                                        "market_question": m.question,
                                        "outcome": t.outcome,
                                        "side": trade.get('side', 'BUY'),
                                        "size": size,
                                        "price": price,
                                        "value_usd": value_usd,
                                        "timestamp": trade.get('timestamp'),
                                        "maker_address": trade.get('proxyWallet') or trade.get('maker_address', ''),
                                        "market_slug": m.market_slug
                                    }
                                    save_whale_trade(whale_trade)
                            except Exception:
                                continue
        except Exception as e:
            logger.error(f"Error analyzing smart money: {e}")

    def _fetch_trades_for_token(self, token_id: str, use_asset_param: bool = False) -> List[Dict]:
        try:
            params = {"asset": token_id} if use_asset_param else {"market": token_id}
            response = requests.get(
                self.DATA_API_URL,
                params=params,
                proxies={"http": None, "https": None},
                timeout=5,
                headers={"User-Agent": "Mozilla/5.0"}
            )
            if response.status_code == 200:
                return response.json()
        except Exception as e:
            logger.warning(f"Failed to fetch trades for token {token_id}: {e}")
        return []

    def get_leaderboard(self, limit: int = 10) -> List[Dict]:
        """
        Get top whale traders by volume.
        """
        return get_leaderboard_stats(limit)
