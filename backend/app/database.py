import sqlite3
import logging
from typing import List, Dict, Optional, Any
import os
import json
from datetime import datetime, timedelta

try:
    import psycopg2
    import psycopg2.extras
except ImportError:
    psycopg2 = None

logger = logging.getLogger(__name__)

DB_PATH = "polypulse.db"
DATABASE_URL = os.environ.get("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

IS_POSTGRES = bool(DATABASE_URL) and (psycopg2 is not None)

def get_db_connection():
    if IS_POSTGRES:
        return psycopg2.connect(DATABASE_URL, cursor_factory=psycopg2.extras.RealDictCursor)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def execute_sql(cursor, query: str, params: tuple = ()) -> None:
    if IS_POSTGRES:
        query = query.replace("?", "%s")
    cursor.execute(query, params)

def init_db():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        pk_type = "SERIAL PRIMARY KEY" if IS_POSTGRES else "INTEGER PRIMARY KEY AUTOINCREMENT"
        
        # Alerts
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS alerts (
                id {pk_type},
                timestamp TEXT NOT NULL,
                market_question TEXT NOT NULL,
                outcome TEXT NOT NULL,
                old_price REAL NOT NULL,
                new_price REAL NOT NULL,
                change REAL NOT NULL,
                message TEXT NOT NULL
            )
        ''')

        # Users
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS users (
                id {pk_type},
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # Watchlists
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS watchlists (
                id {pk_type},
                user_id INTEGER NOT NULL,
                market_id TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, market_id),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        # FCM Tokens
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS fcm_tokens (
                id {pk_type},
                user_id INTEGER,
                token TEXT UNIQUE NOT NULL,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS notification_settings (
                id {pk_type},
                user_id INTEGER NOT NULL UNIQUE,
                push_enabled INTEGER NOT NULL DEFAULT 1,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        # Whale Trades
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS whale_trades (
                id {pk_type},
                timestamp INTEGER NOT NULL,
                maker_address TEXT NOT NULL,
                market_question TEXT NOT NULL,
                outcome TEXT NOT NULL,
                side TEXT NOT NULL,
                size REAL NOT NULL,
                price REAL NOT NULL,
                value_usd REAL NOT NULL,
                market_slug TEXT,
                UNIQUE(maker_address, timestamp, market_slug, value_usd)
            )
        ''')

        # Subscriptions
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS subscriptions (
                id {pk_type},
                user_id INTEGER NOT NULL,
                platform TEXT NOT NULL,
                plan_id TEXT NOT NULL,
                status TEXT NOT NULL,
                start_at TEXT NOT NULL,
                end_at TEXT NOT NULL,
                auto_renew INTEGER NOT NULL DEFAULT 1,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, platform, plan_id)
            )
        ''')

        # Entitlements
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS entitlements (
                id {pk_type},
                tier TEXT NOT NULL,
                feature_key TEXT NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                quota INTEGER,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tier, feature_key)
            )
        ''')

        # Feature Flags
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS feature_flags (
                id {pk_type},
                feature_key TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 0,
                tier TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(feature_key, tier)
            )
        ''')

        # User Entitlements
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS user_entitlements (
                id {pk_type},
                user_id INTEGER NOT NULL,
                tier TEXT NOT NULL,
                effective_at TEXT NOT NULL,
                expires_at TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # Transactions
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS transactions (
                id {pk_type},
                user_id INTEGER NOT NULL,
                platform TEXT NOT NULL,
                order_id TEXT NOT NULL,
                product_id TEXT NOT NULL,
                purchase_token TEXT NOT NULL,
                purchase_state TEXT NOT NULL,
                amount INTEGER NOT NULL,
                currency TEXT NOT NULL,
                purchased_at TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(platform, order_id)
            )
        ''')

        # Signals
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS signals (
                id {pk_type},
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                tier_required TEXT NOT NULL DEFAULT 'free',
                evidence_json TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # Daily Pulse
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS daily_pulse (
                id {pk_type},
                title TEXT NOT NULL,
                summary TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # Analytics Events
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS analytics_events (
                id {pk_type},
                user_id INTEGER,
                event_name TEXT NOT NULL,
                properties TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # Referral Codes
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS referral_codes (
                id {pk_type},
                user_id INTEGER NOT NULL,
                code TEXT NOT NULL UNIQUE,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        # Referral Redemptions
        cursor.execute(f'''
            CREATE TABLE IF NOT EXISTS referral_redemptions (
                id {pk_type},
                code TEXT NOT NULL,
                referrer_user_id INTEGER NOT NULL,
                referee_user_id INTEGER NOT NULL UNIQUE,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(referrer_user_id) REFERENCES users(id),
                FOREIGN KEY(referee_user_id) REFERENCES users(id)
            )
        ''')

        # Initial Data
        if IS_POSTGRES:
            insert_ignore_entitlements = '''
            INSERT INTO entitlements (tier, feature_key, is_enabled, quota)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (tier, feature_key) DO NOTHING
            '''
            cursor.executemany(insert_ignore_entitlements, [
                ("free", "alerts_basic", 1, 2),
                ("free", "alerts_pro", 0, None),
                ("free", "history_performance", 0, None),
                ("free", "low_latency", 0, None),
                ("pro", "alerts_basic", 1, 999),
                ("pro", "alerts_pro", 1, 999),
                ("pro", "history_performance", 1, None),
                ("pro", "low_latency", 1, None)
            ])
            
            insert_ignore_flags = '''
            INSERT INTO feature_flags (feature_key, enabled, tier)
            VALUES (%s, %s, %s)
            ON CONFLICT (feature_key, tier) DO NOTHING
            '''
            cursor.executemany(insert_ignore_flags, [
                ("high_value_signal", 0, "free"),
                ("high_value_signal", 1, "pro")
            ])
        else:
            cursor.executemany(
                '''
                INSERT OR IGNORE INTO entitlements (tier, feature_key, is_enabled, quota)
                VALUES (?, ?, ?, ?)
                ''',
                [
                    ("free", "alerts_basic", 1, 2),
                    ("free", "alerts_pro", 0, None),
                    ("free", "history_performance", 0, None),
                    ("free", "low_latency", 0, None),
                    ("pro", "alerts_basic", 1, 999),
                    ("pro", "alerts_pro", 1, 999),
                    ("pro", "history_performance", 1, None),
                    ("pro", "low_latency", 1, None)
                ]
            )
    
            cursor.executemany(
                '''
                INSERT OR IGNORE INTO feature_flags (feature_key, enabled, tier)
                VALUES (?, ?, ?)
                ''',
                [
                    ("high_value_signal", 0, "free"),
                    ("high_value_signal", 1, "pro")
                ]
            )

        if os.environ.get("SEED_DEMO_DATA", "0") == "1":
            execute_sql(cursor, 'SELECT COUNT(*) as count FROM signals')
            row = cursor.fetchone()
            existing_count = int(row["count"] or 0) if row else 0
            if existing_count == 0:
                now = datetime.utcnow()
                evidence_items = [
                    {
                        "sourceType": "whale_trade",
                        "triggeredAt": now.strftime("%Y-%m-%d %H:%M:%S"),
                        "marketId": "trump-win-2028",
                        "makerAddress": "0x9a7f3b4e2d1c0f9a7f3b4e2d1c0f9a7f3b4e2d1c",
                        "evidenceUrl": "https://polymarket.com/event/trump-win-2028",
                        "dedupeKey": "seed-whale-trump-1"
                    },
                    {
                        "sourceType": "momentum_alert",
                        "triggeredAt": (now - timedelta(hours=3)).strftime("%Y-%m-%d %H:%M:%S"),
                        "marketId": "fed-cuts-march",
                        "makerAddress": "0x4f2a1c9b8d7e6f5a4f2a1c9b8d7e6f5a4f2a1c9b",
                        "evidenceUrl": "https://polymarket.com/event/fed-cuts-march",
                        "dedupeKey": "seed-momentum-fed-1"
                    },
                    {
                        "sourceType": "order_flow",
                        "triggeredAt": (now - timedelta(days=1)).strftime("%Y-%m-%d %H:%M:%S"),
                        "marketId": "btc-100k-2025",
                        "makerAddress": "0x2c8f6a1d3b4e5f6a7c8d9e0f1a2b3c4d5e6f7a8b",
                        "evidenceUrl": "https://polymarket.com/event/btc-100k-2025",
                        "dedupeKey": "seed-orderflow-btc-1"
                    }
                ]
                seed_signals = [
                    (
                        "鲸鱼账户大额押注 YES",
                        "巨鲸在该市场单笔买入金额显著高于近 30 日均值，疑似提前布局。",
                        "free",
                        json.dumps(evidence_items[0])
                    ),
                    (
                        "高频动量信号：价格快速上行",
                        "过去 15 分钟内价格跃升并伴随成交量放大，短线趋势偏多。",
                        "pro",
                        json.dumps(evidence_items[1])
                    ),
                    (
                        "资金流入聚集：订单簿失衡",
                        "买方挂单显著占优，盘口失衡提示潜在方向性机会。",
                        "free",
                        json.dumps(evidence_items[2])
                    )
                ]
                for title, content, tier_required, evidence_json in seed_signals:
                    execute_sql(
                        cursor,
                        '''
                        INSERT INTO signals (title, content, tier_required, evidence_json, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        ''',
                        (title, content, tier_required, evidence_json, now.strftime("%Y-%m-%d %H:%M:%S"))
                    )
        
        conn.commit()
        conn.close()
        logger.info("Database initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")

def save_alert(alert: Dict):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        execute_sql(cursor, '''
            INSERT INTO alerts (timestamp, market_question, outcome, old_price, new_price, change, message)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (
            alert['timestamp'],
            alert['market_question'],
            alert['outcome'],
            alert['old_price'],
            alert['new_price'],
            alert['change'],
            alert['message']
        ))
        conn.commit()
        conn.close()
    except Exception as e:
        logger.error(f"Failed to save alert to DB: {e}")

def get_recent_alerts(limit: int = 50) -> List[Dict]:
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        execute_sql(cursor, 'SELECT * FROM alerts ORDER BY timestamp DESC LIMIT ?', (limit,))
        rows = cursor.fetchall()
        conn.close()
        return [dict(row) for row in rows]
    except Exception as e:
        logger.error(f"Failed to get alerts: {e}")
        return []

def save_whale_trade(trade: Dict):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        if IS_POSTGRES:
            execute_sql(cursor, '''
                INSERT INTO whale_trades (
                    timestamp, maker_address, market_question, outcome, side, size, price, value_usd, market_slug
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (maker_address, timestamp, market_slug, value_usd) DO NOTHING
            ''', (
                trade['timestamp'],
                trade['maker_address'],
                trade['market_question'],
                trade['outcome'],
                trade['side'],
                trade['size'],
                trade['price'],
                trade['value_usd'],
                trade['market_slug']
            ))
        else:
            execute_sql(cursor, '''
                INSERT OR IGNORE INTO whale_trades (
                    timestamp, maker_address, market_question, outcome, side, size, price, value_usd, market_slug
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                trade['timestamp'],
                trade['maker_address'],
                trade['market_question'],
                trade['outcome'],
                trade['side'],
                trade['size'],
                trade['price'],
                trade['value_usd'],
                trade['market_slug']
            ))
        conn.commit()
        conn.close()
    except Exception as e:
        logger.error(f"Failed to save whale trade to DB: {e}")

def get_leaderboard_stats(limit: int = 10) -> List[Dict]:
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        execute_sql(cursor, '''
            SELECT 
                maker_address,
                SUM(value_usd) as total_volume,
                COUNT(*) as trade_count,
                MAX(value_usd) as max_trade_value,
                SUM(CASE WHEN side = 'BUY' THEN value_usd ELSE 0 END) as buy_volume,
                SUM(CASE WHEN side = 'SELL' THEN value_usd ELSE 0 END) as sell_volume
            FROM whale_trades
            GROUP BY maker_address
            ORDER BY total_volume DESC
            LIMIT ?
        ''', (limit,))
        
        rows = cursor.fetchall()
        conn.close()
        return [dict(row) for row in rows]
    except Exception as e:
        logger.error(f"Failed to get leaderboard stats: {e}")
        return []

def create_user(email: str, password_hash: str) -> Optional[int]:
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        if IS_POSTGRES:
            cursor.execute('INSERT INTO users (email, password_hash) VALUES (%s, %s) RETURNING id', (email, password_hash))
            user_id = cursor.fetchone()['id']
        else:
            cursor.execute('INSERT INTO users (email, password_hash) VALUES (?, ?)', (email, password_hash))
            user_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return user_id
    except Exception as e:
        # Check for integrity error in a generic way or import specific exceptions
        if "UNIQUE constraint failed" in str(e) or "duplicate key value" in str(e):
             logger.warning(f"User with email {email} already exists")
             return None
        logger.error(f"Failed to create user: {e}")
        return None

def get_user_by_email(email: str) -> Optional[Dict]:
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        execute_sql(cursor, 'SELECT * FROM users WHERE email = ?', (email,))
        row = cursor.fetchone()
        conn.close()
        if row:
            return dict(row)
        return None
    except Exception as e:
        logger.error(f"Failed to get user by email: {e}")
        return None

def upsert_subscription(
    user_id: int,
    platform: str,
    plan_id: str,
    status: str,
    start_at: str,
    end_at: str,
    auto_renew: bool
) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # SQLite and Postgres have slightly different ON CONFLICT syntax when using excluded
    # SQLite: excluded.column
    # Postgres: EXCLUDED.column
    # They are case insensitive, so excluded.column works in Postgres too.
    
    execute_sql(cursor,
        '''
        INSERT INTO subscriptions (user_id, platform, plan_id, status, start_at, end_at, auto_renew)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(user_id, platform, plan_id) DO UPDATE SET
            status=excluded.status,
            start_at=excluded.start_at,
            end_at=excluded.end_at,
            auto_renew=excluded.auto_renew,
            updated_at=CURRENT_TIMESTAMP
        ''',
        (user_id, platform, plan_id, status, start_at, end_at, 1 if auto_renew else 0)
    )
    conn.commit()
    conn.close()

def get_latest_subscription(user_id: int) -> Optional[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT * FROM subscriptions
        WHERE user_id = ?
        ORDER BY updated_at DESC
        LIMIT 1
        ''',
        (user_id,)
    )
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else None

def save_transaction(
    user_id: int,
    platform: str,
    order_id: str,
    product_id: str,
    purchase_token: str,
    purchase_state: str,
    amount: int,
    currency: str,
    purchased_at: str
) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    if IS_POSTGRES:
        execute_sql(cursor,
            '''
            INSERT INTO transactions (
                user_id, platform, order_id, product_id, purchase_token,
                purchase_state, amount, currency, purchased_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (platform, order_id) DO NOTHING
            ''',
            (
                user_id, platform, order_id, product_id, purchase_token,
                purchase_state, amount, currency, purchased_at
            )
        )
    else:
        execute_sql(cursor,
            '''
            INSERT OR IGNORE INTO transactions (
                user_id, platform, order_id, product_id, purchase_token,
                purchase_state, amount, currency, purchased_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''',
            (
                user_id, platform, order_id, product_id, purchase_token,
                purchase_state, amount, currency, purchased_at
            )
        )
    conn.commit()
    conn.close()

def get_transaction_user_id(order_id: str) -> Optional[int]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        'SELECT user_id FROM transactions WHERE order_id = ? LIMIT 1',
        (order_id,)
    )
    row = cursor.fetchone()
    conn.close()
    return row["user_id"] if row else None

def set_user_entitlements(user_id: int, tier: str, effective_at: str, expires_at: str) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        INSERT INTO user_entitlements (user_id, tier, effective_at, expires_at)
        VALUES (?, ?, ?, ?)
        ''',
        (user_id, tier, effective_at, expires_at)
    )
    conn.commit()
    conn.close()

def get_latest_user_entitlement(user_id: int) -> Optional[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT * FROM user_entitlements
        WHERE user_id = ?
        ORDER BY created_at DESC
        LIMIT 1
        ''',
        (user_id,)
    )
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else None

def get_entitlements_for_tier(tier: str) -> List[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT feature_key, is_enabled, quota
        FROM entitlements
        WHERE tier = ?
        ORDER BY feature_key
        ''',
        (tier,)
    )
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def get_signals(limit: int = 50, offset: int = 0) -> List[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT id, title, content, tier_required, evidence_json, created_at
        FROM signals
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        ''',
        (limit, offset)
    )
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def create_signal(title: str, content: str, tier_required: str = "free", evidence_json: Optional[str] = None) -> int:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(
        cursor,
        '''
        INSERT INTO signals (title, content, tier_required, evidence_json)
        VALUES (?, ?, ?, ?)
        ''',
        (title, content, tier_required, evidence_json)
    )
    conn.commit()
    signal_id = None
    try:
        signal_id = cursor.lastrowid
    except Exception:
        pass
    if IS_POSTGRES and signal_id is None:
        execute_sql(cursor, 'SELECT LASTVAL() as id')
        row = cursor.fetchone()
        signal_id = row["id"] if row else 0
    conn.close()
    return int(signal_id or 0)

def get_signal_by_id(signal_id: int) -> Optional[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT id, title, content, tier_required, evidence_json, created_at
        FROM signals
        WHERE id = ?
        ''',
        (signal_id,)
    )
    row = cursor.fetchone()
    conn.close()
    return dict(row) if row else None

def upsert_fcm_token(user_id: int, token: str) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        INSERT INTO fcm_tokens (user_id, token, updated_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(token) DO UPDATE SET
            user_id=excluded.user_id,
            updated_at=CURRENT_TIMESTAMP
        ''',
        (user_id, token)
    )
    conn.commit()
    conn.close()

def get_notification_settings(user_id: int) -> Dict[str, Any]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(
        cursor,
        '''
        SELECT push_enabled
        FROM notification_settings
        WHERE user_id = ?
        ''',
        (user_id,)
    )
    row = cursor.fetchone()
    conn.close()
    if not row:
        return {"push_enabled": True}
    value = row["push_enabled"]
    return {"push_enabled": bool(value)}

def set_notification_settings(user_id: int, push_enabled: bool) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(
        cursor,
        '''
        INSERT INTO notification_settings (user_id, push_enabled, updated_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(user_id) DO UPDATE SET
            push_enabled=excluded.push_enabled,
            updated_at=CURRENT_TIMESTAMP
        ''',
        (user_id, 1 if push_enabled else 0)
    )
    conn.commit()
    conn.close()

def get_fcm_tokens_for_user(user_id: int) -> List[str]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT token FROM fcm_tokens
        WHERE user_id = ?
        ''',
        (user_id,)
    )
    rows = cursor.fetchall()
    conn.close()
    return [row["token"] for row in rows]

def get_user_ids_with_fcm_tokens() -> List[int]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(
        cursor,
        '''
        SELECT DISTINCT user_id
        FROM fcm_tokens
        WHERE user_id IS NOT NULL
        ORDER BY user_id
        '''
    )
    rows = cursor.fetchall()
    conn.close()
    return [int(row["user_id"]) for row in rows if row["user_id"] is not None]

def save_analytics_event(user_id: Optional[int], event_name: str, properties: Optional[str]) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        INSERT INTO analytics_events (user_id, event_name, properties)
        VALUES (?, ?, ?)
        ''',
        (user_id, event_name, properties)
    )
    conn.commit()
    conn.close()

def has_recent_analytics_event(user_id: int, event_name: str, since_hours: int) -> bool:
    conn = get_db_connection()
    cursor = conn.cursor()
    since_ts = (datetime.utcnow() - timedelta(hours=since_hours)).strftime("%Y-%m-%d %H:%M:%S")
    execute_sql(
        cursor,
        '''
        SELECT COUNT(*) as count
        FROM analytics_events
        WHERE user_id = ? AND event_name = ? AND created_at >= ?
        ''',
        (user_id, event_name, since_ts)
    )
    row = cursor.fetchone()
    conn.close()
    return (row["count"] if row else 0) > 0

def get_signal_stats(days: int = 7) -> Dict[str, int]:
    conn = get_db_connection()
    cursor = conn.cursor()
    since_ts = (datetime.utcnow() - timedelta(days=days)).strftime("%Y-%m-%d %H:%M:%S")
    execute_sql(
        cursor,
        '''
        SELECT
            COUNT(*) as signals_count,
            SUM(CASE WHEN evidence_json IS NOT NULL AND evidence_json != '' THEN 1 ELSE 0 END) as evidence_count
        FROM signals
        WHERE created_at >= ?
        ''',
        (since_ts,)
    )
    row = cursor.fetchone()
    conn.close()
    return {
        "signals_7d": int(row["signals_count"] or 0) if row else 0,
        "evidence_7d": int(row["evidence_count"] or 0) if row else 0
    }

def get_watchlist(user_id: int) -> List[str]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(
        cursor,
        '''
        SELECT market_id
        FROM watchlists
        WHERE user_id = ?
        ORDER BY created_at DESC
        ''',
        (user_id,)
    )
    rows = cursor.fetchall()
    conn.close()
    if not rows:
        return []
    return [row["market_id"] for row in rows]

def add_to_watchlist(user_id: int, market_id: str) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    if IS_POSTGRES:
        execute_sql(
            cursor,
            '''
            INSERT INTO watchlists (user_id, market_id)
            VALUES (?, ?)
            ON CONFLICT (user_id, market_id) DO NOTHING
            ''',
            (user_id, market_id)
        )
    else:
        execute_sql(
            cursor,
            '''
            INSERT OR IGNORE INTO watchlists (user_id, market_id)
            VALUES (?, ?)
            ''',
            (user_id, market_id)
        )
    conn.commit()
    conn.close()

def remove_from_watchlist(user_id: int, market_id: str) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(
        cursor,
        '''
        DELETE FROM watchlists
        WHERE user_id = ? AND market_id = ?
        ''',
        (user_id, market_id)
    )
    conn.commit()
    conn.close()

def get_daily_pulse(limit: int = 20, offset: int = 0) -> List[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT id, title, summary, content, created_at
        FROM daily_pulse
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        ''',
        (limit, offset)
    )
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def get_referral_code(user_id: int) -> Optional[str]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT code FROM referral_codes
        WHERE user_id = ?
        LIMIT 1
        ''',
        (user_id,)
    )
    row = cursor.fetchone()
    conn.close()
    return row["code"] if row else None

def insert_referral_code(user_id: int, code: str) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        INSERT INTO referral_codes (user_id, code)
        VALUES (?, ?)
        ''',
        (user_id, code)
    )
    conn.commit()
    conn.close()

def redeem_referral_code(code: str, referee_user_id: int) -> bool:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT user_id FROM referral_codes
        WHERE code = ?
        ''',
        (code,)
    )
    row = cursor.fetchone()
    if not row:
        conn.close()
        return False
    referrer_user_id = row["user_id"]
    if referrer_user_id == referee_user_id:
        conn.close()
        return False
    try:
        execute_sql(cursor,
            '''
            INSERT INTO referral_redemptions (code, referrer_user_id, referee_user_id)
            VALUES (?, ?, ?)
            ''',
            (code, referrer_user_id, referee_user_id)
        )
        conn.commit()
        conn.close()
        return True
    except Exception:
        # Catch integrity error
        conn.close()
        return False

def get_feature_flags(tier: str) -> List[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor,
        '''
        SELECT feature_key, enabled
        FROM feature_flags
        WHERE tier = ?
        ORDER BY feature_key
        ''',
        (tier,)
    )
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def get_metrics_counts() -> Dict:
    conn = get_db_connection()
    cursor = conn.cursor()
    execute_sql(cursor, "SELECT COUNT(*) as count FROM users")
    users_count = cursor.fetchone()["count"]
    execute_sql(cursor, "SELECT COUNT(*) as count FROM subscriptions")
    subscriptions_count = cursor.fetchone()["count"]
    execute_sql(cursor, "SELECT COUNT(*) as count FROM signals")
    signals_count = cursor.fetchone()["count"]
    execute_sql(cursor, "SELECT COUNT(*) as count FROM alerts")
    alerts_count = cursor.fetchone()["count"]
    execute_sql(cursor, "SELECT COUNT(*) as count FROM daily_pulse")
    daily_pulse_count = cursor.fetchone()["count"]
    conn.close()
    return {
        "users": users_count,
        "subscriptions": subscriptions_count,
        "signals": signals_count,
        "alerts": alerts_count,
        "daily_pulse": daily_pulse_count
    }
