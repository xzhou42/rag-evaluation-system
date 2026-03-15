#!/usr/bin/env python3
"""
调试 AnythingLLM SQLite 数据库
"""

import sqlite3
from pathlib import Path

DB_PATH = Path.home() / "Library/Application Support/anythingllm-desktop/storage/anythingllm.db"

print(f"Database path: {DB_PATH}")
print(f"Database exists: {DB_PATH.exists()}")

if DB_PATH.exists():
    try:
        conn = sqlite3.connect(str(DB_PATH))
        cursor = conn.cursor()
        
        print(f"\n✓ Connected to SQLite database")
        
        # 列出所有表
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()
        print(f"\nTables found: {len(tables)}")
        for table in tables:
            print(f"  - {table[0]}")
        
        # 查看每个表的结构和数据
        for table in tables:
            table_name = table[0]
            print(f"\n--- Table: {table_name} ---")
            
            # 获取表的列信息
            cursor.execute(f"PRAGMA table_info({table_name});")
            columns = cursor.fetchall()
            print(f"Columns: {[col[1] for col in columns]}")
            
            # 获取行数
            cursor.execute(f"SELECT COUNT(*) FROM {table_name};")
            count = cursor.fetchone()[0]
            print(f"Row count: {count}")
            
            # 获取前 2 行
            if count > 0:
                cursor.execute(f"SELECT * FROM {table_name} LIMIT 2;")
                rows = cursor.fetchall()
                print(f"Sample rows:")
                for i, row in enumerate(rows):
                    print(f"  Row {i}: {row[:3]}...")  # 只显示前 3 列
        
        conn.close()
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        import traceback
        traceback.print_exc()
else:
    print(f"\n✗ Database file does not exist")
