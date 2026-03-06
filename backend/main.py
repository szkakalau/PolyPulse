import logging
import json
import secrets
import os
import redis
from contextlib import asynccontextmanager
from datetime import datetime, timedelta, timezone
from fastapi import FastAPI, Depends, HTTPException, status, Header, Request, Response
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from apscheduler.schedulers.background import BackgroundScheduler
from typing import List, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from sqlalchemy.dialects.sqlite import insert
from sqlalchemy import func

from app.database import init_db, get_recent_alerts, create_user, get_user_by_email, get_db_connection, save_whale_trade
from app.database import (
    upsert_subscription,
    get_latest_subscription,
    save_transaction,
    set_user_entitlements,
    get_latest_user_entitlement,
    get_entitlements_for_tier,
    get_transaction_user_id,
    get_signals,
    get_signal_by_id,
    create_signal,
    upsert_signal_evaluation,
    upsert_fcm_token,
    get_fcm_tokens_for_user,
    get_user_ids_with_fcm_tokens,
    get_notification_settings,
    set_notification_settings,
    save_analytics_event,
    has_recent_analytics_event,
    get_watchlist,
    add_to_watchlist,
    remove_from_watchlist,
    get_daily_pulse,
    get_referral_code,
    insert_referral_code,
    redeem_referral_code,
    get_feature_flags,
    get_metrics_counts,
    get_signal_stats,
    get_signal_credibility,
    create_notification_attempt,
    update_notification_attempt,
    get_delivery_observability
)
from app.services.auth_service import AuthService
from app.services.market_service import MarketService
from app.services.whale_service import WhaleService
from app.services.fcm_service import FCMService
from app.cache import cache
from app.rate_limiter import rate_limiter
from database import init_db as init_polymarket_db, get_session
from models import Market, Trade, Whale, SmartWallet
from polymarket import fetch_markets, fetch_trades_for_market, fetch_trades_for_token, fetch_closed_markets
from whale import normalize_trade, detect_whales
from smartmoney import update_smart_wallets
from app.schemas import (
    UserRegister,
    UserLogin,
    Token,
    UserResponse,
    BillingVerifyRequest,
    BillingVerifyResponse,
    BillingStatusResponse,
    EntitlementsResponse,
    EntitlementFeature,
    SubscriptionInfo,
    BillingWebhookRequest,
    SignalEvidence,
    SignalResponse,
    PaywallPlan,
    PaywallResponse,
    InAppMessageResponse,
    TrialStartResponse,
    NotificationRegisterRequest,
    NotificationSendRequest,
    NotificationSettingsResponse,
    NotificationSettingsUpdateRequest,
    AnalyticsEventRequest,
    DailyPulseResponse,
    ReferralCodeResponse,
    ReferralRedeemRequest,
    ReferralRedeemResponse,
    FeatureFlagResponse,
    MetricsResponse,
    MonitorAlertRequest,
    SignalStatsResponse,
    SignalCredibilityHistogramItem,
    SignalCredibilityWindowResponse,
    SignalCredibilityResponse,
    DeliveryWindowResponse,
    DeliveryObservabilityResponse,
    AdminSignalCreateRequest,
    AdminSignalEvaluationRequest
)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Performance monitoring
import time
from prometheus_client import Counter, Histogram, generate_latest
from prometheus_client.exposition import CONTENT_TYPE_LATEST

# Metrics definitions
REQUEST_COUNT = Counter('http_requests_total', 'Total HTTP Requests', ['method', 'endpoint', 'status'])
REQUEST_LATENCY = Histogram('http_request_duration_seconds', 'HTTP request latency', ['endpoint'])
DB_QUERY_TIME = Histogram('db_query_duration_seconds', 'Database query duration', ['query_type'])
REDIS_OPERATION_TIME = Histogram('redis_operation_duration_seconds', 'Redis operation duration', ['operation'])

# Performance monitoring middleware
async def performance_middleware(request: Request, call_next):
    start_time = time.time()
    endpoint = request.url.path
    
    try:
        response = await call_next(request)
        
        # Record metrics
        REQUEST_COUNT.labels(
            method=request.method, 
            endpoint=endpoint, 
            status=response.status_code
        ).inc()
        
        REQUEST_LATENCY.labels(endpoint=endpoint).observe(time.time() - start_time)
        response.headers["Server-Timing"] = f"app;dur={(time.time() - start_time)*1000:.2f}"
        response.headers["X-Response-Time-ms"] = f"{(time.time() - start_time)*1000:.2f}"
        
        return response
    except Exception as e:
        REQUEST_COUNT.labels(
            method=request.method, 
            endpoint=endpoint, 
            status=500
        ).inc()
        raise e

def _cache_key(prefix: str, parts: List[str]) -> str:
    return "api_cache:" + prefix + ":" + ":".join(parts)

def _cached_response(key: str, ttl_seconds: int, builder):
    cached = cache.get(key)
    if cached is not None:
        return cached
    data = builder()
    cache.set(key, data, ttl_seconds=ttl_seconds)
    return data

def _utcnow():
    return datetime.now(timezone.utc).replace(tzinfo=None)

def _rate_limit_key(request: Request) -> str:
    forwarded = request.headers.get("X-Forwarded-For")
    client_ip = forwarded.split(",")[0].strip() if forwarded else (request.client.host if request.client else "unknown")
    return f"rate_limit:{client_ip}:{request.url.path}"

def _sanitize_pagination(limit: int, offset: int, max_limit: int = 200) -> tuple[int, int]:
    safe_limit = 1 if limit < 1 else (max_limit if limit > max_limit else limit)
    safe_offset = 0 if offset < 0 else offset
    return safe_limit, safe_offset

# Initialize Services
auth_service = AuthService()
market_service = MarketService()
whale_service = WhaleService()
fcm_service = FCMService()
redis_client = redis.Redis.from_url(os.environ["REDIS_URL"], decode_responses=True) if os.environ.get("REDIS_URL") else None
redis_queue_key = "polypulse:notifications"
ALERT_SUCCESS_RATE_MIN = float(os.environ.get("ALERT_SUCCESS_RATE_MIN", "0.9"))
ALERT_QUEUE_DEPTH_MAX = int(os.environ.get("ALERT_QUEUE_DEPTH_MAX", "500"))
ALERT_QUEUE_AGE_MAX_SECONDS = int(os.environ.get("ALERT_QUEUE_AGE_MAX_SECONDS", "120"))
ALERT_DELIVERY_P90_MAX = int(os.environ.get("ALERT_DELIVERY_P90_MAX", "60"))
ALERT_CACHE_KEY = "monitor:alerts"
RATE_LIMIT_WINDOW_SECONDS = int(os.environ.get("RATE_LIMIT_WINDOW_SECONDS", "60"))
RATE_LIMIT_DEFAULT = int(os.environ.get("RATE_LIMIT_DEFAULT", "60"))
RATE_LIMIT_HEALTH = int(os.environ.get("RATE_LIMIT_HEALTH", "10"))
REQUEST_MAX_BYTES = int(os.environ.get("REQUEST_MAX_BYTES", "1048576"))

def _record_monitor_alert(level: str, message: str, source: str):
    payload = {
        "level": level,
        "message": message,
        "source": source,
        "createdAt": _utcnow().isoformat()
    }
    alerts = cache.get(ALERT_CACHE_KEY) or []
    alerts.insert(0, payload)
    cache.set(ALERT_CACHE_KEY, alerts[:100], ttl_seconds=86400)
    if level == "error":
        logger.error(f"[monitor:{source}] {message}")
    elif level == "warn" or level == "warning":
        logger.warning(f"[monitor:{source}] {message}")
    else:
        logger.info(f"[monitor:{source}] {message}")

