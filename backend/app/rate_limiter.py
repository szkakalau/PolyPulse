"""
Rate limiting middleware for API endpoints
"""
import time
from typing import Dict, Tuple
from threading import Lock
import redis
import os
from fastapi import Request

class RateLimiter:
    def __init__(self):
        self.redis_client = redis.Redis.from_url(os.environ["REDIS_URL"], decode_responses=True) \
            if os.environ.get("REDIS_URL") else None
        self._local = {}
        self._lock = Lock()
        
    def is_rate_limited(self, key: str, limit: int, window: int) -> bool:
        """
        Check if request should be rate limited
        
        Args:
            key: Unique identifier for rate limiting (e.g., user_id:endpoint)
            limit: Maximum number of requests allowed in window
            window: Time window in seconds
            
        Returns:
            bool: True if rate limited, False otherwise
        """
        current_time = int(time.time())
        window_start = current_time - window
        if not self.redis_client:
            with self._lock:
                items = self._local.get(key, [])
                items = [ts for ts in items if ts > window_start]
                if len(items) >= limit:
                    self._local[key] = items
                    return True
                items.append(current_time)
                self._local[key] = items
            return False

        self.redis_client.zremrangebyscore(key, 0, window_start)
        request_count = self.redis_client.zcard(key)
        if request_count >= limit:
            return True
        self.redis_client.zadd(key, {str(current_time): current_time})
        self.redis_client.expire(key, window)
        return False
    
    def get_rate_limit_headers(self, key: str, limit: int, window: int) -> Dict[str, str]:
        """
        Get rate limit headers for response
        """
        current_time = int(time.time())
        window_start = current_time - window
        if not self.redis_client:
            with self._lock:
                items = self._local.get(key, [])
                items = [ts for ts in items if ts > window_start]
                self._local[key] = items
                request_count = len(items)
            return {
                "X-RateLimit-Limit": str(limit),
                "X-RateLimit-Remaining": str(max(0, limit - request_count)),
                "X-RateLimit-Reset": str(current_time + window)
            }

        self.redis_client.zremrangebyscore(key, 0, window_start)
        request_count = self.redis_client.zcard(key)
        return {
            "X-RateLimit-Limit": str(limit),
            "X-RateLimit-Remaining": str(max(0, limit - request_count)),
            "X-RateLimit-Reset": str(current_time + window)
        }

# Global rate limiter instance
rate_limiter = RateLimiter()

def rate_limit_middleware(limit: int = 100, window: int = 60):
    """
    Decorator for rate limiting endpoints
    """
    def decorator(func):
        async def wrapper(request: Request, *args, **kwargs):
            # Use client IP as rate limit key
            client_ip = request.client.host
            endpoint = request.url.path
            rate_limit_key = f"rate_limit:{client_ip}:{endpoint}"
            
            if rate_limiter.is_rate_limited(rate_limit_key, limit, window):
                headers = rate_limiter.get_rate_limit_headers(rate_limit_key, limit, window)
                return {
                    "error": "rate_limit_exceeded",
                    "message": "Too many requests. Please try again later.",
                    "retry_after": window
                }, 429, headers
            
            # Add rate limit headers to response
            response = await func(request, *args, **kwargs)
            headers = rate_limiter.get_rate_limit_headers(rate_limit_key, limit, window)
            
            if isinstance(response, tuple) and len(response) == 3:
                # Response already has status and headers
                existing_headers = response[2] or {}
                existing_headers.update(headers)
                return response[0], response[1], existing_headers
            elif isinstance(response, dict):
                # Convert to tuple with headers
                return response, 200, headers
            else:
                # For other response types, add headers
                response.headers.update(headers)
                return response
                
        return wrapper
    return decorator
