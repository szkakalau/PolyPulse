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
        
        # Create alerts table
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

        # Create users table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
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
        
        alerts = []
        for row in rows:
            alerts.append({
                "timestamp": row["timestamp"],
                "market_question": row["market_question"],
                "outcome": row["outcome"],
                "old_price": row["old_price"],
                "new_price": row["new_price"],
                "change": row["change"],
                "message": row["message"]
            })
        return alerts
    except Exception as e:
        logger.error(f"Failed to fetch alerts from DB: {e}")
        return []

def create_user(email: str, password_hash: str) -> Optional[Dict]:
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute('INSERT INTO users (email, password_hash) VALUES (?, ?)', (email, password_hash))
        user_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return {"id": user_id, "email": email}
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
            return {
                "id": row["id"],
                "email": row["email"],
                "password_hash": row["password_hash"],
                "created_at": row["created_at"]
            }
        return None
    except Exception as e:
        logger.error(f"Failed to fetch user by email: {e}")
        return None
