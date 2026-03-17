# 评测数据集构建流程详解

## 📊 完整流程图

```
前端用户输入
    ↓
┌─────────────────────────────────────┐
│ 前端收集数据                         │
│ - documents: 文档列表               │
│ - targetSize: 目标数据集大小        │
│ - baseUrl, apiKey, workspaceId      │
└─────────────────────────────────────┘
    ↓
POST /api/dataset-builder/build
    ↓
┌─────────────────────────────────────┐
│ 后端处理 (EvaluationDatasetBuilder)  │
│                                     │
│ 1. 生成Synthetic Queries (40%)      │
│    └─ 基于文档内容生成问题          │
│                                     │
│ 2. 采样Real Queries (40%)           │
│    └─ 从真实用户查询中采样          │
│                                     │
│ 3. 生成Adversarial Queries (20%)    │
│    └─ 生成对抗样本                  │
│                                     │
│ 4. 标注Ground Truth                 │
│    └─ 为每个query标注相关文档       │
│                                     │
│ 5. 评估难度                         │
│    └─ easy/medium/hard              │
│                                     │
│ 6. 分类问题类型                     │
│    └─ factual/analytical/...        │
└─────────────────────────────────────┘
    ↓
返回DatasetBuildResponse
    ↓
前端展示结果
```

## 🔄 前端传入的数据

### 请求体结构

```typescript
{
  documents: string[],        // 文档列表
  targetSize: number,         // 目标数据集大小（如100）
  baseUrl: string,            // RAG系统Base URL
  apiKey: string,             // RAG系统API Key
  workspaceId: string         // RAG系统Workspace ID
}
```

### 前端代码示例

```typescript
const handleBuildDataset = async (values: any) => {
  const res = await api.post<DatasetBuildResponse>('/dataset-builder/build', {
    documents: buildDocuments,           // 用户输入的文档
    targetSize: values.targetSize || 100, // 用户设置的目标大小
    baseUrl: ragEvalConfig.baseUrl,      // RAG系统配置
    apiKey: ragEvalConfig.apiKey,
    workspaceId: ragEvalConfig.workspaceId,
  });
  setBuildResult(res.data);
};
```

## 🔧 后端处理流程

### 1. 生成Synthetic Queries (40%)

**目的**：基于文档内容自动生成问题

**处理步骤**：
```java
// 1. 计算每个文档需要生成的问题数
int queriesPerDoc = targetCount / documents.size();

// 2. 对每个文档进行处理
for (String doc : documents) {
  // 3. 提取关键信息
  String summary = extractKeyInfo(doc);
  
  // 4. 使用模板生成问题
  List<String> questions = generateQuestionsFromDoc(summary, queriesPerDoc);
  queries.addAll(questions);
}

// 5. 限制数量
return queries.stream().limit(targetCount).collect(Collectors.toList());
```

**问题生成模板**：
```
- "关于%s，有什么需要了解的？"
- "%s的主要内容是什么？"
- "如何理解%s？"
- "%s涉及哪些关键概念？"
- "请解释%s的含义"
```

### 2. 采样Real Queries (40%)

**目的**：从真实用户查询中采样

**处理步骤**：
```java
// 当前实现：使用示例数据
String[] exampleQueries = {
  "财务处理的流程是什么？",
  "如何编制年度预算？",
  "预算执行需要注意什么？",
  // ...
};

// 实际应用中应该从数据库或日志中采样
List<String> realQueries = sampleFromDatabase(targetCount);
```

### 3. 生成Adversarial Queries (20%)

**目的**：生成对抗样本，测试系统鲁棒性

**处理步骤**：
```java
// 1. 从Real Queries中选择基础问题
for (String query : baseQueries) {
  // 2. 应用对抗模式
  String adversarialQuery = applyAdversarialPattern(query);
  adversarial.add(adversarialQuery);
}
```

**对抗模式**：
```
- "%s的反面是什么？"
- "与%s相反的概念是什么？"
- "%s的例外情况有哪些？"
- "什么情况下%s不适用？"
```

### 4. 标注Ground Truth

**目的**：为每个query标注相关的文档

**处理步骤**：
```java
private List<String> annotateGroundTruth(String query, List<String> documents) {
  List<String> groundTruthDocs = new ArrayList<>();
  
  // 1. 分割query为单词
  String[] queryWords = query.toLowerCase().split("\\s+");
  
  // 2. 对每个文档计算相关性
  for (String doc : documents) {
    String docLower = doc.toLowerCase();
    int overlap = 0;
    
    // 3. 计算关键词重叠数
    for (String word : queryWords) {
      if (word.length() > 2 && docLower.contains(word)) {
        overlap++;
      }
    }
    
    // 4. 如果重叠度足够高，标记为ground truth
    if (overlap > 0) {
      groundTruthDocs.add(doc.substring(0, Math.min(50, doc.length())));
    }
  }
  
  return groundTruthDocs.isEmpty() ? documents.subList(0, 1) : groundTruthDocs;
}
```

