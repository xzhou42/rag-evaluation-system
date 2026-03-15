#!/usr/bin/env python3
"""
调试 LanceDB 连接和数据
"""

import lancedb
from pathlib import Path

WORKSPACE_ID = "57d56e93-4937-4f9f-8de5-621f99501f14"
LANCEDB_PATH = Path.home() / "Library/Application Support/anythingllm-desktop/storage/lancedb"
DB_PATH = LANCEDB_PATH / f"{WORKSPACE_ID}.lance"

print(f"Database path: {DB_PATH}")
print(f"Database exists: {DB_PATH.exists()}")

if DB_PATH.exists():
    try:
        db = lancedb.connect(str(DB_PATH))
        print(f"\n✓ Connected to LanceDB")
        
        # 列出所有表
        tables = db.table_names()
        print(f"\nTables found: {tables}")
        print(f"Number of tables: {len(tables)}")
        
        if tables:
            for table_name in tables:
                print(f"\n--- Table: {table_name} ---")
                table = db.open_table(table_name)
                print(f"Row count: {table.count_rows()}")
                
                # 获取表的 schema
                print(f"Schema: {table.schema}")
                
                # 获取前 3 行
                results = table.search().limit(3).to_list()
                print(f"Sample rows: {len(results)}")
                for i, row in enumerate(results):
                    print(f"\nRow {i}:")
                    for key, value in row.items():
                        if key == "vector":
                            print(f"  {key}: [vector of size {len(value)}]")
                        elif isinstance(value, str) and len(value) > 100:
                            print(f"  {key}: {value[:100]}...")
                        else:
                            print(f"  {key}: {value}")
        else:
            print("\n✗ No tables found in database")
            
    except Exception as e:
        print(f"\n✗ Error connecting to LanceDB: {e}")
        import traceback
        traceback.print_exc()
else:
    print(f"\n✗ Database path does not exist")
