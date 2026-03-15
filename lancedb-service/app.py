#!/usr/bin/env python3
"""
LanceDB Chunk 查询服务
用于从 AnythingLLM 的 LanceDB 中读取 chunk 信息
"""

from flask import Flask, jsonify, request
import lancedb
import os
from pathlib import Path

app = Flask(__name__)

# AnythingLLM 存储路径
ANYTHINGLLM_STORAGE = Path.home() / "Library/Application Support/anythingllm-desktop/storage"
LANCEDB_PATH = ANYTHINGLLM_STORAGE / "lancedb"


def get_db_connection(workspace_id: str):
    """连接到 LanceDB"""
    db_path = LANCEDB_PATH / f"{workspace_id}.lance"
    if not db_path.exists():
        raise ValueError(f"Workspace database not found: {db_path}")
    
    db = lancedb.connect(str(db_path))
    return db


@app.route('/health', methods=['GET'])
def health():
    """健康检查"""
    return jsonify({"status": "ok"})


@app.route('/chunks/<workspace_id>', methods=['GET'])
def get_chunks(workspace_id: str):
    """
    获取 workspace 中的所有 chunk
    
    查询参数:
    - limit: 返回的最大 chunk 数（默认 1000）
    - offset: 偏移量（默认 0）
    """
    try:
        limit = request.args.get('limit', 1000, type=int)
        offset = request.args.get('offset', 0, type=int)
        
        db = get_db_connection(workspace_id)
        
        # LanceDB 中的表通常叫 "documents" 或类似的名称
        # 列出所有表
        tables = db.table_names()
        
        if not tables:
            # 返回空列表而不是错误
            return jsonify({
                "workspace_id": workspace_id,
                "table": None,
                "chunks": [],
                "count": 0,
                "total": 0,
                "limit": limit,
                "offset": offset,
                "message": "No tables found in database. Make sure AnythingLLM has indexed documents."
            })
        
        # 通常第一个表是 documents
        table_name = tables[0]
        table = db.open_table(table_name)
        
        # 查询所有数据
        results = table.search().limit(limit).offset(offset).to_list()
        
        # 转换为 JSON 格式
        chunks = []
        for i, result in enumerate(results):
            chunk = {
                "id": result.get("id", f"chunk_{offset + i}"),
                "content": result.get("content", result.get("text", "")),
                "metadata": result.get("metadata", {}),
                "vector_size": len(result.get("vector", [])) if "vector" in result else 0,
            }
            chunks.append(chunk)
        
        return jsonify({
            "workspace_id": workspace_id,
            "table": table_name,
            "chunks": chunks,
            "count": len(chunks),
            "total": table.count_rows(),
            "limit": limit,
            "offset": offset,
        })
    
    except Exception as e:
        return jsonify({"error": str(e), "workspace_id": workspace_id}), 500


@app.route('/chunks/<workspace_id>/search', methods=['POST'])
def search_chunks(workspace_id: str):
    """
    搜索 chunk（基于向量相似度）
    
    请求体:
    {
        "query_vector": [...],  // 查询向量
        "limit": 10,            // 返回的最大结果数
        "metric": "cosine"      // 距离度量方式
    }
    """
    try:
        data = request.get_json()
        query_vector = data.get("query_vector")
        limit = data.get("limit", 10)
        metric = data.get("metric", "cosine")
        
        if not query_vector:
            return jsonify({"error": "query_vector is required"}), 400
        
        db = get_db_connection(workspace_id)
        tables = db.table_names()
        
        if not tables:
            return jsonify({"error": "No tables found in database"}), 404
        
        table = db.open_table(tables[0])
        
        # 执行向量搜索
        results = table.search(query_vector).metric(metric).limit(limit).to_list()
        
        chunks = []
        for result in results:
            chunk = {
                "id": result.get("id"),
                "content": result.get("content", result.get("text", "")),
                "distance": result.get("_distance", 0),
                "metadata": result.get("metadata", {}),
            }
            chunks.append(chunk)
        
        return jsonify({
            "workspace_id": workspace_id,
            "chunks": chunks,
            "count": len(chunks),
        })
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/workspaces', methods=['GET'])
def list_workspaces():
    """列出所有可用的 workspace"""
    try:
        if not LANCEDB_PATH.exists():
            return jsonify({"workspaces": []})
        
        workspaces = []
        for db_file in LANCEDB_PATH.glob("*.lance"):
            workspace_id = db_file.stem
            workspaces.append({
                "id": workspace_id,
                "path": str(db_file),
            })
        
        return jsonify({"workspaces": workspaces, "count": len(workspaces)})
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    print("Starting LanceDB Chunk Service...")
    print(f"LanceDB path: {LANCEDB_PATH}")
    app.run(host='0.0.0.0', port=8002, debug=False)
