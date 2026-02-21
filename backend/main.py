import logging
import json
import secrets
import os
import redis
from contextlib import asynccontextmanager
from datetime import datetime, timedelta
from fastapi import FastAPI, Depends, HTTPException, status, Header, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from apscheduler.schedulers.background import BackgroundScheduler
from typing import List, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed
from sqlalchemy.dialects.sqlite import insert
from sqlalchemy import func

from app.database import init_db, get_recent_alerts, create_user, get_user_by_email, get_db_connection
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
    upsert_fcm_token,
    get_fcm_tokens_for_user,
    get_user_ids_with_fcm_tokens,
    get_notification_settings,
    set_notification_settings,
    save_analytics_event,
    get_watchlist,
    add_to_watchlist,
    remove_from_watchlist,
    get_daily_pulse,
    get_referral_code,
    insert_referral_code,
    redeem_referral_code,
    get_feature_flags,
    get_metrics_counts
)
from app.services.auth_service import AuthService
from app.services.market_service import MarketService
from app.services.whale_service import WhaleService
from app.services.fcm_service import FCMService
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
    SignalResponse,
    PaywallPlan,
    PaywallResponse,
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
    AdminSignalCreateRequest
)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Services
auth_service = AuthService()
market_service = MarketService()
whale_service = WhaleService()
fcm_service = FCMService()
redis_client = redis.Redis.from_url(os.environ["REDIS_URL"], decode_responses=True) if os.environ.get("REDIS_URL") else None
redis_queue_key = "polypulse:notifications"

# Scheduler Jobs
def update_whale_data():
    logger.info("Scheduler: Fetching whale activity...")
    try:
        whale_service.fetch_whale_activity()
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
        for row in rows:
            try:
                expires_at = datetime.fromisoformat(row["expires_at"])
                if expires_at < datetime.utcnow():
                    now_str = datetime.utcnow().isoformat()
                    set_user_entitlements(
                        user_id=row["user_id"],
                        tier="free",
                        effective_at=now_str,
                        expires_at=now_str
                    )
            except Exception:
                continue
    finally:
        conn.close()

def deliver_notification(user_id: int, signal_id: int) -> bool:
    signal = get_signal_by_id(signal_id)
    if not signal:
        return False
    settings = get_notification_settings(user_id)
    if not settings.get("push_enabled", True):
        return False
    tokens = get_fcm_tokens_for_user(user_id)
    if not tokens:
        return False
    fcm_service.send_multicast(
        tokens=tokens,
        title="PolyPulse Signal",
        body=signal["title"],
        data={"signalId": str(signal["id"])}
    )
    return True

def enqueue_notification_job(user_id: int, signal_id: int, deliver_at: datetime) -> bool:
    if not redis_client:
        return False
    payload = json.dumps({"userId": user_id, "signalId": signal_id})
    redis_client.zadd(redis_queue_key, {payload: deliver_at.timestamp()})
    return True

def process_notification_queue():
    if not redis_client:
        return
    now_ts = datetime.utcnow().timestamp()
    jobs = redis_client.zrangebyscore(redis_queue_key, 0, now_ts, start=0, num=200)
    if not jobs:
        return
    redis_client.zrem(redis_queue_key, *jobs)
    for raw in jobs:
        try:
            payload = json.loads(raw)
            deliver_notification(payload["userId"], payload["signalId"])
        except Exception:
            continue

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Starting up PolyPulse Backend...")
    init_db()
    init_polymarket_db()
    
    scheduler = BackgroundScheduler()
    # Fetch whale data every 2 minutes
    scheduler.add_job(update_whale_data, 'interval', minutes=2)
    
    # Analyze smart money (closed markets) every 6 hours
    scheduler.add_job(whale_service.analyze_smart_money, 'interval', hours=6)

    scheduler.add_job(refresh_polymarket_data, 'interval', minutes=1)
    scheduler.add_job(expire_trials, 'interval', hours=24)
    scheduler.add_job(process_notification_queue, 'interval', seconds=5)
    auto_interval = int(os.environ.get("AUTO_SIGNAL_BROADCAST_INTERVAL_SECONDS") or "0")
    if auto_interval > 0:
        scheduler.add_job(generate_demo_signal_and_broadcast, 'interval', seconds=auto_interval)
    
    scheduler.start()
    
    # Run one immediate update in background to avoid blocking startup
    scheduler.add_job(update_whale_data)
    # Run smart money analysis in background (don't block startup)
    scheduler.add_job(whale_service.analyze_smart_money) 
    scheduler.add_job(refresh_polymarket_data)
    scheduler.add_job(expire_trials)
    scheduler.add_job(process_notification_queue)
    if auto_interval > 0:
        scheduler.add_job(generate_demo_signal_and_broadcast)
    
    yield
    
    # Shutdown
    logger.info("Shutting down...")
    scheduler.shutdown()

app = FastAPI(title="PolyPulse API", lifespan=lifespan)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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

def _build_entitlements_response(tier: str) -> EntitlementsResponse:
    rows = get_entitlements_for_tier(tier)
    features = [
        EntitlementFeature(
            key=row["feature_key"],
            enabled=bool(row["is_enabled"]),
            quota=row["quota"]
        )
        for row in rows
    ]
    return EntitlementsResponse(tier=tier, features=features)

