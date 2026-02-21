from datetime import datetime
from typing import Dict, List


def normalize_trade(market: Dict, trade: Dict) -> Dict:
    price = float(trade.get("price", 0) or 0)
    size = float(trade.get("size", 0) or 0)
    value = price * size
    address = trade.get("maker_address") or trade.get("makerAddress") or trade.get("proxyWallet") or ""
    side = trade.get("side") or "BUY"
    trade_id = str(trade.get("id") or trade.get("trade_id") or trade.get("tx_hash") or "")
    timestamp_raw = trade.get("timestamp") or trade.get("time") or trade.get("createdAt")
    timestamp = _parse_timestamp(timestamp_raw)

    market_id = (
        market.get("id")
        or market.get("conditionId")
        or market.get("condition_id")
        or market.get("slug")
        or ""
    )
    question = market.get("question") or market.get("title") or ""

    if not trade_id:
        trade_id = f"{market_id}-{address}-{timestamp.timestamp() if timestamp else 0}-{value}"

    return {
        "id": trade_id,
        "market": market_id,
        "question": question,
        "address": address,
        "side": side,
        "price": price,
        "size": size,
        "value": value,
        "timestamp": timestamp
    }


def detect_whales(trades: List[Dict], min_value: float = 1000) -> List[Dict]:
    whales = []
    for trade in trades:
        if trade["value"] >= min_value:
            whales.append({
                "trade_id": trade["id"],
                "address": trade["address"],
                "value": trade["value"],
                "timestamp": trade["timestamp"]
            })
    return whales


def _parse_timestamp(value) -> datetime:
    if isinstance(value, (int, float)):
        return datetime.utcfromtimestamp(value)
    if isinstance(value, str):
        try:
            if value.isdigit():
                return datetime.utcfromtimestamp(int(value))
            return datetime.fromisoformat(value.replace("Z", "+00:00"))
        except Exception:
            pass
    return datetime.utcnow()
