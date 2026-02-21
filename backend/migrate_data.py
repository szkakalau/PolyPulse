import sqlite3
import psycopg2
import psycopg2.extras
import os
import sys
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Config
SQLITE_DB_PATH = "polypulse.db"
# Default to local mapping if not provided
DATABASE_URL = os.environ.get("DATABASE_URL")

def migrate_data():
    if not DATABASE_URL:
        logger.error("DATABASE_URL environment variable is not set.")
        logger.error("Usage: DATABASE_URL=postgresql://... python3 migrate_data.py")
        sys.exit(1)

    # Fix Railway URL if needed
    pg_url = DATABASE_URL
    if pg_url.startswith("postgres://"):
        pg_url = pg_url.replace("postgres://", "postgresql://", 1)

    if not os.path.exists(SQLITE_DB_PATH):
        logger.error(f"SQLite database not found at {SQLITE_DB_PATH}")
        sys.exit(1)

    logger.info("Connecting to databases...")
    
    # Connect SQLite
    try:
        sqlite_conn = sqlite3.connect(SQLITE_DB_PATH)
        sqlite_conn.row_factory = sqlite3.Row
        sqlite_cursor = sqlite_conn.cursor()
        logger.info("Connected to SQLite.")
    except Exception as e:
        logger.error(f"Failed to connect to SQLite: {e}")
        sys.exit(1)

    # Connect Postgres
    try:
        pg_conn = psycopg2.connect(pg_url)
        pg_cursor = pg_conn.cursor()
        logger.info("Connected to Postgres.")
    except Exception as e:
        logger.error(f"Failed to connect to Postgres: {e}")
        sys.exit(1)

    # Tables to migrate (in order of dependencies)
    tables = [
        "users",
        "watchlists",
        "fcm_tokens",
        "subscriptions",
        "entitlements",
        "user_entitlements",
        "feature_flags",
        "alerts",
        "signals",
        "daily_pulse",
        "analytics_events",
        "referral_codes",
        "referral_redemptions",
        "whale_trades", # This might be large, be careful
        "transactions"
    ]

    try:
        for table in tables:
            logger.info(f"Migrating table: {table}...")
            
            # 1. Check if table exists in SQLite
            sqlite_cursor.execute(f"SELECT name FROM sqlite_master WHERE type='table' AND name='{table}'")
            if not sqlite_cursor.fetchone():
                logger.warning(f"Table {table} does not exist in SQLite. Skipping.")
                continue

            # 2. Get data from SQLite
            sqlite_cursor.execute(f"SELECT * FROM {table}")
            rows = sqlite_cursor.fetchall()
            
            if not rows:
                logger.info(f"Table {table} is empty. Skipping.")
                continue
            
            logger.info(f"Found {len(rows)} rows in {table}.")

            # 3. Insert into Postgres
            # We need to construct the INSERT statement dynamically based on columns
            columns = rows[0].keys()
            cols_str = ', '.join(columns)
            vals_str = ', '.join(['%s'] * len(columns))
            
            query = f"INSERT INTO {table} ({cols_str}) VALUES ({vals_str}) ON CONFLICT DO NOTHING"
            
            # Batch insert
            data = [tuple(row) for row in rows]
            
            try:
                psycopg2.extras.execute_batch(pg_cursor, query, data)
                pg_conn.commit()
                logger.info(f"Successfully migrated {len(rows)} rows for {table}.")
            except Exception as e:
                pg_conn.rollback()
                logger.error(f"Failed to migrate table {table}: {e}")
                # Optional: Continue to next table or exit?
                # For now, let's continue
                
    except Exception as e:
        logger.error(f"Migration failed: {e}")
    finally:
        sqlite_conn.close()
        pg_conn.close()
        logger.info("Migration connection closed.")

if __name__ == "__main__":
    migrate_data()