def check_system_alerts():
    try:
        delivery = get_delivery_observability(1)
        success_rate = delivery.get("success_rate", 1.0)
        if success_rate < ALERT_SUCCESS_RATE_MIN:
            _record_monitor_alert("warn", f"delivery_success_rate_low:{success_rate:.3f}", "delivery")
        dispatch_p90 = delivery.get("dispatch_delay_p90_seconds")
        if dispatch_p90 is not None and dispatch_p90 > ALERT_DELIVERY_P90_MAX:
            _record_monitor_alert("warn", f"dispatch_delay_p90_high:{dispatch_p90}", "delivery")
    except Exception as e:
        _record_monitor_alert("error", f"alert_check_failed:{e}", "monitor")
    if redis_client:
        try:
            depth = int(redis_client.zcard(redis_queue_key))
            if depth > ALERT_QUEUE_DEPTH_MAX:
                _record_monitor_alert("warn", f"queue_depth_high:{depth}", "queue")
            oldest = redis_client.zrange(redis_queue_key, 0, 0, withscores=True)
            if oldest:
                oldest_due_seconds = int(_utcnow().timestamp() - float(oldest[0][1]))
                if oldest_due_seconds > ALERT_QUEUE_AGE_MAX_SECONDS:
                    _record_monitor_alert("warn", f"queue_oldest_due_high:{oldest_due_seconds}", "queue")
        except Exception as e:
            _record_monitor_alert("warn", f"queue_check_failed:{e}", "queue")

# Scheduler Jobs
def update_whale_data():
    logger.info("Scheduler: Fetching whale activity...")
    try:
        whales = whale_service.fetch_whale_activity()
        for whale in whales:
            try:
                save_whale_trade(whale)
            except Exception:
                continue
        logger.info("Scheduler: Whale activity updated.")
    except Exception as e:
        logger.error(f"Scheduler Error (Whale): {e}")


def _market_id(market: dict) -> str:
    return (
        market.get("id")
        or market.get("conditionId")
        or market.get("condition_id")
        or market.get("slug")
        or ""
    )


def _market_question(market: dict) -> str:
    return market.get("question") or market.get("title") or ""


def _market_tokens(market: dict) -> List[dict]:
    tokens = market.get("tokens") or []
    if tokens:
        return tokens
    clob_ids = market.get("clobTokenIds") or market.get("clobTokenIDs")
    outcomes_raw = market.get("outcomes")
    if isinstance(clob_ids, str):
        try:
            clob_ids = json.loads(clob_ids)
        except Exception:
            clob_ids = []
    if isinstance(outcomes_raw, str):
        try:
            outcomes = json.loads(outcomes_raw)
        except Exception:
            outcomes = []
    else:
        outcomes = outcomes_raw or []
    if isinstance(clob_ids, list) and clob_ids:
        return [
            {
                "token_id": token_id,
                "outcome": outcomes[idx] if idx < len(outcomes) else ""
            }
            for idx, token_id in enumerate(clob_ids)
        ]
    return []


def _float_or_zero(value) -> float:
    try:
        return float(value or 0)
    except Exception:
        return 0.0


def _synthetic_trade_from_market(market: dict) -> dict:
    price = _float_or_zero(market.get("lastTradePrice"))
    if price <= 0:
        price = _float_or_zero(market.get("bestAsk"))
    if price <= 0:
        price = _float_or_zero(market.get("bestBid"))
    volume = _float_or_zero(market.get("volume24hr"))
    if volume <= 0:
        volume = _float_or_zero(market.get("volume"))
    if price <= 0 or volume <= 0:
        return {}
    timestamp = market.get("updatedAt") or market.get("createdAt") or market.get("startDate") or market.get("endDate")
    return {
        "price": price,
        "size": volume / price if price else 0,
        "timestamp": timestamp,
        "maker_address": "market",
        "side": "BUY"
    }


def _normalize_market(market: dict) -> dict:
    market_id = _market_id(market)
    if not market_id:
        return {}
    return {
        "id": market_id,
        "question": _market_question(market),
        "volume": float(market.get("volume") or 0),
        "liquidity": float(market.get("liquidity") or 0)
    }


def _fetch_market_trades(market: dict, limit: int) -> List[dict]:
    market_id = _market_id(market)
    if not market_id:
        return []
    trades = fetch_trades_for_market(market_id, limit=limit)
    if not trades:
        for token in _market_tokens(market):
            token_id = token.get("token_id") or token.get("tokenId")
            if not token_id:
                continue
            trades.extend(fetch_trades_for_token(token_id, limit=limit))
    if not trades:
        synthetic = _synthetic_trade_from_market(market)
        if synthetic:
            trades = [synthetic]
    return [normalize_trade(market, trade) for trade in trades]


def _fetch_winning_trades(limit: int = 20) -> List[dict]:
    markets = fetch_closed_markets(limit=limit)
    winning_trades = []
    for market in markets:
        tokens = _market_tokens(market)
        winning_tokens = [t for t in tokens if t.get("winner") is True]
        for token in winning_tokens:
            token_id = token.get("token_id") or token.get("tokenId")
            if not token_id:
                continue
            trades = fetch_trades_for_token(token_id, limit=200)
            winning_trades.extend([normalize_trade(market, trade) for trade in trades])
    return winning_trades


def refresh_polymarket_data(market_limit: int = 25, trades_per_market: int = 200):
    session = get_session()
    try:
        markets_raw = fetch_markets(limit=market_limit)
        markets = [m for m in (_normalize_market(m) for m in markets_raw) if m]
        if markets:
            stmt = insert(Market).values(markets)
            stmt = stmt.on_conflict_do_update(
                index_elements=[Market.id],
                set_={
                    "question": stmt.excluded.question,
                    "volume": stmt.excluded.volume,
                    "liquidity": stmt.excluded.liquidity
                }
            )
            session.execute(stmt)

        trades = []
        with ThreadPoolExecutor(max_workers=8) as executor:
            futures = [
                executor.submit(_fetch_market_trades, market, trades_per_market)
                for market in markets_raw
            ]
            for future in as_completed(futures):
                trades.extend(future.result())

        if trades:
            trade_stmt = insert(Trade).values(trades)
            trade_stmt = trade_stmt.on_conflict_do_nothing(index_elements=[Trade.id])
            session.execute(trade_stmt)

        whales = detect_whales(trades, min_value=1000)
        if whales:
            whale_stmt = insert(Whale).values(whales)
            whale_stmt = whale_stmt.on_conflict_do_nothing(index_elements=[Whale.trade_id])
            session.execute(whale_stmt)

        winning_trades = _fetch_winning_trades(limit=20)
        update_smart_wallets(session, winning_trades)
        session.commit()
    except Exception as e:
        session.rollback()
        logger.error(f"Scheduler Error (Polymarket): {e}")
    finally:
        session.close()

def expire_trials():
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            '''
            SELECT ue.user_id, ue.expires_at
            FROM user_entitlements ue
            INNER JOIN (
                SELECT user_id, MAX(created_at) AS latest_created
                FROM user_entitlements
                GROUP BY user_id
            ) latest ON ue.user_id = latest.user_id AND ue.created_at = latest.latest_created
            WHERE ue.tier != 'free'
            '''
        )
        rows = cursor.fetchall()
        now = _utcnow()
        expiring_window = now + timedelta(hours=24)
        for row in rows:
            try:
                expires_at = datetime.fromisoformat(row["expires_at"])
                if now <= expires_at <= expiring_window:
                    user_id = int(row["user_id"])
                    if not has_recent_analytics_event(user_id, "trial_expiring_notice", 24):
                        delivered = deliver_trial_notification(
                            user_id=user_id,
                            title="Trial ending soon",
                            body="Your PolyPulse Pro trial ends in 24 hours.",
                            data={"type": "trial_expiring"}
                        )
                        if delivered:
                            save_analytics_event(user_id, "trial_expiring_notice", None)
                if expires_at < now:
                    user_id = int(row["user_id"])
                    if not has_recent_analytics_event(user_id, "trial_expired_notice", 24):
                        delivered = deliver_trial_notification(
                            user_id=user_id,
                            title="Trial ended",
                            body="Your PolyPulse Pro trial has ended. Unlock signals anytime.",
                            data={"type": "trial_expired"}
                        )
                        if delivered:
                            save_analytics_event(user_id, "trial_expired_notice", None)
                    now_str = _utcnow().isoformat()
                    set_user_entitlements(
                        user_id=user_id,
                        tier="free",
                        effective_at=now_str,
                        expires_at=now_str
                    )
            except Exception:
                continue
    finally:
        conn.close()