### 5. 评估难度

**目的**：根据问题复杂度分类

**处理步骤**：
```java
private String assessDifficulty(String query, List<String> groundTruthDocs) {
  int queryLength = query.length();
  int docCount = groundTruthDocs.size();

  // 启发式难度评估
  if (queryLength < 20 && docCount == 1) {
    return "easy";      // 短问题，单个文档
  } else if (queryLength < 50 && docCount <= 3) {
    return "medium";    // 中等长度，少量文档
  } else {
    return "hard";      // 长问题，多个文档
  }
}
```

### 6. 分类问题类型

**目的**：根据问题特征分类

**处理步骤**：
```java
private String classifyQueryType(String query) {
  String lower = query.toLowerCase();

  if (lower.contains("如何") || lower.contains("怎样") || lower.contains("步骤")) {
    return "procedural";    // 过程性问题
  } else if (lower.contains("为什么") || lower.contains("原因")) {
    return "analytical";    // 分析性问题
  } else if (lower.contains("和") || lower.contains("对比") || lower.contains("区别")) {
    return "comparative";   // 比较性问题
  } else {
    return "factual";       // 事实性问题
  }
}
```

## 📤 后端返回的数据

### 响应体结构

```typescript
{
  data: EvaluationData[],                    // 生成的评测数据
  totalCount: number,                        // 总数据条数
  difficultyDistribution: {                  // 难度分布
    "easy": number,
    "medium": number,
    "hard": number
  },
  categoryDistribution: {                    // 类型分布
    "factual": number,
    "analytical": number,
    "comparative": number,
    "procedural": number
  },
  sourceDistribution: {                      // 来源分布
    "synthetic": number,
    "real": number,
    "adversarial": number
  }
}
```

### EvaluationData结构

```typescript
{
  query: string,                    // 生成的问题
  groundTruthDocs: string[],        // 标注的相关文档
  difficulty: string,               // 难度等级
  category: string,                 // 问题类型
  source: string                    // 数据来源
}
```

## 📊 数据分布示例

假设targetSize = 100：

```
总数据量: 100条

来源分布:
├─ Synthetic Queries: 40条 (40%)
├─ Real Queries: 40条 (40%)
└─ Adversarial Queries: 20条 (20%)

难度分布:
├─ Easy: 30条 (30%)
├─ Medium: 50条 (50%)
└─ Hard: 20条 (20%)

类型分布:
├─ Factual: 40条 (40%)
├─ Analytical: 30条 (30%)
├─ Comparative: 20条 (20%)
└─ Procedural: 10条 (10%)
```

## 🔍 关键算法详解

### 关键词重叠计算

```
Query: "财务处理的流程是什么"
Doc: "财务处理是企业管理的重要部分，需要专业的财务人员"

分词:
Query词: [财务, 处理, 流程, 什么]
Doc词: [财务, 处理, 企业, 管理, 重要, 部分, 需要, 专业, 财务, 人员]

重叠词: [财务, 处理] = 2个
重叠度: 2 / min(4, 10) = 2/4 = 50%

判断: 重叠度 > 0，标记为Ground Truth
```

### 难度评估算法

```
Query长度 < 20 && 文档数 == 1  → Easy
Query长度 < 50 && 文档数 <= 3  → Medium
其他情况                        → Hard

示例:
"预算?" (长度5) + 1个文档 → Easy
"如何编制年度预算?" (长度12) + 2个文档 → Medium
"详细说明企业财务管理的完整流程和注意事项" (长度25) + 5个文档 → Hard
```

## 💾 数据持久化

生成的数据可以：
1. **直接用于评测** - 作为测试用例导入系统
2. **导出为JSON** - 保存为文件供后续使用
3. **导出为Excel** - 便于人工审核和修改
4. **保存到数据库** - 作为评测数据集的一部分

## ⚙️ 配置参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| documents | 输入文档列表 | ["财务知识手册...", "预算管理指南..."] |
| targetSize | 目标数据集大小 | 100, 500, 1000 |
| baseUrl | RAG系统地址 | http://localhost:3001/api/v1 |
| apiKey | RAG系统密钥 | sk-xxx... |
| workspaceId | 工作空间ID | 57d56e93-4937-4f9f-8de5-... |

## 🎯 最佳实践

1. **文档质量** - 输入高质量的文档，生成的数据质量会更好
2. **目标大小** - 根据需求选择合适的数据集大小（100-1000条）
3. **数据审核** - 生成后进行人工审核，确保数据质量
4. **多次迭代** - 可以多次运行，生成不同的数据集进行对比
5. **版本管理** - 保存不同版本的数据集，便于追踪和对比

