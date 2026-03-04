#!/usr/bin/env python3
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from app.database import get_db_connection

def check_signals_table_schema():
    print("🔍 Checking signals table schema...")
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Get schema information for signals table
        cursor.execute("PRAGMA table_info(signals)")
        columns = cursor.fetchall()
        
        print("📊 Signals table columns:")
        for col in columns:
            print(f"   {col['name']} ({col['type']})")
        
        # Check if evidence_json column exists
        evidence_col = any(col['name'] == 'evidence_json' for col in columns)
        print(f"\n🔍 evidence_json column exists: {evidence_col}")
        
        # Check what columns actually exist that might contain evidence data
        evidence_like_cols = [col['name'] for col in columns if 'evidence' in col['name'].lower() or 'json' in col['name'].lower()]
        print(f"🔍 Evidence-related columns: {evidence_like_cols}")
        
        conn.close()
        return evidence_col, evidence_like_cols
        
    except Exception as e:
        print(f"❌ Error checking schema: {e}")
        import traceback
        traceback.print_exc()
        return False, []

def check_sample_signals_data():
    print("\n🔍 Checking sample signals data...")
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Get a few sample records to understand the data structure
        cursor.execute("SELECT * FROM signals LIMIT 3")
        samples = cursor.fetchall()
        
        print("📊 Sample signals records:")
        for i, sample in enumerate(samples):
            print(f"\n   Record {i+1}:")
            for key in sample.keys():
                if sample[key] is not None:
                    print(f"     {key}: {sample[key]}")
        
        conn.close()
        return True
        
    except Exception as e:
        print(f"❌ Error checking sample data: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("Starting database schema analysis...")
    
    evidence_exists, evidence_cols = check_signals_table_schema()
    check_sample_signals_data()
    
    if not evidence_exists:
        print(f"\n❌ The evidence_json column does not exist in the signals table.")
        print(f"💡 Available evidence-related columns: {evidence_cols}")
        print(f"🔧 This explains why the statistical endpoints are failing.")
    else:
        print(f"\n✅ evidence_json column exists - the issue might be elsewhere.")