package com.talon.rageval.service.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * RAG 评测服务
 * 加载测试集、调用 RAG 接口、计算评测指标
 */
@Service
public class RagEvaluationService {

  private final AnyllmWorkspaceClient anyllmClient;
  private final RagEvaluator evaluator;
  private final ObjectMapper objectMapper;

  public RagEvaluationService(AnyllmWorkspaceClient anyllmClient) {
    this.anyllmClient = anyllmClient;
    this.evaluator = new RagEvaluator();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * 运行 RAG 评测
   */
  public RagEvaluationResult runEvaluation(
      String baseUrl,
      String apiKey,
      String workspaceId,
      List<RagTestCase> testCases) {
    RagEvaluationResult result = new RagEvaluationResult();
    result.testCases = new ArrayList<>();
    result.metrics = new HashMap<>();

    List<Double> correctnessScores = new ArrayList<>();
    List<Double> faithfulnessScores = new ArrayList<>();
    List<Double> relevanceScores = new ArrayList<>();
    List<Double> coherenceScores = new ArrayList<>();
    List<Double> hitRateScores = new ArrayList<>();
    List<Double> mrrScores = new ArrayList<>();
    List<Double> precisionScores = new ArrayList<>();
    List<Double> recallScores = new ArrayList<>();
    List<Long> responseTimes = new ArrayList<>();

    for (RagTestCase testCase : testCases) {
      long startTime = System.currentTimeMillis();

      try {
        // 调用 RAG 接口（带重试机制）
        AnyllmWorkspaceClient.WorkspaceChatResponse response =
            callWithRetry(baseUrl, apiKey, workspaceId, testCase.query);

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        String modelAnswer = response.textResponse != null ? response.textResponse : "";
        String sourceText =
            response.sources != null && !response.sources.isEmpty()
                ? response.sources.get(0).text
                : "";

        // 提取检索到的文档 ID
        List<String> retrievedDocIds = new ArrayList<>();
        if (response.sources != null) {
          for (AnyllmWorkspaceClient.Source source : response.sources) {
            if (source.id != null) {
              retrievedDocIds.add(source.id);
            }
          }
        }

        // 计算生成层指标
        double correctness =
            evaluator.calculateAnswerCorrectness(modelAnswer, testCase.groundTruthAnswer);
        double faithfulness = evaluator.calculateFaithfulness(modelAnswer, sourceText);
        double relevance = evaluator.calculateRelevance(testCase.query, modelAnswer);
        double coherence = evaluator.calculateCoherence(modelAnswer);

        correctnessScores.add(correctness);
        faithfulnessScores.add(faithfulness);
        relevanceScores.add(relevance);
        coherenceScores.add(coherence);

        // 计算检索层指标（如果提供了 ground truth 文档 ID）
        double hitRate = 0.0;
        double mrr = 0.0;
        double precision = 0.0;
        double recall = 0.0;

        if (testCase.groundTruthDocIds != null && !testCase.groundTruthDocIds.isEmpty()) {
          hitRate = evaluator.calculateHitRate(retrievedDocIds, testCase.groundTruthDocIds);
          mrr = evaluator.calculateMRR(retrievedDocIds, testCase.groundTruthDocIds);
          precision = evaluator.calculatePrecision(retrievedDocIds, testCase.groundTruthDocIds, 5);
          recall = evaluator.calculateRecall(retrievedDocIds, testCase.groundTruthDocIds, 5);

          hitRateScores.add(hitRate);
          mrrScores.add(mrr);
          precisionScores.add(precision);
          recallScores.add(recall);
        }

        responseTimes.add(responseTime);

        // 记录单个测试用例结果
        RagTestCaseResult caseResult = new RagTestCaseResult();
        caseResult.query = testCase.query;
        caseResult.modelAnswer = modelAnswer;
        caseResult.groundTruthAnswer = testCase.groundTruthAnswer;
        caseResult.correctness = correctness;
        caseResult.faithfulness = faithfulness;
        caseResult.relevance = relevance;
        caseResult.coherence = coherence;
        caseResult.hitRate = hitRate;
        caseResult.mrr = mrr;
        caseResult.precision = precision;
        caseResult.recall = recall;
        caseResult.responseTimeMs = responseTime;
        caseResult.sourceCount = response.sources != null ? response.sources.size() : 0;
        caseResult.retrievedDocIds = retrievedDocIds;

        result.testCases.add(caseResult);

      } catch (Exception e) {
        // 记录错误
        RagTestCaseResult caseResult = new RagTestCaseResult();
        caseResult.query = testCase.query;
        caseResult.error = e.getMessage();
        result.testCases.add(caseResult);
      }
    }

    // 计算聚合指标
    if (!correctnessScores.isEmpty()) {
      result.metrics.put("avg_correctness", average(correctnessScores));
      result.metrics.put("avg_faithfulness", average(faithfulnessScores));
      result.metrics.put("avg_relevance", average(relevanceScores));
      result.metrics.put("avg_coherence", average(coherenceScores));

      // 检索层指标（如果有计算）
      if (!hitRateScores.isEmpty()) {
        result.metrics.put("avg_hit_rate", average(hitRateScores));
        result.metrics.put("avg_mrr", average(mrrScores));
        result.metrics.put("avg_precision", average(precisionScores));
        result.metrics.put("avg_recall", average(recallScores));
      }

      result.metrics.put("avg_response_time_ms", average(responseTimes));
      result.metrics.put("total_test_cases", (double) testCases.size());
      result.metrics.put("successful_cases", (double) correctnessScores.size());

      // 综合评分（0-100）
      double overallScore =
          (average(correctnessScores) * 0.4
                  + average(faithfulnessScores) * 0.2
                  + average(relevanceScores) * 0.2
                  + average(coherenceScores) * 0.2)
              * 100;
      result.metrics.put("overall_score", Math.round(overallScore * 100.0) / 100.0);
    }

    return result;
  }

  /**
   * 带重试机制的 RAG 调用
   * 使用指数退避策略
   */
  private AnyllmWorkspaceClient.WorkspaceChatResponse callWithRetry(
      String baseUrl, String apiKey, String workspaceId, String query) throws Exception {
    int maxRetries = 3;
    int retryCount = 0;
    Exception lastException = null;

    while (retryCount < maxRetries) {
      try {
        return anyllmClient.chat(baseUrl, apiKey, workspaceId, query, "eval-session");
      } catch (Exception e) {
        lastException = e;
        retryCount++;

        // 如果是最后一次重试，直接抛出异常
        if (retryCount >= maxRetries) {
          throw e;
        }

        // 指数退避：第1次等待1秒，第2次等待2秒
        long waitTime = (long) Math.pow(2, retryCount - 1) * 1000;
        System.out.println(
            "RAG call failed (attempt "
                + retryCount
                + "/"
                + maxRetries
                + "), retrying in "
                + waitTime
                + "ms. Error: "
                + e.getMessage());

        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Retry interrupted", ie);
        }
      }
    }

    throw lastException;
  }

