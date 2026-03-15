# 向量数据读取指南

## 概述
这个工具用于读取和处理向量数据库中的JSON格式向量数据。支持嵌套数组结构和单层数组结构。

## 数据结构

### 向量数据格式
```json
{
  "id": "向量唯一标识",
  "values": [0.123, -0.456, ...],  // 浮点数数组，代表向量的维度
  "metadata": {
    "id": "文档ID",
    "title": "文档标题",
    "description": "文档描述",
    "docAuthor": "作者",
    "docSource": "来源",
    "published": "发布时间",
    "wordCount": 815,
    "token_count_estimate": 7719,
    "text": "文档文本内容"
  }
}
```

## 使用方法

### 1. 在Spring Boot中使用

#### 注入VectorDataReader
```java
@RestController
@RequiredArgsConstructor
public class MyController {
    private final VectorDataReader vectorDataReader;
    
    @GetMapping("/load-vectors")
    public void loadVectors() throws IOException {
        List<List<VectorData>> data = vectorDataReader.readVectorDataFile("/path/to/file.json");
        // 处理数据
    }
}
```

#### 读取嵌套数据
```java
List<List<VectorData>> nestedData = vectorDataReader.readVectorDataFile(filePath);
for (List<VectorData> batch : nestedData) {
    for (VectorData vector : batch) {
        String id = vector.getId();
        List<Double> values = vector.getValues();
        Map<String, Object> metadata = vector.getMetadata();
    }
}
```

#### 读取单层数据
```java
List<VectorData> flatData = vectorDataReader.readFlatVectorData(filePath);
```

#### 提取元数据
```java
VectorData vector = ...;
String metadataInfo = vectorDataReader.extractMetadataInfo(vector);
String text = vectorDataReader.extractText(vector);
```

### 2. API端点

#### 读取向量数据
```
GET /api/vector/read?filePath=/path/to/file.json
```

响应:
```json
{
  "success": true,
  "totalBatches": 1,
  "data": [[{...}, {...}]]
}
```

#### 读取单层数据
```
GET /api/vector/read-flat?filePath=/path/to/file.json
```

#### 获取统计信息
```
GET /api/vector/stats?filePath=/path/to/file.json
```

响应:
```json
{
  "success": true,
  "stats": {
    "totalBatches": 1,
    "totalVectors": 100,
    "vectorDimensions": 384
  }
}
```

#### 获取元数据
```
GET /api/vector/metadata?filePath=/path/to/file.json&batchIndex=0&vectorIndex=0
```

## 关键特性

1. **灵活的数据格式支持**: 支持嵌套和单层数组结构
2. **元数据提取**: 自动提取和格式化文档元数据
3. **文本内容提取**: 获取向量对应的原始文本
4. **错误处理**: 完善的异常处理和日志记录
5. **RESTful API**: 提供HTTP接口便于集成

## 常见用途

### 用途1: 加载向量到数据库
```java
List<List<VectorData>> data = vectorDataReader.readVectorDataFile(filePath);
for (List<VectorData> batch : data) {
    for (VectorData vector : batch) {
        // 保存到数据库
        vectorRepository.save(vector);
    }
}
```

### 用途2: 向量相似度搜索
```java
VectorData queryVector = ...;
List<Double> queryValues = queryVector.getValues();
// 使用向量进行相似度计算
```

### 用途3: 文档检索
```java
VectorData vector = ...;
String text = vectorDataReader.extractText(vector);
String title = (String) vector.getMetadata().get("title");
// 返回给用户
```

## 性能考虑

- 大文件建议分批处理
- 使用流式处理而不是一次性加载所有数据
- 考虑使用缓存存储频繁访问的向量

## 依赖

- Jackson (JSON处理)
- Lombok (代码简化)
- Spring Boot (框架)
