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
