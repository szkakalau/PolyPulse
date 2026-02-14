import sqlite3
import sys
import os

# Add current directory to path
sys.path.append(os.getcwd())

from app.database import get_db_connection

def check_db():
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT count(*) FROM whale_trades")
    count = cursor.fetchone()[0]
    print(f"Total whale trades in DB: {count}")
    
    if count > 0:
        cursor.execute("SELECT * FROM whale_trades LIMIT 5")
        rows = cursor.fetchall()
        print("Sample trades:")
        for r in rows:
            print(dict(r))
            
    conn.close()

if __name__ == "__main__":
    check_db()
