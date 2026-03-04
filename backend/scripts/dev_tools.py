"""
Development tools for PolyPulse backend
Includes database utilities, performance testing, and debugging tools
"""
import time
import json
from datetime import datetime, timedelta
from typing import Dict, Any, List
import sqlite3
import re
import pandas as pd
from app.database import get_db_connection

def analyze_query_performance() -> Dict[str, Any]:
    """
    Analyze database query performance
    Returns slow queries and optimization suggestions
    """
    conn = get_db_connection()
    
    # Get query performance metrics
    queries = conn.execute("""
        SELECT 
            query, 
            COUNT(*) as execution_count,
            AVG(duration) as avg_duration,
            MAX(duration) as max_duration,
            SUM(duration) as total_duration
        FROM query_metrics 
        WHERE timestamp > datetime('now', '-1 hour')
        GROUP BY query
        ORDER BY total_duration DESC
        LIMIT 10
    """).fetchall()
    
    # Get table statistics
    tables = conn.execute("""
        SELECT name 
        FROM sqlite_master 
        WHERE type='table'
    """).fetchall()
    
    table_stats = {}
    for table in tables:
        table_name = table[0]
        if table_name not in ['query_metrics', 'sqlite_sequence'] and re.match(r"^[A-Za-z0-9_]+$", table_name):
            count = conn.execute(f"SELECT COUNT(*) FROM {table_name}").fetchone()[0]
            table_stats[table_name] = {
                'row_count': count,
                'analyzed': datetime.now().isoformat()
            }
    
    conn.close()
    
    return {
        'slow_queries': [
            {
                'query': q[0],
                'execution_count': q[1],
                'avg_duration_ms': round(q[2] * 1000, 2),
                'max_duration_ms': round(q[3] * 1000, 2),
                'total_duration_ms': round(q[4] * 1000, 2)
            } for q in queries
        ],
        'table_statistics': table_stats,
        'timestamp': datetime.now().isoformat()
    }

def generate_performance_report() -> Dict[str, Any]:
    """
    Generate comprehensive performance report
    """
    report = {
        'database_performance': analyze_query_performance(),
        'system_metrics': {
            'timestamp': datetime.now().isoformat(),
            'python_version': '3.11+',
            'environment': 'development'
        },
        'recommendations': []
    }
    
    # Add performance recommendations
    slow_queries = report['database_performance']['slow_queries']
    for query in slow_queries:
        if query['avg_duration_ms'] > 100:  # More than 100ms is considered slow
            report['recommendations'].append({
                'type': 'query_optimization',
                'query': query['query'],
                'avg_duration_ms': query['avg_duration_ms'],
                'suggestion': 'Consider adding indexes or optimizing query logic'
            })
    
    return report

def export_to_csv(data: List[Dict[str, Any]], filename: str) -> None:
    """
    Export data to CSV for analysis
    """
    if not data:
        print("No data to export")
        return
        
    df = pd.DataFrame(data)
    df.to_csv(filename, index=False)
    print(f"Data exported to {filename}")

def clear_test_data() -> None:
    """
    Clear test data from database
    Use with caution in production!
    """
    conn = get_db_connection()
    
    # List of tables to clear (excluding system tables)
    tables_to_clear = [
        'signals', 'signal_evaluations', 'notification_attempts',
        'analytics_events', 'test_data'
    ]
    
    for table in tables_to_clear:
        try:
            # 使用参数化查询防止SQL注入 - SQLite表名不能参数化，需要白名单验证
            if table in tables_to_clear:  # 验证表名在白名单中
                conn.execute(f"DELETE FROM {table} WHERE 1=1")
                print(f"Cleared table: {table}")
            else:
                print(f"Skipping unauthorized table: {table}")
        except Exception as e:
            print(f"Error clearing {table}: {e}")
    
    conn.commit()
    conn.close()
    print("Test data cleared successfully")

if __name__ == "__main__":
    # Example usage
    report = generate_performance_report()
    print("Performance Report:")
    print(json.dumps(report, indent=2))
