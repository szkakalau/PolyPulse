import time
from functools import wraps
from typing import Dict, Any, Callable
import logging
from app.cache import cache

logger = logging.getLogger(__name__)

class MetricsCollector:
    def __init__(self):
        self.metrics = {}
    
    def track_metrics(self, endpoint: str):
        def decorator(func: Callable):
            @wraps(func)
            async def wrapper(*args, **kwargs):
                start_time = time.time()
                try:
                    result = await func(*args, **kwargs)
                    duration = (time.time() - start_time) * 1000  # ms
                    
                    # Store metrics in cache
                    metrics_key = f"metrics:{endpoint}"
                    current_metrics = cache.get(metrics_key) or {
                        'count': 0, 'total_time': 0, 'avg_time': 0, 'errors': 0
                    }
                    
                    current_metrics['count'] += 1
                    current_metrics['total_time'] += duration
                    current_metrics['avg_time'] = current_metrics['total_time'] / current_metrics['count']
                    
                    cache.set(metrics_key, current_metrics, ttl_seconds=3600)
                    
                    return result
                except Exception as e:
                    duration = (time.time() - start_time) * 1000
                    error_key = f"metrics:{endpoint}:errors"
                    error_count = cache.get(error_key) or 0
                    cache.set(error_key, error_count + 1, ttl_seconds=3600)
                    
                    logger.error(f"Endpoint {endpoint} failed in {duration:.2f}ms: {e}")
                    raise
            
            return wrapper
        return decorator
    
    def get_metrics(self) -> Dict[str, Any]:
        metrics = {}
        try:
            # Get all metric keys
            if cache.redis_client:
                metric_keys = cache.redis_client.keys("metrics:*")
                for key in metric_keys:
                    endpoint = key.replace("metrics:", "")
                    data = cache.get(key)
                    if data:
                        metrics[endpoint] = data
        except Exception as e:
            logger.error(f"Failed to get metrics: {e}")
        
        return metrics

# Global metrics collector
metrics_collector = MetricsCollector()