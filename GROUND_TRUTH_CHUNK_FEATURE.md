# Ground Truth Chunk 功能实现总结

## 功能描述
用户在创建测试用例时，可以选择一个或多个向量数据中的chunk作为ground truth文本依据（非必选）。这些chunk ID会被保存到数据库，后端在计算检索层评价指标时可以使用这些ground truth文本进行计算。

## 后端实现

### 1. 数据库迁移 (V2__add_ground_truth_chunks.sql)
- 在 `test_case` 表中添加 `ground_truth_chunk_ids` 字段（JSON格式）
- 添加索引以提高查询性能

### 2. 实体类更新 (TestCaseEntity.java)
```java
@Column(name = "ground_truth_chunk_ids", columnDefinition = "json")
private String groundTruthChunkIds;
```

### 3. DTO更新 (TestCaseDtos.java)
- `CreateTestCaseRequest`: 添加 `List<String> groundTruthChunkIds` 字段
- `UpdateTestCaseRequest`: 添加 `List<String> groundTruthChunkIds` 字段
- `TestCaseResponse`: 添加 `List<String> groundTruthChunkIds` 字段

### 4. 控制器更新 (TestCaseController.java)
- 在创建和更新用例时，将 `groundTruthChunkIds` 列表序列化为JSON字符串保存
- 在返回响应时，将JSON字符串反序列化为列表
- 使用 `ObjectMapper` 处理JSON序列化/反序列化

## 前端实现

### 1. 数据模型
```typescript
interface TestCase {
  id: number;
  datasetId: number;
  question: string;
  referenceAnswer?: string;
  groundTruthChunkIds?: string[];
  createdAt?: string;
}
```

### 2. UI交互流程
1. 用户点击"加载向量数据"按钮加载chunk列表
2. 在chunk列表中勾选要作为ground truth的chunk
3. 填写问题和参考答案
4. 点击"新增用例"按钮
5. 选中的chunk ID会随请求发送到后端

### 3. 表格显示
- 在测试用例表格中添加"依据"列
- 显示该用例关联的chunk数量
- 如果没有关联chunk，显示"-"

## API接口

### 创建用例
```
POST /api/datasets/{datasetId}/cases
Content-Type: application/json

{
  "question": "问题内容",
  "referenceAnswer": "参考答案",
  "groundTruthChunkIds": ["chunk-id-1", "chunk-id-2"]
}
```

### 更新用例
```
PUT /api/cases/{id}
Content-Type: application/json

{
  "question": "问题内容",
  "referenceAnswer": "参考答案",
  "groundTruthChunkIds": ["chunk-id-1"]
}
```

### 响应格式
```json
{
  "id": 1,
  "datasetId": 1,
  "question": "问题内容",
  "referenceAnswer": "参考答案",
  "groundTruthChunkIds": ["chunk-id-1", "chunk-id-2"],
  "createdAt": "2026-03-15T10:00:00Z"
}
```

## 后续使用

在计算检索层评价指标时，可以：
1. 获取测试用例的 `groundTruthChunkIds`
2. 使用这些chunk ID从向量数据库中检索对应的文本
3. 将这些文本作为ground truth进行评价计算

## 数据库字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| ground_truth_chunk_ids | JSON | 存储chunk ID列表，格式为JSON数组 |

示例值：
```json
["b056f615-e8cd-4f83-adb3-f3f3c95e3769", "c167g726-f9de-5g94-bec4-g4g4d06f4870"]
```