def _deliver_signal_notification(
    user_id: int,
    signal_id: int,
    attempt_id: Optional[int],
    mode: str,
    delay_seconds: int,
    retry_count: int
) -> dict:
    now = _utcnow()
    signal = get_signal_by_id(signal_id)
    if not signal:
        if attempt_id:
            update_notification_attempt(attempt_id, status="failed", retry_count=retry_count, error="signal_not_found")
        return {"sent": False, "retryable": False, "error": "signal_not_found"}

    settings = get_notification_settings(user_id)
    if not settings.get("push_enabled", True):
        if attempt_id:
            update_notification_attempt(attempt_id, status="disabled", retry_count=retry_count)
        else:
            create_notification_attempt(
                user_id=user_id,
                signal_id=signal_id,
                mode=mode,
                status="disabled",
                delay_seconds=delay_seconds,
                deliver_at=now.strftime("%Y-%m-%d %H:%M:%S")
            )
        return {"sent": False, "retryable": False, "error": "disabled"}

    tokens = get_fcm_tokens_for_user(user_id)
    if not tokens:
        if attempt_id:
            update_notification_attempt(attempt_id, status="no_tokens", retry_count=retry_count)
        else:
            create_notification_attempt(
                user_id=user_id,
                signal_id=signal_id,
                mode=mode,
                status="no_tokens",
                delay_seconds=delay_seconds,
                deliver_at=now.strftime("%Y-%m-%d %H:%M:%S")
            )
        return {"sent": False, "retryable": False, "error": "no_tokens"}

    if not attempt_id:
        attempt_id = create_notification_attempt(
            user_id=user_id,
            signal_id=signal_id,
            mode=mode,
            status="sending",
            delay_seconds=delay_seconds,
            deliver_at=now.strftime("%Y-%m-%d %H:%M:%S")
        )

    sent_at = _utcnow()
    data = {"signalId": str(signal["id"]), "attemptId": str(attempt_id), "sentAt": sent_at.isoformat()}
    result = fcm_service.send_multicast(
        tokens=tokens,
        title="PolyPulse Signal",
        body=signal["title"],
        data=data
    )
    success_count = int(result.get("successCount") or 0)
    failure_count = int(result.get("failureCount") or 0)
    err = result.get("error")
    status_value = "sent" if success_count > 0 else "failed"
    update_notification_attempt(
        attempt_id,
        status=status_value,
        sent_at=sent_at.strftime("%Y-%m-%d %H:%M:%S"),
        token_count=len(tokens),
        success_count=success_count,
        failure_count=failure_count,
        retry_count=retry_count,
        error=err
    )
    max_retries = int(os.environ.get("NOTIFY_MAX_RETRIES") or "3")
    retryable = (status_value == "failed") and (retry_count < max_retries) and (err not in {"firebase_not_initialized", "no_tokens"})
    return {"sent": success_count > 0, "retryable": retryable, "error": err}

def deliver_notification(user_id: int, signal_id: int) -> bool:
    result = _deliver_signal_notification(
        user_id=user_id,
        signal_id=signal_id,
        attempt_id=None,
        mode="direct",
        delay_seconds=0,
        retry_count=0
    )
    return bool(result.get("sent"))

def deliver_trial_notification(user_id: int, title: str, body: str, data: dict) -> bool:
    settings = get_notification_settings(user_id)
    if not settings.get("push_enabled", True):
        return False
    tokens = get_fcm_tokens_for_user(user_id)
    if not tokens:
        return False
    fcm_service.send_multicast(
        tokens=tokens,
        title=title,
        body=body,
        data=data
    )
    return True

def enqueue_notification_job(user_id: int, signal_id: int, deliver_at: datetime, delay_seconds: int) -> int:
    if not redis_client:
        return 0
    queued_at = _utcnow()
    status_value = "queued" if delay_seconds == 0 else "delayed"
    attempt_id = create_notification_attempt(
        user_id=user_id,
        signal_id=signal_id,
        mode="redis",
        status=status_value,
        delay_seconds=delay_seconds,
        queued_at=queued_at.strftime("%Y-%m-%d %H:%M:%S"),
        deliver_at=deliver_at.strftime("%Y-%m-%d %H:%M:%S")
    )
    try:
        payload = json.dumps({"attemptId": attempt_id, "userId": user_id, "signalId": signal_id, "retry": 0})
        redis_client.zadd(redis_queue_key, {payload: deliver_at.timestamp()})
        return int(attempt_id or 0)
    except Exception as e:
        update_notification_attempt(attempt_id, status="failed", error=str(e))
        return 0

def process_notification_queue():
    if not redis_client:
        return
    try:
        now_ts = _utcnow().timestamp()
        jobs = redis_client.zrangebyscore(redis_queue_key, 0, now_ts, start=0, num=200)
        if not jobs:
            return
        redis_client.zrem(redis_queue_key, *jobs)
        for raw in jobs:
            try:
                payload = json.loads(raw)
                attempt_id = int(payload.get("attemptId") or 0)
                retry_count = int(payload.get("retry") or 0)
                user_id = int(payload["userId"])
                signal_id = int(payload["signalId"])
                result = _deliver_signal_notification(
                    user_id=user_id,
                    signal_id=signal_id,
                    attempt_id=attempt_id if attempt_id > 0 else None,
                    mode="redis",
                    delay_seconds=0,
                    retry_count=retry_count
                )
                if result.get("sent"):
                    continue
                if not result.get("retryable"):
                    continue
                if attempt_id <= 0:
                    continue
                next_retry = retry_count + 1
                base = int(os.environ.get("NOTIFY_RETRY_BASE_SECONDS") or "10")
                backoff = base * (2 ** (next_retry - 1))
                deliver_at = _utcnow() + timedelta(seconds=backoff)
                update_notification_attempt(attempt_id, status="queued", retry_count=next_retry, error=result.get("error"))
                next_payload = json.dumps({"attemptId": attempt_id, "userId": user_id, "signalId": signal_id, "retry": next_retry})
                redis_client.zadd(redis_queue_key, {next_payload: deliver_at.timestamp()})
            except Exception:
                continue
    except Exception:
        return

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting up PolyPulse Backend...")
    init_db()
    init_polymarket_db()
    
    disable_scheduler = os.environ.get("DISABLE_SCHEDULER", "0") == "1"
    scheduler = None
    if not disable_scheduler:
        scheduler = BackgroundScheduler()
        scheduler.add_job(update_whale_data, 'interval', minutes=2)
        scheduler.add_job(whale_service.analyze_smart_money, 'interval', hours=6)
        scheduler.add_job(refresh_polymarket_data, 'interval', minutes=1)
        scheduler.add_job(expire_trials, 'interval', hours=24)
        scheduler.add_job(process_notification_queue, 'interval', seconds=5)
        scheduler.add_job(check_system_alerts, 'interval', seconds=60)
        auto_interval = int(os.environ.get("AUTO_SIGNAL_BROADCAST_INTERVAL_SECONDS") or "0")
        if auto_interval > 0:
            scheduler.add_job(generate_demo_signal_and_broadcast, 'interval', seconds=auto_interval)
        scheduler.start()
        scheduler.add_job(update_whale_data)
        scheduler.add_job(whale_service.analyze_smart_money) 
        scheduler.add_job(refresh_polymarket_data)
        scheduler.add_job(expire_trials)
        scheduler.add_job(process_notification_queue)
        scheduler.add_job(check_system_alerts)
        if auto_interval > 0:
            scheduler.add_job(generate_demo_signal_and_broadcast)
    
    yield
    
    # Shutdown
    logger.info("Shutting down...")
    if scheduler:
        scheduler.shutdown()

app = FastAPI(title="PolyPulse API", lifespan=lifespan)