  /**
   * 从 JSON 文件加载测试集
   */
  public List<RagTestCase> loadTestCasesFromFile(String filePath) throws Exception {
    File file = new File(filePath);
    if (!file.exists()) {
      throw new IllegalArgumentException("Test file not found: " + filePath);
    }

    RagTestDataset dataset = objectMapper.readValue(file, RagTestDataset.class);
    return dataset.testCases != null ? dataset.testCases : new ArrayList<>();
  }

  private double average(List<? extends Number> numbers) {
    if (numbers.isEmpty()) return 0.0;
    double sum = 0;
    for (Number n : numbers) {
      sum += n.doubleValue();
    }
    return sum / numbers.size();
  }

  // DTO 类
  public static class RagTestCase {
    public String query;
    public String groundTruthAnswer;
    public List<String> groundTruthDocIds; // 可选：ground truth 文档 ID
    public String category; // factual/analytical/multi-hop
    public String difficulty; // easy/medium/hard
  }

  public static class RagTestDataset {
    public List<RagTestCase> testCases;
  }

  public static class RagTestCaseResult {
    public String query;
    public String modelAnswer;
    public String groundTruthAnswer;
    public double correctness;
    public double faithfulness;
    public double relevance;
    public double coherence;
    public double hitRate;
    public double mrr;
    public double precision;
    public double recall;
    public long responseTimeMs;
    public int sourceCount;
    public List<String> retrievedDocIds;
    public String error;
  }

  public static class RagEvaluationResult {
    public List<RagTestCaseResult> testCases;
    public Map<String, Double> metrics;
  }
}
