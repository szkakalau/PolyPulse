from collections import defaultdict
from typing import Dict, List, Tuple
from datetime import datetime, timedelta, timezone
import logging
from app.cache import cache

logger = logging.getLogger(__name__)

class EnhancedSmartMoneyAnalyzer:
    def __init__(self):
        self.min_trades_threshold = 5
        self.min_volume_threshold = 10000.0
        self.recency_weight = 0.7  # Weight for recent trades
    
    def calculate_enhanced_metrics(self, trades: List[Dict]) -> List[Dict]:
        """Calculate enhanced smart money metrics with recency weighting"""
        
        # Group trades by address
        address_data = defaultdict(lambda: {
            'trades': [],
            'total_volume': 0.0,
            'recent_volume': 0.0,
            'win_count': 0,
            'recent_win_count': 0
        })
        
        current_time = datetime.now(timezone.utc).replace(tzinfo=None)
        recent_cutoff = current_time - timedelta(hours=24)
        
        for trade in trades:
            address = trade.get('address', '')
            if not address:
                continue
                
            trade_time = trade.get('timestamp')
            if isinstance(trade_time, str):
                try:
                    trade_time = datetime.fromisoformat(trade_time.replace('Z', '+00:00'))
                except (ValueError, AttributeError):
                    trade_time = current_time
            if isinstance(trade_time, datetime) and trade_time.tzinfo is not None:
                trade_time = trade_time.astimezone(timezone.utc).replace(tzinfo=None)
            
            value = float(trade.get('value', 0))
            is_win = trade.get('is_winning', False)
            
            address_data[address]['trades'].append(trade)
            address_data[address]['total_volume'] += value
            
            if trade_time >= recent_cutoff:
                address_data[address]['recent_volume'] += value
                if is_win:
                    address_data[address]['recent_win_count'] += 1
            
            if is_win:
                address_data[address]['win_count'] += 1
        
        # Calculate enhanced metrics
        enhanced_results = []
        
        for address, data in address_data.items():
            total_trades = len(data['trades'])
            if total_trades < self.min_trades_threshold:
                continue
                
            if data['total_volume'] < self.min_volume_threshold:
                continue
            
            total_win_rate = data['win_count'] / total_trades if total_trades > 0 else 0
            
            # Calculate recent performance
            recent_trades = []
            for t in data['trades']:
                ts_value = t.get('timestamp', current_time.isoformat())
                try:
                    trade_ts = datetime.fromisoformat(ts_value.replace('Z', '+00:00'))
                except Exception:
                    trade_ts = current_time
                if trade_ts.tzinfo is not None:
                    trade_ts = trade_ts.astimezone(timezone.utc).replace(tzinfo=None)
                if trade_ts >= recent_cutoff:
                    recent_trades.append(t)
            recent_trade_count = len(recent_trades)
            
            if recent_trade_count > 0:
                recent_win_rate = data['recent_win_count'] / recent_trade_count
            else:
                recent_win_rate = total_win_rate
            
            # Combined score with recency weighting
            combined_win_rate = (self.recency_weight * recent_win_rate + 
                              (1 - self.recency_weight) * total_win_rate)
            
            # Volume consistency score
            if data['recent_volume'] > 0 and data['total_volume'] > 0:
                volume_consistency = min(data['recent_volume'] / data['total_volume'] * 5, 1.0)
            else:
                volume_consistency = 0.5
            
            # Final confidence score
            confidence_score = combined_win_rate * volume_consistency
            
            enhanced_results.append({
                'address': address,
                'total_trades': total_trades,
                'total_volume': data['total_volume'],
                'recent_volume': data['recent_volume'],
                'win_rate': total_win_rate,
                'recent_win_rate': recent_win_rate,
                'combined_win_rate': combined_win_rate,
                'confidence_score': confidence_score,
                'win_count': data['win_count'],
                'recent_win_count': data['recent_win_count']
            })
        
        # Sort by confidence score descending
        enhanced_results.sort(key=lambda x: x['confidence_score'], reverse=True)
        
        return enhanced_results
    
    def analyze_market_specific_performance(self, trades: List[Dict], market_tags: List[str] = None):
        """Analyze performance for specific market types"""
        if not trades:
            return []
        
        # Filter by market tags if provided
        if market_tags:
            filtered_trades = [
                t for t in trades 
                if any(tag in t.get('market_tags', []) for tag in market_tags)
            ]
        else:
            filtered_trades = trades
        
        return self.calculate_enhanced_metrics(filtered_trades)
    
    def get_performance_trend(self, address: str, trades: List[Dict]) -> Dict:
        """Get performance trend for a specific address"""
        address_trades = [t for t in trades if t.get('address') == address]
        if not address_trades:
            return {}
        
        # Group by time periods
        current_time = datetime.now(timezone.utc).replace(tzinfo=None)
        time_periods = {
            '24h': current_time - timedelta(hours=24),
            '7d': current_time - timedelta(days=7),
            '30d': current_time - timedelta(days=30)
        }
        
        trend_data = {}
        
        for period, cutoff in time_periods.items():
            period_trades = []
            for t in address_trades:
                ts_value = t.get('timestamp', current_time.isoformat())
                try:
                    trade_ts = datetime.fromisoformat(ts_value.replace('Z', '+00:00'))
                except Exception:
                    trade_ts = current_time
                if trade_ts.tzinfo is not None:
                    trade_ts = trade_ts.astimezone(timezone.utc).replace(tzinfo=None)
                if trade_ts >= cutoff:
                    period_trades.append(t)
            
            if period_trades:
                win_count = sum(1 for t in period_trades if t.get('is_winning', False))
                total_volume = sum(float(t.get('value', 0)) for t in period_trades)
                win_rate = win_count / len(period_trades) if period_trades else 0
                
                trend_data[period] = {
                    'trades': len(period_trades),
                    'win_rate': win_rate,
                    'total_volume': total_volume,
                    'win_count': win_count
                }
        
        return trend_data

# Global analyzer instance
smart_money_analyzer = EnhancedSmartMoneyAnalyzer()
