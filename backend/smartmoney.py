from collections import defaultdict
from typing import Dict, List
from sqlalchemy import func
from sqlalchemy.dialects.sqlite import insert
from models import Trade, SmartWallet


def update_smart_wallets(session, winning_trades: List[Dict]):
    totals = session.query(
        Trade.address,
        func.count(Trade.id).label("total_trades"),
        func.coalesce(func.sum(Trade.value), 0).label("total_value")
    ).group_by(Trade.address).all()

    total_map = {row.address: row for row in totals}

    win_count = defaultdict(int)
    win_value = defaultdict(float)
    for trade in winning_trades:
        address = trade.get("address") or ""
        if not address:
            continue
        value = float(trade.get("value") or 0)
        win_count[address] += 1
        win_value[address] += value

    rows = []
    for address, row in total_map.items():
        total_trades = int(row.total_trades or 0)
        wins = int(win_count.get(address, 0))
        profit = float(win_value.get(address, 0))
        win_rate = (wins / total_trades) if total_trades else 0
        total_value = float(row.total_value or 0)
        roi = (profit / total_value) if total_value else 0
        rows.append({
            "address": address,
            "profit": profit,
            "roi": roi,
            "win_rate": win_rate,
            "total_trades": total_trades
        })

    if not rows:
        return

    stmt = insert(SmartWallet).values(rows)
    stmt = stmt.on_conflict_do_update(
        index_elements=[SmartWallet.address],
        set_={
            "profit": stmt.excluded.profit,
            "roi": stmt.excluded.roi,
            "win_rate": stmt.excluded.win_rate,
            "total_trades": stmt.excluded.total_trades
        }
    )
    session.execute(stmt)
