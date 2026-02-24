import os
import requests
from typing import Dict, List, Optional
from datetime import datetime
from database import get_session
from models import Market as DbMarket, Trade as DbTrade

GAMMA_API = "https://gamma-api.polymarket.com"
TRADES_API = "https://data-api.polymarket.com/trades"

MOCK_MARKETS = [
    {
        "id": "mock-market-1",
        "question": "Will BTC trade above $100,000 this month?",
        "volume": 150000,
        "liquidity": 30000,
        "tokens": [
            {"token_id": "mock-token-yes", "outcome": "Yes", "winner": True},
            {"token_id": "mock-token-no", "outcome": "No", "winner": False}
        ],
        "lastTradePrice": 0.62,
        "bestAsk": 0.63,
        "bestBid": 0.61,
        "volume24hr": 12000,
        "updatedAt": datetime.utcnow().isoformat()
    },
    {
        "id": "mock-market-2",
        "question": "Will ETH ETF be approved in 2025?",
        "volume": 95000,
        "liquidity": 22000,
        "tokens": [
            {"token_id": "mock2-token-yes", "outcome": "Yes", "winner": False},
            {"token_id": "mock2-token-no", "outcome": "No", "winner": True}
        ],
        "lastTradePrice": 0.41,
        "bestAsk": 0.42,
        "bestBid": 0.40,
        "volume24hr": 8000,
        "updatedAt": datetime.utcnow().isoformat()
    }
]


def _request_get(url: str, params: Optional[Dict] = None) -> requests.Response:
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
                params=params or {},
                timeout=10,
                headers={"User-Agent": "Mozilla/5.0"}
            )
            response.raise_for_status()
            return response
        except Exception as e:
            last_exc = e
    if last_exc:
        raise last_exc
    raise RuntimeError("Request failed")


def _get_json(url: str, params: Optional[Dict] = None) -> List[Dict]:
    response = _request_get(url, params=params)
    data = response.json()
    if isinstance(data, dict) and "data" in data:
        return data["data"] or []
    if isinstance(data, list):
        return data
    return []


def _cached_markets(limit: int) -> List[Dict]:
    session = get_session()
    try:
        rows = (
            session.query(DbMarket)
            .order_by(DbMarket.volume.desc())
            .limit(limit)
            .all()
        )
        return [
            {
                "id": row.id,
                "question": row.question,
                "volume": row.volume or 0,
                "liquidity": row.liquidity or 0
            }
            for row in rows
        ]
    except Exception:
        return []
    finally:
        session.close()


def _cached_trades_by_market(market_id: str, limit: int) -> List[Dict]:
    session = get_session()
    try:
        rows = (
            session.query(DbTrade)
            .filter(DbTrade.market == market_id)
            .order_by(DbTrade.timestamp.desc())
            .limit(limit)
            .all()
        )
        return [
            {
                "id": row.id,
                "price": row.price,
                "size": row.size,
                "side": row.side,
                "maker_address": row.address,
                "timestamp": row.timestamp.isoformat() if row.timestamp else None
            }
            for row in rows
        ]
    except Exception:
        return []
    finally:
        session.close()


def _mock_markets(limit: int) -> List[Dict]:
    return MOCK_MARKETS[:limit]


def _mock_trades(limit: int, market_id: str, token_id: Optional[str] = None) -> List[Dict]:
    now = datetime.utcnow()
    base_price = 0.6 if token_id and token_id.endswith("yes") else 0.4
    trades = []
    for idx in range(min(limit, 5)):
        trades.append({
            "id": f"mock-{market_id}-{token_id or 'market'}-{idx}",
            "price": base_price + (idx * 0.01),
            "size": 50 + (idx * 10),
            "side": "BUY" if idx % 2 == 0 else "SELL",
            "maker_address": f"0xmock{idx:02d}",
            "timestamp": now.isoformat()
        })
    return trades


def fetch_markets(limit: int = 50) -> List[Dict]:
    mode = os.getenv("POLYMARKET_DATA_MODE", "live").lower()
    cache_fallback = os.getenv("POLYMARKET_CACHE_FALLBACK", "1") == "1"
    if mode == "mock":
        return _mock_markets(limit)
    if mode == "cache":
        return _cached_markets(limit)
    try:
        return _get_json(
            f"{GAMMA_API}/markets",
            params={
                "limit": limit,
                "active": "true",
                "closed": "false",
                "order": "volume"
            }
        )
    except Exception:
        return _cached_markets(limit) if cache_fallback else []


def fetch_closed_markets(limit: int = 20) -> List[Dict]:
    mode = os.getenv("POLYMARKET_DATA_MODE", "live").lower()
    cache_fallback = os.getenv("POLYMARKET_CACHE_FALLBACK", "1") == "1"
    if mode == "mock":
        return _mock_markets(limit)
    if mode == "cache":
        return _cached_markets(limit)
    try:
        return _get_json(
            f"{GAMMA_API}/markets",
            params={
                "limit": limit,
                "active": "false",
                "closed": "true",
                "order": "volume"
            }
        )
    except Exception:
        return _cached_markets(limit) if cache_fallback else []


def fetch_trades_for_market(market_id: str, limit: int = 200) -> List[Dict]:
    mode = os.getenv("POLYMARKET_DATA_MODE", "live").lower()
    cache_fallback = os.getenv("POLYMARKET_CACHE_FALLBACK", "1") == "1"
    if mode == "mock":
        return _mock_trades(limit, market_id)
    if mode == "cache":
        return _cached_trades_by_market(market_id, limit)
    try:
        return _get_json(
            TRADES_API,
            params={"market": market_id, "limit": limit}
        )
    except Exception:
        return _cached_trades_by_market(market_id, limit) if cache_fallback else []


def fetch_trades_for_token(token_id: str, limit: int = 200) -> List[Dict]:
    mode = os.getenv("POLYMARKET_DATA_MODE", "live").lower()
    cache_fallback = os.getenv("POLYMARKET_CACHE_FALLBACK", "1") == "1"
    if mode == "mock":
        return _mock_trades(limit, token_id, token_id=token_id)
    if mode == "cache":
        return []
    try:
        return _get_json(
            TRADES_API,
            params={"asset": token_id, "limit": limit}
        )
    except Exception:
        return [] if not cache_fallback else []
