# LanceDB Chunk 查询服务

用于从 AnythingLLM 的 LanceDB 中读取 chunk 信息的 Python 服务。

## 安装

```bash
cd lancedb-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 启动

```bash
python app.py
```

服务会在 `http://localhost:8002` 启动。

## API 端点

### 1. 健康检查
```bash
GET /health
```

### 2. 列出所有 workspace
```bash
GET /workspaces
```

返回：
```json
{
  "workspaces": [
    {
      "id": "57d56e93-4937-4f9f-8de5-621f99501f14",
      "path": "/path/to/lancedb/57d56e93-4937-4f9f-8de5-621f99501f14.lance"
    }
  ],
  "count": 1
}
```

### 3. 获取 workspace 中的所有 chunk
```bash
GET /chunks/{workspace_id}?limit=1000&offset=0
```

返回：
```json
{
  "workspace_id": "57d56e93-4937-4f9f-8de5-621f99501f14",
  "table": "documents",
  "chunks": [
    {
      "id": "chunk_id_1",
      "content": "chunk 内容",
      "metadata": {...},
      "vector_size": 1536
    }
  ],
  "count": 10,
  "total": 100,
  "limit": 1000,
  "offset": 0
}
```

### 4. 向量搜索
```bash
POST /chunks/{workspace_id}/search
Content-Type: application/json

{
  "query_vector": [0.1, 0.2, ...],
  "limit": 10,
  "metric": "cosine"
}
```

## 在 Java 中使用

```java
// 获取所有 chunk
String response = restClient.get()
    .uri("http://localhost:8002/chunks/57d56e93-4937-4f9f-8de5-621f99501f14")
    .retrieve()
    .body(String.class);
```

## 注意

- 确保 AnythingLLM 已启动并且有数据
- LanceDB 路径：`~/Library/Application Support/anythingllm-desktop/storage/lancedb`
- 首次运行可能需要一些时间来加载数据库
