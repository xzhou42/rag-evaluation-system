package com.talon.rageval.service.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG评测数据集构建服务
 * 支持多种策略生成高质量的评测数据
 */
@Slf4j
@Service
public class EvaluationDatasetBuilder {

  private final AnyllmWorkspaceClient anyllmClient;
  private final ObjectMapper objectMapper;
  private final Random random = new Random();

  public EvaluationDatasetBuilder(AnyllmWorkspaceClient anyllmClient) {
    this.anyllmClient = anyllmClient;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * 构建评测数据集
   */
  public List<EvaluationData> buildEvaluationDataset(
      List<String> documents, 
      int targetSize, 
      String baseUrl, 
      String apiKey, 
      String workspaceId,
      List<UserTestCase> userTestCases) {
    List<EvaluationData> evaluationData = new ArrayList<>();

    log.info("开始构建评测数据集，目标大小: {}", targetSize);

    // 判断是否有用户提供的测试用例
    boolean hasUserTestCases = userTestCases != null && !userTestCases.isEmpty();
    
    if (hasUserTestCases) {
      log.info("检测到用户提供的 {} 个测试用例", userTestCases.size());
      // 有用户用例：Real + Synthetic + Adversarial
      buildWithUserTestCases(evaluationData, documents, targetSize, userTestCases);
    } else {
      log.info("未检测到用户提供的测试用例，仅生成Synthetic和Adversarial queries");
      // 无用户用例：Synthetic + Adversarial
      buildWithoutUserTestCases(evaluationData, documents, targetSize);
    }

    log.info("成功构建 {} 条评测数据", evaluationData.size());
    return evaluationData;
  }

  /**
   * 有用户测试用例时的构建逻辑
   */
  private void buildWithUserTestCases(
      List<EvaluationData> evaluationData,
      List<String> documents,
      int targetSize,
      List<UserTestCase> userTestCases) {
    
    // 1. 基于用户手动添加的测试用例采样 (使用实际提供的数量)
    // 返回Map<query, groundTruthDocs>来追踪query和groundTruthDocs的对应关系
    Map<String, List<String>> realQueriesMap = sampleRealQueriesWithUserAnswer(userTestCases);
    int realQueryCount = realQueriesMap.size();
    log.info("采样了 {} 个真实queries", realQueryCount);

    // 2. 基于文档内容生成synthetic queries (使用剩余的目标大小)
    int syntheticTargetCount = targetSize - realQueryCount;
    Map<String, String> syntheticQueriesMap = generateSyntheticQueriesWithSource(documents, syntheticTargetCount);
    log.info("生成了 {} 个synthetic queries", syntheticQueriesMap.size());

    // 3. 对抗样本构建 (基于synthetic queries生成，使用synthetic的Ground Truth)
    Map<String, List<String>> adversarialQueriesMap = generateAdversarialQueriesWithGroundTruth(
        new ArrayList<>(syntheticQueriesMap.keySet()), 
        syntheticQueriesMap,
        Math.max(1, (int) (syntheticQueriesMap.size() * 0.5)));
    log.info("生成了 {} 个对抗样本", adversarialQueriesMap.size());

    // 处理real queries - 使用用户提供的参考答案作为Ground Truth
    for (String query : realQueriesMap.keySet()) {
      try {
        EvaluationData data = new EvaluationData();
        data.query = query;
        data.groundTruthDocs = realQueriesMap.get(query);  // 使用用户提供的参考答案
        data.difficulty = assessDifficulty(query, data.groundTruthDocs);
        data.category = classifyQueryType(query);
        data.source = "real";
        evaluationData.add(data);
      } catch (Exception e) {
        log.warn("处理real query失败: {}", query, e);
      }
    }

    // 处理synthetic queries
    for (String query : syntheticQueriesMap.keySet()) {
      try {
        EvaluationData data = new EvaluationData();
        data.query = query;
        String sourceDoc = syntheticQueriesMap.get(query);
        data.groundTruthDocs = new ArrayList<>();
        data.groundTruthDocs.add(sourceDoc);
        data.difficulty = assessDifficulty(query, data.groundTruthDocs);
        data.category = classifyQueryType(query);
        data.source = "synthetic";
        evaluationData.add(data);
      } catch (Exception e) {
        log.warn("处理synthetic query失败: {}", query, e);
      }
    }

    // 处理adversarial queries - 使用引用的synthetic query的Ground Truth
    for (String query : adversarialQueriesMap.keySet()) {
      try {
        EvaluationData data = new EvaluationData();
        data.query = query;
        data.groundTruthDocs = adversarialQueriesMap.get(query);  // 使用synthetic的Ground Truth
        data.difficulty = assessDifficulty(query, data.groundTruthDocs);
        data.category = classifyQueryType(query);
        data.source = "adversarial";
        evaluationData.add(data);
      } catch (Exception e) {
        log.warn("处理adversarial query失败: {}", query, e);
      }
    }
  }

  /**
   * 无用户测试用例时的构建逻辑
   */
  private void buildWithoutUserTestCases(
      List<EvaluationData> evaluationData,
      List<String> documents,
      int targetSize) {
    
    // 1. 基于文档内容生成synthetic queries (50%)
    Map<String, String> syntheticQueriesMap = generateSyntheticQueriesWithSource(documents, (int) (targetSize * 0.5));
    log.info("生成了 {} 个synthetic queries", syntheticQueriesMap.size());

    // 2. 对抗样本构建 (50%)
    List<String> syntheticQueries = new ArrayList<>(syntheticQueriesMap.keySet());
    List<String> adversarialQueries = generateAdversarialQueries(syntheticQueries, (int) (targetSize * 0.5));
    log.info("生成了 {} 个对抗样本", adversarialQueries.size());

    // 处理synthetic queries
    for (String query : syntheticQueriesMap.keySet()) {
      try {
        EvaluationData data = new EvaluationData();
        data.query = query;
        String sourceDoc = syntheticQueriesMap.get(query);
        data.groundTruthDocs = new ArrayList<>();
        data.groundTruthDocs.add(sourceDoc);
        data.difficulty = assessDifficulty(query, data.groundTruthDocs);
        data.category = classifyQueryType(query);
        data.source = "synthetic";
        evaluationData.add(data);
      } catch (Exception e) {
        log.warn("处理synthetic query失败: {}", query, e);
      }
    }

    // 处理adversarial queries
    for (String query : adversarialQueries) {
      try {
        EvaluationData data = new EvaluationData();
        data.query = query;
        data.groundTruthDocs = annotateGroundTruth(query, documents);
        data.difficulty = assessDifficulty(query, data.groundTruthDocs);
        data.category = classifyQueryType(query);
        data.source = "adversarial";
        evaluationData.add(data);
      } catch (Exception e) {
        log.warn("处理adversarial query失败: {}", query, e);
      }
    }
  }

  /**
   * 生成synthetic queries并返回query与源文档的映射
   */
  private Map<String, String> generateSyntheticQueriesWithSource(List<String> documents, int targetCount) {
    Map<String, String> queriesMap = new LinkedHashMap<>();
    int queriesPerDoc = Math.max(1, targetCount / documents.size());

    for (String doc : documents) {
      try {
        // 从文档中提取关键信息
        String summary = extractKeyInfo(doc);
        
        // 生成多个问题
        List<String> questions = generateQuestionsFromDoc(summary, queriesPerDoc);
        
        // 将每个问题与源文档关联
        for (String question : questions) {
          queriesMap.put(question, doc);
          if (queriesMap.size() >= targetCount) {
            break;
          }
        }

        if (queriesMap.size() >= targetCount) {
          break;
        }
      } catch (Exception e) {
        log.warn("生成synthetic queries失败", e);
      }
    }

    return queriesMap;
  }

  /**
   * 从文档生成问题
   */
  private List<String> generateQuestionsFromDoc(String docContent, int numQuestions) {
    List<String> questions = new ArrayList<>();

    // 简单的启发式问题生成
    String[] questionTemplates = {
        "关于%s，有什么需要了解的？",
        "%s的主要内容是什么？",
        "如何理解%s？",
        "%s涉及哪些关键概念？",
        "请解释%s的含义",
    };

    String[] keywords = extractKeywords(docContent);
    for (int i = 0; i < numQuestions && i < keywords.length; i++) {
      String template = questionTemplates[i % questionTemplates.length];
      String question = String.format(template, keywords[i]);
      questions.add(question);
    }

    return questions;
  }

  /**
   * 采样真实queries并使用用户提供的参考答案作为Ground Truth
   */
  private Map<String, List<String>> sampleRealQueriesWithUserAnswer(List<UserTestCase> userTestCases) {
    Map<String, List<String>> realQueriesMap = new LinkedHashMap<>();
    
    // 直接使用用户提供的所有测试用例
    if (userTestCases != null && !userTestCases.isEmpty()) {
      for (UserTestCase testCase : userTestCases) {
        String query = testCase.question;
        String referenceAnswer = testCase.referenceAnswer;
        
        // 使用用户提供的参考答案作为Ground Truth
        List<String> groundTruthDocs = new ArrayList<>();
        if (referenceAnswer != null && !referenceAnswer.trim().isEmpty()) {
          groundTruthDocs.add(referenceAnswer);
        }
        
        realQueriesMap.put(query, groundTruthDocs);
      }
      
      log.info("使用了用户提供的 {} 个测试用例及其参考答案", realQueriesMap.size());
    }

    return realQueriesMap;
  }

  /**
   * 生成对抗样本并使用引用的synthetic query的Ground Truth
   */
  private Map<String, List<String>> generateAdversarialQueriesWithGroundTruth(
      List<String> baseQueries,
      Map<String, String> syntheticQueriesMap,
      int targetCount) {
    Map<String, List<String>> adversarialQueriesMap = new LinkedHashMap<>();

    String[] adversarialPatterns = {
        "%s的反面是什么？",
        "与%s相反的概念是什么？",
        "%s的例外情况有哪些？",
        "什么情况下%s不适用？",
    };

    for (String query : baseQueries) {
      if (adversarialQueriesMap.size() >= targetCount) break;

      String[] words = query.split("\\s+");
      if (words.length > 0) {
        String pattern = adversarialPatterns[random.nextInt(adversarialPatterns.length)];
        String adversarialQuery = String.format(pattern, words[0]);
        
        // 使用引用的synthetic query的Ground Truth
        String sourceDoc = syntheticQueriesMap.get(query);
        List<String> groundTruthDocs = new ArrayList<>();
        if (sourceDoc != null) {
          groundTruthDocs.add(sourceDoc);
        }
        
        adversarialQueriesMap.put(adversarialQuery, groundTruthDocs);
      }
    }

    return adversarialQueriesMap;
  }

  /**
   * 采样真实queries并同时标注Ground Truth
   * 返回Map<query, groundTruthDocs>来追踪query和groundTruthDocs的对应关系
   */
  private Map<String, List<String>> sampleRealQueriesWithGroundTruth(
      List<String> userTestCases, 
      List<String> documents, 
      int targetCount) {
    Map<String, List<String>> realQueriesMap = new LinkedHashMap<>();
    
    // 从用户提供的测试用例中采样
    if (userTestCases != null && !userTestCases.isEmpty()) {
      // 直接使用用户提供的所有测试用例，不做扩展
      for (String query : userTestCases) {
        // 为每个query标注Ground Truth
        List<String> groundTruthDocs = annotateGroundTruth(query, documents);
        realQueriesMap.put(query, groundTruthDocs);
      }
      
      log.info("使用了用户提供的 {} 个测试用例，并标注了Ground Truth", realQueriesMap.size());
    }

    return realQueriesMap;
  }

  /**
   * 采样真实queries
   * 从用户手动添加的测试用例中采样
   */
  private List<String> sampleRealQueries(List<String> userTestCases, int targetCount) {
    List<String> realQueries = new ArrayList<>();
    
    // 从用户提供的测试用例中采样
    if (userTestCases != null && !userTestCases.isEmpty()) {
      // 随机采样
      for (int i = 0; i < targetCount && i < userTestCases.size(); i++) {
        realQueries.add(userTestCases.get(i));
      }
      
      // 如果用户提供的用例不足，循环使用
      while (realQueries.size() < targetCount && !userTestCases.isEmpty()) {
        int randomIndex = new Random().nextInt(userTestCases.size());
        realQueries.add(userTestCases.get(randomIndex));
      }
      
      log.info("从用户提供的 {} 个测试用例中采样了 {} 个", userTestCases.size(), realQueries.size());
    }

    return realQueries;
  }

  /**
   * 生成对抗样本
   * 生成相似但答案不同的问题
   */
  private List<String> generateAdversarialQueries(List<String> baseQueries, int targetCount) {
    List<String> adversarial = new ArrayList<>();

    String[] adversarialPatterns = {
        "%s的反面是什么？",
        "与%s相反的概念是什么？",
        "%s的例外情况有哪些？",
        "什么情况下%s不适用？",
    };

    for (String query : baseQueries) {
      if (adversarial.size() >= targetCount) break;

      String[] words = query.split("\\s+");
      if (words.length > 0) {
        String pattern = adversarialPatterns[random.nextInt(adversarialPatterns.length)];
        String adversarialQuery = String.format(pattern, words[0]);
        adversarial.add(adversarialQuery);
      }
    }

    return adversarial;
  }

  /**
   * 标注ground truth文档
   */
  private List<String> annotateGroundTruth(String query, List<String> documents) {
    Set<String> groundTruthDocsSet = new LinkedHashSet<>();

    // 简单的相关性计算：基于关键词重叠
    String[] queryWords = query.toLowerCase().split("\\s+");
    
    for (String doc : documents) {
      String docLower = doc.toLowerCase();
      int overlap = 0;
      
      for (String word : queryWords) {
        if (word.length() > 2 && docLower.contains(word)) {
          overlap++;
        }
      }

      // 如果重叠度足够高，标记为ground truth
      if (overlap > 0) {
        // 使用完整的文档，不截断
        groundTruthDocsSet.add(doc);
      }
    }

    // 转换为List并返回，使用LinkedHashSet自动去重
    List<String> groundTruthDocs = new ArrayList<>(groundTruthDocsSet);
    return groundTruthDocs.isEmpty() ? documents.subList(0, 1) : groundTruthDocs;
  }

  /**
   * 评估难度
   */
  private String assessDifficulty(String query, List<String> groundTruthDocs) {
    int queryLength = query.length();
    int docCount = groundTruthDocs.size();

    // 启发式难度评估
    if (queryLength < 20 && docCount == 1) {
      return "easy";
    } else if (queryLength < 50 && docCount <= 3) {
      return "medium";
    } else {
      return "hard";
    }
  }

  /**
   * 分类query类型
   */
  private String classifyQueryType(String query) {
    String lower = query.toLowerCase();

    if (lower.contains("如何") || lower.contains("怎样") || lower.contains("步骤")) {
      return "procedural";
    } else if (lower.contains("为什么") || lower.contains("原因")) {
      return "analytical";
    } else if (lower.contains("和") || lower.contains("对比") || lower.contains("区别")) {
      return "comparative";
    } else {
      return "factual";
    }
  }

  /**
   * 提取关键信息
   */
  private String extractKeyInfo(String doc) {
    // 简单的提取：取前200个字符
    return doc.substring(0, Math.min(200, doc.length()));
  }

  /**
   * 提取关键词
   */
  private String[] extractKeywords(String text) {
    return text.split("[\\s，。！？；：]+");
  }

  // 内部数据结构：用于追踪query和groundTruthDocs的对应关系
  private static class QueryWithGroundTruth {
    String query;
    List<String> groundTruthDocs;
    
    QueryWithGroundTruth(String query, List<String> groundTruthDocs) {
      this.query = query;
      this.groundTruthDocs = groundTruthDocs;
    }
  }

  // DTO 类
  public static class EvaluationData {
    public String query;
    public List<String> groundTruthDocs;
    public String difficulty; // easy/medium/hard
    public String category; // factual/analytical/comparative/procedural
    public String source; // synthetic/real/adversarial
  }

  public static class DatasetBuildRequest {
    public List<String> documents;
    public int targetSize;
    public String baseUrl;
    public String apiKey;
    public String workspaceId;
    public List<UserTestCase> userTestCases;  // 用户手动添加的测试用例
  }

  public static class UserTestCase {
    public String question;
    public String referenceAnswer;
  }

  public static class DatasetBuildResponse {
    public List<EvaluationData> data;
    public int totalCount;
    public Map<String, Integer> difficultyDistribution;
    public Map<String, Integer> categoryDistribution;
    public Map<String, Integer> sourceDistribution;
  }
}
