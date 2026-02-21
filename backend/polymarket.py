import os
import requests
from typing import Dict, List, Optional

GAMMA_API = "https://gamma-api.polymarket.com"
TRADES_API = "https://data-api.polymarket.com/trades"


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


def fetch_markets(limit: int = 50) -> List[Dict]:
    return _get_json(
        f"{GAMMA_API}/markets",
        params={
            "limit": limit,
            "active": "true",
            "closed": "false",
            "order": "volume"
        }
    )


def fetch_closed_markets(limit: int = 20) -> List[Dict]:
    return _get_json(
        f"{GAMMA_API}/markets",
        params={
            "limit": limit,
            "active": "false",
            "closed": "true",
            "order": "volume"
        }
    )


def fetch_trades_for_market(market_id: str, limit: int = 200) -> List[Dict]:
    return _get_json(
        TRADES_API,
        params={"market": market_id, "limit": limit}
    )


def fetch_trades_for_token(token_id: str, limit: int = 200) -> List[Dict]:
    return _get_json(
        TRADES_API,
        params={"asset": token_id, "limit": limit}
    )
