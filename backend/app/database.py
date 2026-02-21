import sqlite3
import logging
from typing import List, Dict, Optional
import os

logger = logging.getLogger(__name__)

DB_PATH = "polypulse.db"

def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Alerts
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS alerts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
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
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        # Watchlists
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS watchlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                market_id TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, market_id),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        # FCM Tokens
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS fcm_tokens (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                token TEXT UNIQUE NOT NULL,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        # Whale Trades
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS whale_trades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
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

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS subscriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
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

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS entitlements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tier TEXT NOT NULL,
                feature_key TEXT NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                quota INTEGER,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(tier, feature_key)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS feature_flags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                feature_key TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 0,
                tier TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(feature_key, tier)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_entitlements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                tier TEXT NOT NULL,
                effective_at TEXT NOT NULL,
                expires_at TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
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

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS signals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                tier_required TEXT NOT NULL DEFAULT 'free',
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS daily_pulse (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                summary TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS analytics_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                event_name TEXT NOT NULL,
                properties TEXT,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS referral_codes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                code TEXT NOT NULL UNIQUE,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS referral_redemptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL,
                referrer_user_id INTEGER NOT NULL,
                referee_user_id INTEGER NOT NULL UNIQUE,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(referrer_user_id) REFERENCES users(id),
                FOREIGN KEY(referee_user_id) REFERENCES users(id)
            )
        ''')

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
        
        conn.commit()
        conn.close()
        logger.info("Database initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")

def save_alert(alert: Dict):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('''
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
        cursor.execute('SELECT * FROM alerts ORDER BY timestamp DESC LIMIT ?', (limit,))
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
        cursor.execute('''
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
        
        cursor.execute('''
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
        cursor.execute('INSERT INTO users (email, password_hash) VALUES (?, ?)', (email, password_hash))
        user_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return user_id
    except sqlite3.IntegrityError:
        logger.warning(f"User with email {email} already exists")
        return None
    except Exception as e:
        logger.error(f"Failed to create user: {e}")
        return None

def get_user_by_email(email: str) -> Optional[Dict]:
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM users WHERE email = ?', (email,))
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
    cursor.execute(
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
    cursor.execute(
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
    cursor.execute(
        '''
        INSERT OR IGNORE INTO transactions (
            user_id, platform, order_id, product_id, purchase_token,
            purchase_state, amount, currency, purchased_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''',
        (
            user_id,
            platform,
            order_id,
            product_id,
            purchase_token,
            purchase_state,
            amount,
            currency,
            purchased_at
        )
    )
    conn.commit()
    conn.close()

def get_transaction_user_id(order_id: str) -> Optional[int]:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        'SELECT user_id FROM transactions WHERE order_id = ? LIMIT 1',
        (order_id,)
    )
    row = cursor.fetchone()
    conn.close()
    return row["user_id"] if row else None

def set_user_entitlements(user_id: int, tier: str, effective_at: str, expires_at: str) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
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
    cursor.execute(
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
    cursor.execute(
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
    cursor.execute(
        '''
        SELECT id, title, content, tier_required, created_at
        FROM signals
        ORDER BY created_at DESC
        LIMIT ? OFFSET ?
        ''',
        (limit, offset)
    )
    rows = cursor.fetchall()
    conn.close()
    return [dict(row) for row in rows]

def get_signal_by_id(signal_id: int) -> Optional[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        '''
        SELECT id, title, content, tier_required, created_at
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
    cursor.execute(
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

def get_fcm_tokens_for_user(user_id: int) -> List[str]:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        '''
        SELECT token FROM fcm_tokens
        WHERE user_id = ?
        ''',
        (user_id,)
    )
    rows = cursor.fetchall()
    conn.close()
    return [row["token"] for row in rows]

def save_analytics_event(user_id: Optional[int], event_name: str, properties: Optional[str]) -> None:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
        '''
        INSERT INTO analytics_events (user_id, event_name, properties)
        VALUES (?, ?, ?)
        ''',
        (user_id, event_name, properties)
    )
    conn.commit()
    conn.close()

def get_daily_pulse(limit: int = 20, offset: int = 0) -> List[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
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
    cursor.execute(
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
    cursor.execute(
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
    cursor.execute(
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
        cursor.execute(
            '''
            INSERT INTO referral_redemptions (code, referrer_user_id, referee_user_id)
            VALUES (?, ?, ?)
            ''',
            (code, referrer_user_id, referee_user_id)
        )
        conn.commit()
        conn.close()
        return True
    except sqlite3.IntegrityError:
        conn.close()
        return False

def get_feature_flags(tier: str) -> List[Dict]:
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(
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
    cursor.execute("SELECT COUNT(*) as count FROM users")
    users_count = cursor.fetchone()["count"]
    cursor.execute("SELECT COUNT(*) as count FROM subscriptions")
    subscriptions_count = cursor.fetchone()["count"]
    cursor.execute("SELECT COUNT(*) as count FROM signals")
    signals_count = cursor.fetchone()["count"]
    cursor.execute("SELECT COUNT(*) as count FROM alerts")
    alerts_count = cursor.fetchone()["count"]
    cursor.execute("SELECT COUNT(*) as count FROM daily_pulse")
    daily_pulse_count = cursor.fetchone()["count"]
    conn.close()
    return {
        "users": users_count,
        "subscriptions": subscriptions_count,
        "signals": signals_count,
        "alerts": alerts_count,
        "daily_pulse": daily_pulse_count
    }
