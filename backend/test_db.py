#!/usr/bin/env python3
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'backend'))

from app.database import get_db_connection

def test_database_connection():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Test simple query
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        
        print("✅ Database connection successful!")
        print(f"📊 Found {len(tables)} tables:")
        for table in tables:
            print(f"  - {table['name']}")
        
        # Test user table if exists
        cursor.execute("SELECT COUNT(*) as count FROM users")
        user_count = cursor.fetchone()
        print(f"👥 User count: {user_count['count'] if user_count else 0}")
        
        conn.close()
        return True
        
    except Exception as e:
        print(f"❌ Database connection failed: {e}")
        return False

if __name__ == "__main__":
    success = test_database_connection()
    sys.exit(0 if success else 1)