#!/usr/bin/env python3
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from app.database import get_db_connection

def check_related_tables():
    print("🔍 Checking related tables for evidence data...")
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Get all tables in database
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [row['name'] for row in cursor.fetchall()]
        
        print("📊 All tables in database:")
        for table in sorted(tables):
            print(f"   {table}")
        
        # Check signal_evaluations table specifically
        if 'signal_evaluations' in tables:
            print(f"\n🔍 Checking signal_evaluations table schema...")
            cursor.execute("PRAGMA table_info(signal_evaluations)")
            eval_columns = cursor.fetchall()
            
            print("📊 signal_evaluations table columns:")
            for col in eval_columns:
                print(f"   {col['name']} ({col['type']})")
        
        # Check for any other potential evidence storage
        evidence_tables = [table for table in tables if 'evidence' in table.lower() or 'eval' in table.lower()]
        print(f"\n🔍 Potential evidence-related tables: {evidence_tables}")
        
        conn.close()
        return tables
        
    except Exception as e:
        print(f"❌ Error checking related tables: {e}")
        import traceback
        traceback.print_exc()
        return []

if __name__ == "__main__":
    print("Analyzing database for evidence storage...")
    tables = check_related_tables()