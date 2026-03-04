import redis
import json
import os
from typing import Optional, Any
import logging

logger = logging.getLogger(__name__)

class RedisCache:
    def __init__(self, host='localhost', port=6379, db=0, password=None):
        try:
            self.redis_client = redis.Redis(
                host=host, port=port, db=db, password=password,
                decode_responses=True, socket_timeout=5, socket_connect_timeout=5
            )
            self.redis_client.ping()
            logger.info("Redis cache connected successfully")
        except Exception as e:
            logger.warning(f"Redis cache not available: {e}")
            self.redis_client = None

    def get(self, key: str) -> Optional[Any]:
        if not self.redis_client:
            return None
        try:
            cached = self.redis_client.get(key)
            return json.loads(cached) if cached else None
        except Exception as e:
            logger.error(f"Cache get error for key {key}: {e}")
            return None

    def set(self, key: str, value: Any, ttl_seconds: int = 300) -> bool:
        if not self.redis_client:
            return False
        try:
            serialized = json.dumps(value)
            self.redis_client.setex(key, ttl_seconds, serialized)
            return True
        except Exception as e:
            logger.error(f"Cache set error for key {key}: {e}")
            return False

    def delete(self, key: str) -> bool:
        if not self.redis_client:
            return False
        try:
            self.redis_client.delete(key)
            return True
        except Exception as e:
            logger.error(f"Cache delete error for key {key}: {e}")
            return False

    def clear_pattern(self, pattern: str) -> int:
        if not self.redis_client:
            return 0
        try:
            keys = self.redis_client.keys(pattern)
            if keys:
                self.redis_client.delete(*keys)
            return len(keys)
        except Exception as e:
            logger.error(f"Cache clear error for pattern {pattern}: {e}")
            return 0

# Global cache instance
cache = RedisCache(
    host=os.environ.get('REDIS_HOST', 'localhost'),
    port=int(os.environ.get('REDIS_PORT', 6379)),
    db=int(os.environ.get('REDIS_DB', 0)),
    password=os.environ.get('REDIS_PASSWORD')
)
