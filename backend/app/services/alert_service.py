import logging
from typing import List, Dict
from app.models.market import Market
from datetime import datetime
from app.services.fcm_service import FCMService
from app.database import save_alert, get_recent_alerts, get_users_watching_market

logger = logging.getLogger(__name__)

class AlertService:
    def __init__(self):
        # In-memory storage for previous prices: {condition_id: {outcome: price}}
        self.price_cache: Dict[str, Dict[str, float]] = {}
        # Initialize FCM
        self.fcm_service = FCMService()
        
    def get_recent_alerts(self) -> List[Dict]:
        return get_recent_alerts(limit=50)

    def check_for_alerts(self, markets: List[Market]):
        new_alerts = []
        for market in markets:
            # 1. Check for Flips (Outcome probability change > 50%)
            # This is a simplified logic. Real logic needs history.
            
            # 2. Check for Significant Price Movements (> 5% change)
            if market.condition_id in self.price_cache:
                prev_prices = self.price_cache[market.condition_id]
                for token in market.tokens:
                    prev_price = prev_prices.get(token.outcome, 0.0)
                    curr_price = token.price
                    
                    # Detect change > 5% (0.05)
                    if abs(curr_price - prev_price) >= 0.05:
                        alert_obj = {
                            "timestamp": datetime.now().isoformat(),
                            "market_question": market.question,
                            "outcome": token.outcome,
                            "old_price": prev_price,
                            "new_price": curr_price,
                            "change": curr_price - prev_price,
                            "message": f"🚨 ALERT: {market.question} | {token.outcome} moved from {prev_price:.2f} to {curr_price:.2f}"
                        }
                        new_alerts.append(alert_obj)
                        logger.warning(alert_obj["message"]) # Log alert
                        
                        # Save to DB
                        save_alert(alert_obj)
                        
                        # Send Push Notification
                        # 1. Send to general 'alerts' topic (if broad alert)
                        # self.fcm_service.send_to_topic(...)
                        
                        # 2. Send to specific users watching this market
                        watching_tokens = get_users_watching_market(market.condition_id)
                        if watching_tokens:
                            self.fcm_service.send_multicast(
                                tokens=watching_tokens,
                                title="PolyPulse Watchlist Alert",
                                body=f"Watched Market: {market.question} | {token.outcome} changed to {curr_price:.2f}",
                                data={"marketId": market.id}
                            )
            
            # Update cache
            current_prices = {t.outcome: t.price for t in market.tokens}
            self.price_cache[market.condition_id] = current_prices
            
        # Keep last 50 alerts
        if new_alerts:
            self.recent_alerts.extend(new_alerts)
            self.recent_alerts = self.recent_alerts[-50:]
            
        return new_alerts