def _resolve_tier_for_user(user_id: int) -> str:
    entitlement = get_latest_user_entitlement(user_id)
    if not entitlement:
        return "free"
    try:
        if datetime.fromisoformat(entitlement["expires_at"]) >= datetime.utcnow():
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
            deliver_at = datetime.utcnow() + timedelta(seconds=delay_seconds)
            enqueue_notification_job(user_id, signal_id, deliver_at)
            if delay_seconds > 0:
                delayed += 1
            else:
                queued += 1
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
    now = datetime.utcnow()
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
        raise HTTPException(status_code=500, detail="Failed to create user")
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
    session = get_session()
    try:
        query = session.query(Whale, Trade).join(Trade, Whale.trade_id == Trade.id)
        if sort == "value":
            query = query.order_by(Whale.value.desc())
        else:
            query = query.order_by(Whale.timestamp.desc())
        rows = query.offset(offset).limit(limit).all()
        if rows:
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
        return [
            {
                "trade_id": f"sqlite-{row['id']}",
                "market_question": row["market_question"],
                "outcome": row["outcome"],
                "side": row["side"],
                "price": row["price"],
                "size": row["size"],
                "value_usd": row["value_usd"],
                "timestamp": datetime.utcfromtimestamp(row["timestamp"]).isoformat(),
                "maker_address": row["maker_address"],
                "market_slug": row["market_slug"]
            }
            for row in rows
        ]
    finally:
        conn.close()


@app.get("/api/whales/leaderboard")
def api_whale_leaderboard(limit: int = 50, offset: int = 0):
    return api_get_whales(limit=limit, offset=offset, sort="value")


@app.get("/api/trades")
def api_get_trades(limit: int = 100, offset: int = 0):
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


@app.get("/signals", response_model=List[SignalResponse])
def get_signals_api(
    limit: int = 50,
    offset: int = 0,
    current_user: dict = Depends(get_optional_user)
):
    tier = "free"
    if current_user:
        tier = _resolve_tier_for_user(current_user["id"])
    rows = get_signals(limit=limit, offset=offset)
    response = []
    for row in rows:
        required = row["tier_required"]
        locked = _is_signal_locked(required, tier)
        evidence = None
        if row["evidence_json"]:
            evidence = SignalEvidence(**json.loads(row["evidence_json"]))
        response.append(
            SignalResponse(
                id=row["id"],
                title=row["title"],
                content=None if locked else row["content"],
                locked=locked,
                tierRequired=required,
                createdAt=row["created_at"],
                evidence=evidence
            )
        )
    return response


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
        PaywallPlan(id="free", name="Free", price=0, currency="CNY", period="month", trialDays=0),
        PaywallPlan(id="pro_monthly", name="Pro Monthly", price=49, currency="CNY", period="month", trialDays=7),
        PaywallPlan(id="pro_yearly", name="Pro Yearly", price=399, currency="CNY", period="year", trialDays=7)
    ]
    return PaywallResponse(plans=plans)


@app.post("/trial/start", response_model=TrialStartResponse)
def start_trial(current_user: dict = Depends(get_current_user)):
    start_at = datetime.utcnow()
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
        deliver_at = datetime.utcnow() + timedelta(seconds=delay_seconds)
        enqueue_notification_job(payload.userId, payload.signalId, deliver_at)
        return {"status": "queued" if delay_seconds == 0 else "delayed"}
    if delay_seconds > 0:
        return {"status": "delayed"}
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
    for _ in range(5):
        candidate = secrets.token_urlsafe(6).replace("-", "").replace("_", "")[:8]
        try:
            insert_referral_code(current_user["id"], candidate)
            code = candidate
            break
        except Exception:
            continue
    if not code:
        raise HTTPException(status_code=500, detail="Failed to generate referral code")
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
    return MetricsResponse(**get_metrics_counts())


@app.get("/api/smart")
def api_get_smart_wallets(limit: int = 50, offset: int = 0):
    session = get_session()
    try:
        rows = session.query(SmartWallet).order_by(SmartWallet.profit.desc()).offset(offset).limit(limit).all()
        if rows:
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
        if whale_rows:
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
    conn = get_db_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(
            """
            SELECT maker_address as address,
                   SUM(value_usd) as total_value,
                   COUNT(*) as total_trades
            FROM whale_trades
            GROUP BY maker_address
            ORDER BY total_value DESC
            LIMIT ? OFFSET ?
            """,
            (limit, offset)
        )
        rows = cursor.fetchall()
        return [
            {
                "address": row["address"],
                "profit": row["total_value"],
                "roi": 0.0,
                "win_rate": 0.0,
                "total_trades": row["total_trades"]
            }
            for row in rows
        ]
    finally:
        conn.close()


@app.post("/api/refresh")
def api_refresh():
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
    start_at = datetime.utcnow()
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
        currency="CNY",
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
    entitlements = _build_entitlements_response("pro")
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
        if status_value in ["active", "grace"] and end_at < datetime.utcnow():
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
    return _build_entitlements_response(tier)

@app.post("/billing/webhook")
def billing_webhook(payload: BillingWebhookRequest):
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
        now_str = datetime.utcnow().isoformat()
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
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}


@app.post("/monitor/alert")
def monitor_alert(payload: MonitorAlertRequest):
    level = payload.level.strip().lower()
    message = payload.message
    source = payload.source or "unknown"
    if level == "error":
        logger.error(f"[monitor:{source}] {message}")
    elif level == "warn" or level == "warning":
        logger.warning(f"[monitor:{source}] {message}")
    else:
        logger.info(f"[monitor:{source}] {message}")
    return {"status": "ok"}
