# 数据构建结果分页功能说明

## 📋 功能概述

为数据构建模块添加了完整的分页功能，支持前后端协同的分页查询。

## 🎯 改进内容

### 前端改进

#### 1. **分页状态管理**
```typescript
const [buildResultPage, setBuildResultPage] = useState(1);
const [buildResultPageSize, setBuildResultPageSize] = useState(10);
```

#### 2. **表格分页配置**
```typescript
<Table
  dataSource={buildResult.data}
  pagination={{
    current: buildResultPage,
    pageSize: buildResultPageSize,
    total: buildResult.totalCount,
    onChange: (page, pageSize) => {
      setBuildResultPage(page);
      setBuildResultPageSize(pageSize);
    },
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total) => `共 ${total} 条数据`,
  }}
/>
```

#### 3. **功能特性**
- ✅ 显示全部数据（不再限制为10条）
- ✅ 每页显示10条（默认）
- ✅ 支持自定义页面大小（10、20、50、100）
- ✅ 显示总数据量
- ✅ 显示当前页码

### 后端改进

#### 1. **新增分页查询接口**
```java
@PostMapping("/query")
public Map<String, Object> queryDataset(
    @RequestBody DatasetBuildRequest request,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int pageSize)
```

#### 2. **接口参数**
| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| request | DatasetBuildRequest | 数据构建请求 | 必需 |
| page | int | 页码（从1开始） | 1 |
| pageSize | int | 每页数据量 | 10 |

#### 3. **响应格式**
```json
{
  "data": [...],                    // 当前页数据
  "totalCount": 100,                // 总数据量
  "totalPages": 10,                 // 总页数
  "currentPage": 1,                 // 当前页码
  "pageSize": 10,                   // 每页数据量
  "difficultyDistribution": {...},  // 难度分布
  "categoryDistribution": {...},    // 类型分布
  "sourceDistribution": {...}       // 来源分布
}
```

## 📊 使用示例

### 前端调用

#### 方式1：使用现有的buildDataset方法
```typescript
// 构建数据集（返回全部数据）
const res = await api.post('/dataset-builder/build', {
  documents: buildDocuments,
  targetSize: 100,
  baseUrl: ragEvalConfig.baseUrl,
  apiKey: ragEvalConfig.apiKey,
  workspaceId: ragEvalConfig.workspaceId,
});

// 前端自动分页显示
setBuildResult(res.data);
```

#### 方式2：使用新的分页查询接口
```typescript
// 查询第2页，每页20条
const res = await api.post('/dataset-builder/query', 
  {
    documents: buildDocuments,
    targetSize: 100,
    baseUrl: ragEvalConfig.baseUrl,
    apiKey: ragEvalConfig.apiKey,
    workspaceId: ragEvalConfig.workspaceId,
  },
  {
    params: {
      page: 2,
      pageSize: 20
    }
  }
);

setBuildResult(res.data);
```

### 后端处理流程

```
请求: POST /api/dataset-builder/query?page=1&pageSize=10
  ↓
验证参数
  ↓
构建完整数据集
  ↓
计算分页信息
  ├─ startIndex = (page - 1) * pageSize
  ├─ endIndex = min(startIndex + pageSize, totalCount)
  └─ totalPages = (totalCount + pageSize - 1) / pageSize
  ↓
提取当前页数据
  ↓
计算分布统计
  ↓
返回分页结果
```

## 🔄 分页计算示例

**场景**：总共100条数据，每页10条

| 页码 | startIndex | endIndex | 返回数据 |
|------|-----------|----------|---------|
| 1 | 0 | 10 | 第1-10条 |
| 2 | 10 | 20 | 第11-20条 |
| 3 | 20 | 30 | 第21-30条 |
| ... | ... | ... | ... |
| 10 | 90 | 100 | 第91-100条 |

## 📈 性能考虑

### 当前实现
- 每次查询都构建完整数据集
- 前端进行分页显示
- 适合中等规模数据（<10000条）

### 优化建议
1. **缓存数据集** - 避免重复构建
2. **后端分页** - 只返回需要的数据
3. **数据库存储** - 将生成的数据保存到数据库
4. **异步处理** - 大数据集使用异步任务

## 🎨 前端UI改进

### 分页控件显示
```
┌─────────────────────────────────────────────────────┐
│ 生成的数据样本                                       │
├─────────────────────────────────────────────────────┤
│ [表格数据]                                          │
├─────────────────────────────────────────────────────┤
│ 共 100 条数据  [10 ▼] 上一页 1 2 3 ... 10 下一页   │
└─────────────────────────────────────────────────────┘
```

### 功能说明
- **共 X 条数据** - 显示总数据量
- **[10 ▼]** - 页面大小选择器（10、20、50、100）
- **上一页/下一页** - 页码导航
- **1 2 3 ... 10** - 快速跳转到指定页

## ✅ 测试清单

- [x] 前端分页状态管理正常
- [x] 表格显示全部数据
- [x] 分页导航功能正常
- [x] 页面大小切换正常
- [x] 后端分页查询接口正常
- [x] 分布统计数据正确
- [x] 边界情况处理（最后一页、空数据等）

## 🚀 后续优化

### 短期
1. 添加数据导出功能（CSV、Excel）
2. 添加数据过滤功能（按难度、类型、来源）
3. 添加数据排序功能

### 中期
1. 实现后端真正的分页查询（只返回需要的数据）
2. 添加数据缓存机制
3. 支持大数据集处理

### 长期
1. 将生成的数据保存到数据库
2. 支持数据集版本管理
3. 支持数据集对比分析

## 📝 API文档

### POST /api/dataset-builder/build
构建完整数据集（返回全部数据）

**请求**：
```json
{
  "documents": ["文档1", "文档2", ...],
  "targetSize": 100,
  "baseUrl": "http://localhost:3001/api/v1",
  "apiKey": "sk-xxx",
  "workspaceId": "xxx"
}
```

**响应**：
```json
{
  "data": [...],
  "totalCount": 100,
  "difficultyDistribution": {...},
  "categoryDistribution": {...},
  "sourceDistribution": {...}
}
```

### POST /api/dataset-builder/query
分页查询数据集

**请求**：
```
POST /api/dataset-builder/query?page=1&pageSize=10
Content-Type: application/json

{
  "documents": ["文档1", "文档2", ...],
  "targetSize": 100,
  "baseUrl": "http://localhost:3001/api/v1",
  "apiKey": "sk-xxx",
  "workspaceId": "xxx"
}
```

**响应**：
```json
{
  "data": [...],
  "totalCount": 100,
  "totalPages": 10,
  "currentPage": 1,
  "pageSize": 10,
  "difficultyDistribution": {...},
  "categoryDistribution": {...},
  "sourceDistribution": {...}
}
```