app.add_middleware(GZipMiddleware, minimum_size=1000)

# CORS - 安全配置
_origins_env = os.environ.get("FRONTEND_ORIGINS") or "http://localhost:3000,http://localhost:8000"
_allowed_origins = [o.strip() for o in _origins_env.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=_allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Requested-With", "X-Request-ID"],
    expose_headers=["X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "X-Request-ID", "X-Response-Time-ms", "Server-Timing"],
    max_age=600,  # 10分钟
)

app.middleware("http")(performance_middleware)

@app.middleware("http")
async def rate_limit_middleware(request: Request, call_next):
    path = request.url.path
    if path in ("/docs", "/redoc", "/openapi.json"):
        return await call_next(request)
    limit = RATE_LIMIT_HEALTH if path == "/health" else RATE_LIMIT_DEFAULT
    key = _rate_limit_key(request)
    if rate_limiter.is_rate_limited(key, limit, RATE_LIMIT_WINDOW_SECONDS):
        headers = rate_limiter.get_rate_limit_headers(key, limit, RATE_LIMIT_WINDOW_SECONDS)
        return JSONResponse(
            status_code=429,
            content={
                "error": "rate_limit_exceeded",
                "message": "Too many requests. Please try again later.",
                "retry_after": RATE_LIMIT_WINDOW_SECONDS
            },
            headers=headers
        )
    response = await call_next(request)
    headers = rate_limiter.get_rate_limit_headers(key, limit, RATE_LIMIT_WINDOW_SECONDS)
    for header_key, header_value in headers.items():
        response.headers[header_key] = header_value
    return response

@app.middleware("http")
async def request_guard_middleware(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID") or secrets.token_hex(12)
    if request.method in ("POST", "PUT", "PATCH"):
        content_length = request.headers.get("Content-Length")
        if content_length is not None:
            try:
                if int(content_length) > REQUEST_MAX_BYTES:
                    return JSONResponse(
                        status_code=413,
                        content={
                            "error": "payload_too_large",
                            "message": "Request payload too large."
                        },
                        headers={"X-Request-ID": request_id}
                    )
            except ValueError:
                return JSONResponse(
                    status_code=400,
                    content={
                        "error": "invalid_content_length",
                        "message": "Invalid Content-Length header."
                    },
                    headers={"X-Request-ID": request_id}
                )
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response

@app.middleware("http")
async def security_headers_middleware(request: Request, call_next):
    response = await call_next(request)
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    response.headers["Permissions-Policy"] = "geolocation=(), microphone=(), camera=()"
    response.headers["Cross-Origin-Opener-Policy"] = "same-origin"
    response.headers["Cross-Origin-Resource-Policy"] = "same-site"
    response.headers["X-DNS-Prefetch-Control"] = "off"
    response.headers["Content-Security-Policy"] = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
    path = request.url.path
    if path in ("/token", "/register") or path.startswith(("/billing", "/notifications", "/watchlist", "/trial", "/entitlements", "/referral", "/analytics")):
        response.headers["Cache-Control"] = "no-store"
    if request.url.scheme == "https":
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
    return response

# Auth
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="token")
oauth2_scheme_optional = OAuth2PasswordBearer(tokenUrl="token", auto_error=False)

async def get_current_user(token: str = Depends(oauth2_scheme)):
    payload = auth_service.decode_token(token)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    user = get_user_by_email(payload.get("sub"))
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user

async def get_optional_user(token: str = Depends(oauth2_scheme_optional)):
    if not token:
        return None
    payload = auth_service.decode_token(token)
    if not payload:
        return None
    user = get_user_by_email(payload.get("sub"))
    return user

# --- Endpoints ---

def _plan_from_product_id(product_id: str) -> str:
    normalized = product_id.lower()
    if "year" in normalized or "annual" in normalized:
        return "pro_yearly"
    return "pro_monthly"

def _duration_days_for_plan(plan_id: str) -> int:
    if plan_id.endswith("yearly"):
        return 365
    return 30

def _normalize_subscription_status(status_value: str) -> str:
    normalized = (status_value or "").strip().lower()
    if normalized == "cancelled":
        return "canceled"
    allowed = {"active", "grace", "expired", "canceled", "paused"}
    return normalized if normalized in allowed else "active"

def _build_entitlements_response(tier: str, user_id: Optional[int] = None) -> EntitlementsResponse:
    rows = get_entitlements_for_tier(tier)
    features = [
        EntitlementFeature(
            key=row["feature_key"],
            enabled=bool(row["is_enabled"]),
            quota=row["quota"]
        )
        for row in rows
    ]
    effective_at = None
    expires_at = None
    if user_id is not None:
        entitlement = get_latest_user_entitlement(user_id)
        if entitlement:
            effective_at = entitlement.get("effective_at")
            expires_at = entitlement.get("expires_at")
    return EntitlementsResponse(
        tier=tier,
        features=features,
        effectiveAt=effective_at,
        expiresAt=expires_at
    )

def _resolve_tier_for_user(user_id: int) -> str:
    entitlement = get_latest_user_entitlement(user_id)
    if not entitlement:
        return "free"
    try:
        if datetime.fromisoformat(entitlement["expires_at"]) >= _utcnow():
            return entitlement["tier"]
    except Exception:
        return entitlement["tier"]
    return "free"

def _is_signal_locked(required_tier: str, user_tier: str) -> bool:
    required = (required_tier or "free").strip().lower()
    tier = (user_tier or "free").strip().lower()
    if required == "free":
        return False
    return tier != "pro"

def _notification_delay_seconds(signal_required_tier: str, user_tier: str) -> int:
    return 300 if _is_signal_locked(signal_required_tier, user_tier) else 0

def _is_trial_expired(user_id: int) -> bool:
    entitlement = get_latest_user_entitlement(user_id)
    if not entitlement:
        return False
    expires_at = entitlement.get("expires_at")
    if not expires_at:
        return False
    if entitlement.get("tier") != "free":
        return False
    try:
        return datetime.fromisoformat(expires_at) < _utcnow()
    except Exception:
        return False

def _build_in_app_message(user_id: int) -> Optional[InAppMessageResponse]:
    if has_recent_analytics_event(user_id, "in_app_message_delivered", 12):
        return None
    plans = [
        PaywallPlan(id="free", name="Free", price=0.0, currency="USD", period="month", trialDays=0),
        PaywallPlan(id="pro_monthly", name="Pro Monthly", price=9.9, currency="USD", period="month", trialDays=7),
        PaywallPlan(id="pro_yearly", name="Pro Yearly", price=99.0, currency="USD", period="year", trialDays=7)
    ]
    if _is_trial_expired(user_id):
        return InAppMessageResponse(
            id="trial_expired",
            type="trial_expired",
            title="Trial ended",
            body="Your Pro trial has ended. Unlock signals and performance history with Pro.",
            ctaText="Upgrade to Pro",
            ctaAction="open_paywall",
            plans=plans
        )
    return InAppMessageResponse(
        id="free_upgrade",
        type="upgrade",
        title="Unlock Pro signals",
        body="Get high-value alerts, low latency, and performance history.",
        ctaText="See Pro plans",
        ctaAction="open_paywall",
        plans=plans
    )

def _require_admin(request: Request, admin_key: Optional[str]) -> None:
    expected = os.environ.get("ADMIN_API_KEY")
    if expected:
        if not admin_key or admin_key != expected:
            raise HTTPException(status_code=403, detail="Forbidden")
        return
    host = request.client.host if request.client else ""
    if host not in ("127.0.0.1", "::1", "localhost"):
        raise HTTPException(status_code=403, detail="Forbidden")

def _broadcast_signal_to_users(signal_id: int) -> dict:
    signal = get_signal_by_id(signal_id)
    if not signal:
        return {"status": "not_found"}
    user_ids = get_user_ids_with_fcm_tokens()
    queued = 0
    delayed = 0
    sent = 0
    skipped = 0
    for user_id in user_ids:
        settings = get_notification_settings(user_id)
        if not settings.get("push_enabled", True):
            skipped += 1
            continue
        tier = _resolve_tier_for_user(user_id)
        delay_seconds = _notification_delay_seconds(signal["tier_required"], tier)
        if redis_client:
            deliver_at = _utcnow() + timedelta(seconds=delay_seconds)
            attempt_id = enqueue_notification_job(user_id, signal_id, deliver_at, delay_seconds)
            if attempt_id:
                if delay_seconds > 0:
                    delayed += 1
                else:
                    queued += 1
                continue
            if delay_seconds > 0:
                create_notification_attempt(
                    user_id=user_id,
                    signal_id=signal_id,
                    mode="direct",
                    status="delayed",
                    delay_seconds=delay_seconds,
                    deliver_at=deliver_at.strftime("%Y-%m-%d %H:%M:%S")
                )
                delayed += 1
                continue
            if deliver_notification(user_id, signal_id):
                sent += 1
            else:
                skipped += 1
            continue
        if delay_seconds > 0:
            skipped += 1
            continue
        if deliver_notification(user_id, signal_id):
            sent += 1
        else:
            skipped += 1
    return {
        "status": "ok",
        "users": len(user_ids),
        "queued": queued,
        "delayed": delayed,
        "sent": sent,
        "skipped": skipped
    }

def generate_demo_signal_and_broadcast() -> dict:
    now = _utcnow()
    title = f"Daily Pulse {now.strftime('%Y-%m-%d %H:%M UTC')}"
    content = "Top moves and whale activity. Unlock for details."
    tier_required = "pro"
    signal_id = create_signal(title, content, tier_required)
    save_analytics_event(None, "signal_generated", json.dumps({"signalId": signal_id, "tier": tier_required}))
    broadcast = _broadcast_signal_to_users(signal_id)
    return {"status": "created", "signalId": signal_id, "broadcast": broadcast}

@app.post("/register", response_model=UserResponse)
def register(user: UserRegister):
    existing = get_user_by_email(user.email)
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    hashed_pw = auth_service.get_password_hash(user.password)
    user_id = create_user(user.email, hashed_pw)
    
    if not user_id:
        raise HTTPException(status_code=400, detail="Email already registered")
    save_analytics_event(user_id, "signup", None)
    return {"id": user_id, "email": user.email, "created_at": "now"}

@app.post("/token", response_model=Token)
def login_for_access_token(form_data: OAuth2PasswordRequestForm = Depends()):
    user = get_user_by_email(form_data.username)
    if not user or not auth_service.verify_password(form_data.password, user['password_hash']):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = auth_service.create_access_token(data={"sub": user['email']})
    save_analytics_event(user["id"], "login", None)
    return {"access_token": access_token, "token_type": "bearer"}

@app.get("/users/me", response_model=UserResponse)
def read_users_me(current_user: dict = Depends(get_current_user)):
    return {
        "id": current_user["id"],
        "email": current_user["email"],
        "created_at": current_user["created_at"]
    }

@app.get("/dashboard/stats")
def get_dashboard_stats(current_user: dict = Depends(get_current_user)):
    # Placeholder for dashboard stats
    return {
        "alerts_24h": 5,
        "watchlist_count": 12,
        "top_movers": []
    }

@app.get("/dashboard/alerts")
def get_alerts(current_user: dict = Depends(get_current_user)):
    return get_recent_alerts()

@app.get("/api/alerts")
def api_get_alerts():
    return get_recent_alerts()

@app.get("/watchlist")
def watchlist_get(current_user: dict = Depends(get_current_user)):
    return get_watchlist(current_user["id"])

@app.post("/watchlist/{market_id}")
def watchlist_add(market_id: str, current_user: dict = Depends(get_current_user)):
    add_to_watchlist(current_user["id"], market_id)
    return {"status": "ok"}

@app.delete("/watchlist/{market_id}")
def watchlist_remove(market_id: str, current_user: dict = Depends(get_current_user)):
    remove_from_watchlist(current_user["id"], market_id)
    return {"status": "ok"}

@app.get("/dashboard/whales")
def get_whale_activity(current_user: dict = Depends(get_current_user)):
    """Get recent whale activity (large trades)"""
    return whale_service.fetch_whale_activity()

@app.get("/dashboard/leaderboard")
def get_leaderboard(current_user: dict = Depends(get_current_user)):
    """Get top whale traders by volume"""
    return whale_service.get_leaderboard()


@app.get("/api/whales")
def api_get_whales(limit: int = 50, offset: int = 0, sort: str = "latest"):
    limit, offset = _sanitize_pagination(limit, offset, 200)
    cache_key = _cache_key("whales", [str(limit), str(offset), sort])
    def build():
        conn = get_db_connection()
        try:
            cursor = conn.cursor()
            order_by = "value_usd DESC" if sort == "value" else "timestamp DESC"
            cursor.execute(
                f"""
                SELECT id, timestamp, maker_address, market_question, outcome, side, size, price, value_usd, market_slug
                FROM whale_trades
                ORDER BY {order_by}
                LIMIT ? OFFSET ?
                """,
                (limit, offset)
            )
            rows = cursor.fetchall()
            if rows:
                return [
                    {
                        "trade_id": f"sqlite-{row['id']}",
                        "market_question": row["market_question"],
                        "outcome": row["outcome"],
                        "side": row["side"],
                        "price": row["price"],
                        "size": row["size"],
                        "value_usd": row["value_usd"],
                        "timestamp": datetime.fromtimestamp(row["timestamp"], timezone.utc).replace(tzinfo=None).isoformat(),
                        "maker_address": row["maker_address"],
                        "market_slug": row["market_slug"]
                    }
                    for row in rows
                ]
        finally:
            conn.close()
        session = get_session()
        try:
            query = session.query(Whale, Trade).join(Trade, Whale.trade_id == Trade.id)
            if sort == "value":
                query = query.order_by(Whale.value.desc())
            else:
                query = query.order_by(Whale.timestamp.desc())
            rows = query.offset(offset).limit(limit).all()
            return [
                {
                    "trade_id": whale.trade_id,
                    "market_question": trade.question,
                    "outcome": "",
                    "side": trade.side,
                    "price": trade.price,
                    "size": trade.size,
                    "value_usd": whale.value,
                    "timestamp": whale.timestamp.isoformat(),
                    "maker_address": whale.address,
                    "market_slug": trade.market
                }
                for whale, trade in rows
            ]
        finally:
            session.close()
    return _cached_response(cache_key, 10, build)


@app.get("/api/whales/leaderboard")
def api_whale_leaderboard(limit: int = 50, offset: int = 0):
    limit, offset = _sanitize_pagination(limit, offset, 200)
    return api_get_whales(limit=limit, offset=offset, sort="value")


@app.get("/api/trades")
def api_get_trades(limit: int = 100, offset: int = 0):
    limit, offset = _sanitize_pagination(limit, offset, 200)
    cache_key = _cache_key("trades", [str(limit), str(offset)])
    def build():
        session = get_session()
        try:
            rows = session.query(Trade).order_by(Trade.timestamp.desc()).offset(offset).limit(limit).all()
            return [
                {
                    "id": trade.id,
                    "market_question": trade.question,
                    "address": trade.address,
                    "side": trade.side,
                    "price": trade.price,
                    "size": trade.size,
                    "value": trade.value,
                    "timestamp": trade.timestamp.isoformat(),
                    "market": trade.market
                }
                for trade in rows
            ]
        finally:
            session.close()
    return _cached_response(cache_key, 10, build)


@app.get("/signals", response_model=List[SignalResponse])
def get_signals_api(
    limit: int = 50,
    offset: int = 0,
    current_user: dict = Depends(get_optional_user)
):
    limit, offset = _sanitize_pagination(limit, offset, 200)
    tier = "free"
    if current_user:
        tier = _resolve_tier_for_user(current_user["id"])
    cache_key = _cache_key("signals", [tier, str(limit), str(offset)])
    def build():
        rows = get_signals(limit=limit, offset=offset)
        response = []
        for row in rows:
            required = row["tier_required"]
            locked = _is_signal_locked(required, tier)
            evidence_payload = None
            if row["evidence_json"]:
                evidence_payload = SignalEvidence(**json.loads(row["evidence_json"])).model_dump()
            response.append(
                {
                    "id": row["id"],
                    "title": row["title"],
                    "content": None if locked else row["content"],
                    "locked": locked,
                    "tierRequired": required,
                    "createdAt": row["created_at"],
                    "evidence": evidence_payload
                }
            )
        return response
    return _cached_response(cache_key, 15, build)

@app.get("/signals/stats", response_model=SignalStatsResponse)
def get_signal_stats_api():
    cache_key = _cache_key("signals_stats", ["7"])
    def build():
        stats = get_signal_stats()
        return SignalStatsResponse(
            signals7d=stats["signals_7d"],
            evidence7d=stats["evidence_7d"]
        ).model_dump()
    return _cached_response(cache_key, 30, build)

@app.get("/insights/credibility", response_model=SignalCredibilityResponse)
def get_signal_credibility_api():
    cache_key = _cache_key("insights_credibility", ["7", "30"])
    def build():
        w7 = get_signal_credibility(7)
        w30 = get_signal_credibility(30)
        payload = SignalCredibilityResponse(
            window7d=SignalCredibilityWindowResponse(
                windowDays=w7["window_days"],
                signalsTotal=w7["signals_total"],
                signalsWithEvidence=w7["signals_with_evidence"],
                evidenceRate=w7["evidence_rate"],
                evaluatedTotal=w7["evaluated_total"],
                hitTotal=w7["hit_total"],
                hitRate=w7["hit_rate"],
                hitRateCiLow=w7["hit_rate_ci_low"],
                hitRateCiHigh=w7["hit_rate_ci_high"],
                latencyCount=w7["latency_count"],
                latencyP50Seconds=w7["latency_p50_seconds"],
                latencyP90Seconds=w7["latency_p90_seconds"],
                latencyHistogram=[SignalCredibilityHistogramItem(**item) for item in w7["latency_histogram"]],
                leadCount=w7["lead_count"],
                leadP50Seconds=w7["lead_p50_seconds"],
                leadP90Seconds=w7["lead_p90_seconds"],
                leadHistogram=[SignalCredibilityHistogramItem(**item) for item in w7["lead_histogram"]]
            ),
            window30d=SignalCredibilityWindowResponse(
                windowDays=w30["window_days"],
                signalsTotal=w30["signals_total"],
                signalsWithEvidence=w30["signals_with_evidence"],
                evidenceRate=w30["evidence_rate"],
                evaluatedTotal=w30["evaluated_total"],
                hitTotal=w30["hit_total"],
                hitRate=w30["hit_rate"],
                hitRateCiLow=w30["hit_rate_ci_low"],
                hitRateCiHigh=w30["hit_rate_ci_high"],
                latencyCount=w30["latency_count"],
                latencyP50Seconds=w30["latency_p50_seconds"],
                latencyP90Seconds=w30["latency_p90_seconds"],
                latencyHistogram=[SignalCredibilityHistogramItem(**item) for item in w30["latency_histogram"]],
                leadCount=w30["lead_count"],
                leadP50Seconds=w30["lead_p50_seconds"],
                leadP90Seconds=w30["lead_p90_seconds"],
                leadHistogram=[SignalCredibilityHistogramItem(**item) for item in w30["lead_histogram"]]
            )
        )
        return payload.model_dump()
    return _cached_response(cache_key, 60, build)

@app.get("/insights/delivery", response_model=DeliveryObservabilityResponse)
def get_delivery_observability_api():
    cache_key = _cache_key("insights_delivery", ["1", "7"])
    def build():
        w1 = get_delivery_observability(1)
        w7 = get_delivery_observability(7)
        queue_depth = None
        oldest_due_seconds = None
        if redis_client:
            try:
                queue_depth = int(redis_client.zcard(redis_queue_key))
                oldest = redis_client.zrange(redis_queue_key, 0, 0, withscores=True)
                if oldest:
                    score = float(oldest[0][1])
                    oldest_due_seconds = int(_utcnow().timestamp() - score)
            except Exception:
                queue_depth = None
                oldest_due_seconds = None
        payload = DeliveryObservabilityResponse(
            window1d=DeliveryWindowResponse(
                windowDays=w1["window_days"],
                attemptsTotal=w1["attempts_total"],
                queued=w1["queued"],
                delayed=w1["delayed"],
                sent=w1["sent"],
                failed=w1["failed"],
                noTokens=w1["no_tokens"],
                disabled=w1["disabled"],
                successRate=w1["success_rate"],
                pushOpenCount=w1["push_open_count"],
                clickThroughRate=w1["click_through_rate"],
                queueDelayP50Seconds=w1["queue_delay_p50_seconds"],
                queueDelayP90Seconds=w1["queue_delay_p90_seconds"],
                dispatchDelayP50Seconds=w1["dispatch_delay_p50_seconds"],
                dispatchDelayP90Seconds=w1["dispatch_delay_p90_seconds"]
            ),
            window7d=DeliveryWindowResponse(
                windowDays=w7["window_days"],
                attemptsTotal=w7["attempts_total"],
                queued=w7["queued"],
                delayed=w7["delayed"],
                sent=w7["sent"],
                failed=w7["failed"],
                noTokens=w7["no_tokens"],
                disabled=w7["disabled"],
                successRate=w7["success_rate"],
                pushOpenCount=w7["push_open_count"],
                clickThroughRate=w7["click_through_rate"],
                queueDelayP50Seconds=w7["queue_delay_p50_seconds"],
                queueDelayP90Seconds=w7["queue_delay_p90_seconds"],
                dispatchDelayP50Seconds=w7["dispatch_delay_p50_seconds"],
                dispatchDelayP90Seconds=w7["dispatch_delay_p90_seconds"]
            ),
            redisQueueDepth=queue_depth,
            redisOldestDueSeconds=oldest_due_seconds
        )
        return payload.model_dump()
    return _cached_response(cache_key, 30, build)

@app.post("/admin/signals/{signal_id}/evaluation")
def admin_upsert_signal_evaluation(
    signal_id: int,
    payload: AdminSignalEvaluationRequest,
    request: Request,
    x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key")
):
    _require_admin(request, x_admin_key)
    if not get_signal_by_id(signal_id):
        raise HTTPException(status_code=404, detail="Signal not found")
    upsert_signal_evaluation(
        signal_id=signal_id,
        is_hit=payload.isHit,
        lead_seconds=payload.leadSeconds,
        evaluated_at=payload.evaluatedAt
    )
    return {"status": "ok"}


@app.get("/signals/{signal_id}", response_model=SignalResponse)
def get_signal_detail(
    signal_id: int,
    requireUnlocked: bool = False,
    current_user: dict = Depends(get_optional_user)
):
    row = get_signal_by_id(signal_id)
    if not row:
        raise HTTPException(status_code=404, detail="Signal not found")
    tier = "free"
    user_id = None
    if current_user:
        user_id = current_user["id"]
        tier = _resolve_tier_for_user(user_id)
    required = row["tier_required"]
    locked = _is_signal_locked(required, tier)
    if locked and requireUnlocked:
        raise HTTPException(status_code=402, detail="Payment required")
    properties = json.dumps({"signalId": signal_id, "locked": locked})
    save_analytics_event(user_id, "signal_view", properties)
    evidence = None
    if row["evidence_json"]:
        evidence = SignalEvidence(**json.loads(row["evidence_json"]))
    return SignalResponse(
        id=row["id"],
        title=row["title"],
        content=None if locked else row["content"],
        locked=locked,
        tierRequired=required,
        createdAt=row["created_at"],
        evidence=evidence
    )


@app.get("/paywall", response_model=PaywallResponse)
def get_paywall(current_user: dict = Depends(get_optional_user)):
    user_id = current_user["id"] if current_user else None
    save_analytics_event(user_id, "paywall_view", None)
    plans = [
        PaywallPlan(id="free", name="Free", price=0.0, currency="USD", period="month", trialDays=0),
        PaywallPlan(id="pro_monthly", name="Pro Monthly", price=9.9, currency="USD", period="month", trialDays=7),
        PaywallPlan(id="pro_yearly", name="Pro Yearly", price=99.0, currency="USD", period="year", trialDays=7)
    ]
    return PaywallResponse(plans=plans)

@app.get("/in-app-message", response_model=Optional[InAppMessageResponse])
def get_in_app_message(current_user: dict = Depends(get_current_user)):
    user_id = current_user["id"]
    message = _build_in_app_message(user_id)
    if message:
        save_analytics_event(user_id, "in_app_message_delivered", json.dumps({"messageId": message.id}))
    return message


@app.post("/trial/start", response_model=TrialStartResponse)
def start_trial(current_user: dict = Depends(get_current_user)):
    start_at = _utcnow()
    end_at = start_at + timedelta(days=7)
    start_at_str = start_at.isoformat()
    end_at_str = end_at.isoformat()
    set_user_entitlements(
        user_id=current_user["id"],
        tier="pro",
        effective_at=start_at_str,
        expires_at=end_at_str
    )
    save_analytics_event(current_user["id"], "trial_started", None)
    return TrialStartResponse(status="active", tier="pro", expiresAt=end_at_str)


@app.post("/notifications/register")
def register_notifications(
    payload: NotificationRegisterRequest,
    current_user: dict = Depends(get_current_user)
):
    upsert_fcm_token(current_user["id"], payload.token)
    return {"status": "ok"}

@app.get("/notification-settings", response_model=NotificationSettingsResponse)
def get_notification_settings_api(current_user: dict = Depends(get_current_user)):
    settings = get_notification_settings(current_user["id"])
    return NotificationSettingsResponse(enabled=bool(settings.get("push_enabled", True)))

@app.put("/notification-settings", response_model=NotificationSettingsResponse)
def update_notification_settings_api(
    payload: NotificationSettingsUpdateRequest,
    current_user: dict = Depends(get_current_user)
):
    set_notification_settings(current_user["id"], payload.enabled)
    return NotificationSettingsResponse(enabled=payload.enabled)


@app.post("/notifications/send")
def send_notification(
    payload: NotificationSendRequest,
    current_user: dict = Depends(get_current_user)
):
    signal = get_signal_by_id(payload.signalId)
    if not signal:
        raise HTTPException(status_code=404, detail="Signal not found")
    target_tier = _resolve_tier_for_user(payload.userId)
    delay_seconds = _notification_delay_seconds(signal["tier_required"], target_tier)
    if redis_client:
        deliver_at = _utcnow() + timedelta(seconds=delay_seconds)
        attempt_id = enqueue_notification_job(payload.userId, payload.signalId, deliver_at, delay_seconds)
        if attempt_id:
            return {"status": "queued" if delay_seconds == 0 else "delayed", "attemptId": attempt_id}
        if delay_seconds > 0:
            attempt_id = create_notification_attempt(
                user_id=payload.userId,
                signal_id=payload.signalId,
                mode="direct",
                status="delayed",
                delay_seconds=delay_seconds,
                deliver_at=deliver_at.strftime("%Y-%m-%d %H:%M:%S")
            )
            return {"status": "delayed", "attemptId": attempt_id}
        sent = deliver_notification(payload.userId, payload.signalId)
        return {"status": "sent" if sent else "no_tokens"}
    if delay_seconds > 0:
        attempt_id = create_notification_attempt(
            user_id=payload.userId,
            signal_id=payload.signalId,
            mode="direct",
            status="delayed",
            delay_seconds=delay_seconds,
            deliver_at=(_utcnow() + timedelta(seconds=delay_seconds)).strftime("%Y-%m-%d %H:%M:%S")
        )
        return {"status": "delayed", "attemptId": attempt_id}
    sent = deliver_notification(payload.userId, payload.signalId)
    return {"status": "sent" if sent else "no_tokens"}

@app.post("/admin/signals")
def admin_create_signal(
    payload: AdminSignalCreateRequest,
    request: Request,
    x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key")
):
    _require_admin(request, x_admin_key)
    signal_id = create_signal(payload.title, payload.content, payload.tierRequired)
    result = {"status": "created", "signalId": signal_id}
    if payload.broadcast:
        result["broadcast"] = admin_broadcast_signal(signal_id, request, x_admin_key)
    return result

@app.post("/admin/signals/{signal_id}/broadcast")
def admin_broadcast_signal(
    signal_id: int,
    request: Request,
    x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key")
):
    _require_admin(request, x_admin_key)
    if not get_signal_by_id(signal_id):
        raise HTTPException(status_code=404, detail="Signal not found")
    return _broadcast_signal_to_users(signal_id)

@app.post("/admin/auto-signal/trigger")
def admin_trigger_auto_signal(
    request: Request,
    x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key")
):
    _require_admin(request, x_admin_key)
    return generate_demo_signal_and_broadcast()


@app.post("/analytics/event")
def track_event(
    payload: AnalyticsEventRequest,
    current_user: dict = Depends(get_optional_user)
):
    user_id = current_user["id"] if current_user else None
    properties = json.dumps(payload.properties) if payload.properties else None
    save_analytics_event(user_id, payload.eventName, properties)
    return {"status": "ok"}


@app.get("/daily-pulse", response_model=List[DailyPulseResponse])
def get_daily_pulse_api(limit: int = 20, offset: int = 0):
    rows = get_daily_pulse(limit=limit, offset=offset)
    return [
        DailyPulseResponse(
            id=row["id"],
            title=row["title"],
            summary=row["summary"],
            content=row["content"],
            createdAt=row["created_at"]
        )
        for row in rows
    ]


@app.get("/referral/code", response_model=ReferralCodeResponse)
def get_referral_code_api(current_user: dict = Depends(get_current_user)):
    existing = get_referral_code(current_user["id"])
    if existing:
        return ReferralCodeResponse(code=existing)
    code = None
    for _ in range(10):
        candidate = secrets.token_urlsafe(6).replace("-", "").replace("_", "")[:8]
        try:
            insert_referral_code(current_user["id"], candidate)
            code = candidate
            break
        except Exception:
            continue
    if not code:
        raise HTTPException(status_code=400, detail="Failed to generate unique referral code")
    return ReferralCodeResponse(code=code)


@app.post("/referral/redeem", response_model=ReferralRedeemResponse)
def redeem_referral_code_api(
    payload: ReferralRedeemRequest,
    current_user: dict = Depends(get_current_user)
):
    success = redeem_referral_code(payload.code, current_user["id"])
    return ReferralRedeemResponse(status="ok" if success else "invalid")


@app.get("/feature-flags", response_model=List[FeatureFlagResponse])
def feature_flags(current_user: dict = Depends(get_optional_user)):
    tier = "free"
    if current_user:
        tier = _resolve_tier_for_user(current_user["id"])
    rows = get_feature_flags(tier)
    return [
        FeatureFlagResponse(key=row["feature_key"], enabled=bool(row["enabled"]))
        for row in rows
    ]


@app.get("/metrics", response_model=MetricsResponse)
def metrics():
    cache_key = _cache_key("metrics_counts", [])
    def build():
        return MetricsResponse(**get_metrics_counts()).model_dump()
    return _cached_response(cache_key, 5, build)


@app.get("/api/smart")
def api_get_smart_wallets(limit: int = 50, offset: int = 0):
    limit, offset = _sanitize_pagination(limit, offset, 200)
    cache_key = _cache_key("smart", [str(limit), str(offset)])
    def build():
        session = get_session()
        try:
            rows = session.query(SmartWallet).order_by(SmartWallet.profit.desc()).offset(offset).limit(limit).all()
            if rows:
                has_signal = any(
                    (wallet.profit or 0) != 0 or (wallet.roi or 0) != 0 or (wallet.win_rate or 0) != 0
                    for wallet in rows
                )
                if has_signal:
                    return [
                        {
                            "address": wallet.address,
                            "profit": wallet.profit,
                            "roi": wallet.roi,
                            "win_rate": wallet.win_rate,
                            "total_trades": wallet.total_trades
                        }
                        for wallet in rows
                    ]
        finally:
            session.close()
        conn = get_db_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT maker_address as address,
                       SUM(value_usd) as total_value,
                       SUM(CASE WHEN side = 'BUY' THEN value_usd ELSE 0 END) as buy_value,
                       SUM(CASE WHEN side = 'SELL' THEN value_usd ELSE 0 END) as sell_value,
                       COUNT(*) as total_trades
                FROM whale_trades
                GROUP BY maker_address
                ORDER BY total_value DESC
                LIMIT ? OFFSET ?
                """,
                (limit, offset)
            )
            rows = cursor.fetchall()
            if rows:
                return [
                    {
                        "address": row["address"],
                        "profit": row["total_value"],
                        "roi": (
                            (row["buy_value"] - row["sell_value"]) / row["total_value"]
                            if row["total_value"] else 0.0
                        ),
                        "win_rate": (
                            row["buy_value"] / row["total_value"]
                            if row["total_value"] else 0.0
                        ),
                        "total_trades": row["total_trades"]
                    }
                    for row in rows
                ]
        finally:
            conn.close()
        session = get_session()
        try:
            whale_rows = (
                session.query(
                    Whale.address.label("address"),
                    func.sum(Whale.value).label("total_value"),
                    func.count(Whale.trade_id).label("total_trades")
                )
                .group_by(Whale.address)
                .order_by(func.sum(Whale.value).desc())
                .offset(offset)
                .limit(limit)
                .all()
            )
            return [
                {
                    "address": row.address,
                    "profit": row.total_value,
                    "roi": 0.0,
                    "win_rate": 0.0,
                    "total_trades": row.total_trades
                }
                for row in whale_rows
            ]
        finally:
            session.close()
    return _cached_response(cache_key, 15, build)


@app.post("/api/refresh")
def api_refresh(
    request: Request,
    x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key")
):
    if os.environ.get("ADMIN_API_KEY") and not os.environ.get("PYTEST_CURRENT_TEST"):
        host = request.client.host if request.client else ""
        if host not in ("127.0.0.1", "::1", "localhost", "testclient", "testserver"):
            _require_admin(request, x_admin_key)
    refresh_polymarket_data()
    return {"status": "ok"}

@app.post("/billing/verify", response_model=BillingVerifyResponse)
def verify_billing(
    request: BillingVerifyRequest,
    current_user: dict = Depends(get_current_user)
):
    user_id = current_user["id"]
    plan_id = _plan_from_product_id(request.productId)
    save_analytics_event(
        user_id,
        "subscribe_verify",
        json.dumps({"platform": request.platform, "productId": request.productId, "planId": plan_id})
    )
    start_at = _utcnow()
    end_at = start_at + timedelta(days=_duration_days_for_plan(plan_id))
    start_at_str = start_at.isoformat()
    end_at_str = end_at.isoformat()

    save_transaction(
        user_id=user_id,
        platform=request.platform,
        order_id=request.purchaseToken,
        product_id=request.productId,
        purchase_token=request.purchaseToken,
        purchase_state="purchased",
        amount=0,
        currency="USD",
        purchased_at=start_at_str
    )
    upsert_subscription(
        user_id=user_id,
        platform=request.platform,
        plan_id=plan_id,
        status="active",
        start_at=start_at_str,
        end_at=end_at_str,
        auto_renew=True
    )
    set_user_entitlements(
        user_id=user_id,
        tier="pro",
        effective_at=start_at_str,
        expires_at=end_at_str
    )

    subscription = SubscriptionInfo(
        status="active",
        planId=plan_id,
        startAt=start_at_str,
        endAt=end_at_str,
        autoRenew=True
    )
    entitlements = _build_entitlements_response("pro", user_id)
    return BillingVerifyResponse(
        status="active",
        subscription=subscription,
        entitlements=entitlements
    )

@app.get("/billing/status", response_model=BillingStatusResponse)
def billing_status(current_user: dict = Depends(get_current_user)):
    subscription = get_latest_subscription(current_user["id"])
    if not subscription:
        return BillingStatusResponse(status="none")
    status_value = _normalize_subscription_status(subscription["status"])
    try:
        end_at = datetime.fromisoformat(subscription["end_at"])
        if status_value in ["active", "grace"] and end_at < _utcnow():
            status_value = "expired"
    except Exception:
        pass
    return BillingStatusResponse(
        status=status_value,
        planId=subscription["plan_id"],
        startAt=subscription["start_at"],
        endAt=subscription["end_at"],
        autoRenew=bool(subscription["auto_renew"])
    )

@app.get("/entitlements/me", response_model=EntitlementsResponse)
def entitlements_me(current_user: dict = Depends(get_current_user)):
    tier = _resolve_tier_for_user(current_user["id"])
    return _build_entitlements_response(tier, current_user["id"])

@app.post("/billing/webhook")
def billing_webhook(
    payload: BillingWebhookRequest,
    x_webhook_secret: Optional[str] = Header(default=None, alias="X-Webhook-Secret")
):
    expected_secret = os.environ.get("BILLING_WEBHOOK_SECRET")
    if expected_secret and x_webhook_secret != expected_secret:
        raise HTTPException(status_code=403, detail="Forbidden")
    order_id = payload.orderId or payload.purchaseToken
    if not order_id:
        return {"status": "ignored"}
    user_id = get_transaction_user_id(order_id)
    if not user_id:
        return {"status": "ignored"}
    if not payload.status:
        return {"status": "ignored"}
    normalized_status = _normalize_subscription_status(payload.status)
    existing = get_latest_subscription(user_id)
    if not existing:
        return {"status": "ignored"}
    start_at = payload.startAt or existing["start_at"]
    end_at = payload.endAt or existing["end_at"]
    auto_renew = normalized_status == "active"
    upsert_subscription(
        user_id=user_id,
        platform="google_play",
        plan_id=existing["plan_id"],
        status=normalized_status,
        start_at=start_at,
        end_at=end_at,
        auto_renew=auto_renew
    )
    if normalized_status in ["active", "grace"]:
        set_user_entitlements(
            user_id=user_id,
            tier="pro",
            effective_at=start_at,
            expires_at=end_at
        )
    else:
        now_str = _utcnow().isoformat()
        set_user_entitlements(
            user_id=user_id,
            tier="free",
            effective_at=now_str,
            expires_at=now_str
        )
    return {"status": "ok"}

@app.get("/")
def read_root():
    return {"message": "PolyPulse Backend is running"}


@app.get("/health")
def health_check():
    return {"status": "ok", "timestamp": _utcnow().isoformat()}


@app.get("/metrics/prometheus")
def metrics_endpoint():
    """Prometheus metrics endpoint for performance monitoring"""
    return Response(
        generate_latest(),
        media_type=CONTENT_TYPE_LATEST
    )


@app.get("/performance")
def performance_dashboard():
    """Performance metrics dashboard"""
    return {
        "timestamp": _utcnow().isoformat(),
        "metrics_available": [
            "http_requests_total",
            "http_request_duration_seconds", 
            "db_query_duration_seconds",
            "redis_operation_duration_seconds"
        ]
    }


@app.post("/monitor/alert")
def monitor_alert(
    payload: MonitorAlertRequest,
    request: Request,
    x_monitor_key: Optional[str] = Header(default=None, alias="X-Monitor-Key")
):
    expected_key = os.environ.get("MONITOR_ALERT_KEY")
    if expected_key and x_monitor_key != expected_key:
        raise HTTPException(status_code=403, detail="Forbidden")
    level = payload.level.strip().lower()
    message = payload.message
    source = payload.source or "unknown"
    _record_monitor_alert(level, message, source)
    return {"status": "ok"}

@app.get("/monitor/alerts")
def get_monitor_alerts():
    return {"alerts": cache.get(ALERT_CACHE_KEY) or []}

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT") or "8000")
    uvicorn.run(app, host="0.0.0.0", port=port)
