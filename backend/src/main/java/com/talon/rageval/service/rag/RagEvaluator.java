package com.talon.rageval.service.rag;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG 评测指标计算器
 * 支持生成层和端到端指标，检索层指标需要额外的文档 ID 信息
 */
public class RagEvaluator {

  /**
   * 计算答案正确性（0-1 分数）
   * 基于关键词重叠度和语义相似度的简化实现
   */
  public double calculateAnswerCorrectness(String modelAnswer, String groundTruthAnswer) {
    if (modelAnswer == null || groundTruthAnswer == null) {
      return 0.0;
    }

    // 关键词重叠度（简化实现）
    String[] modelWords = modelAnswer.toLowerCase().split("\\s+");
    String[] truthWords = groundTruthAnswer.toLowerCase().split("\\s+");

    int overlap = 0;
    for (String mWord : modelWords) {
      for (String tWord : truthWords) {
        if (mWord.equals(tWord)) {
          overlap++;
          break;
        }
      }
    }

    double keywordOverlap =
        Math.min(modelWords.length, truthWords.length) > 0
            ? (double) overlap / Math.min(modelWords.length, truthWords.length)
            : 0.0;

    // 长度相似度（答案长度不应差异太大）
    double lengthSimilarity =
        1.0
            - Math.abs(modelAnswer.length() - groundTruthAnswer.length())
                / (double) Math.max(modelAnswer.length(), groundTruthAnswer.length());

    // 综合分数：关键词占 70%，长度占 30%
    return keywordOverlap * 0.7 + lengthSimilarity * 0.3;
  }

  /**
   * 计算忠实度（答案是否基于检索内容）
   * 基于答案与源文档的关键词重叠度
   */
  public double calculateFaithfulness(String answer, String sourceText) {
    if (answer == null || sourceText == null || sourceText.isEmpty()) {
      return 0.0;
    }

    String[] answerWords = answer.toLowerCase().split("\\s+");
    String[] sourceWords = sourceText.toLowerCase().split("\\s+");

    int overlap = 0;
    for (String aWord : answerWords) {
      for (String sWord : sourceWords) {
        if (aWord.equals(sWord) && aWord.length() > 2) { // 忽略短词
          overlap++;
          break;
        }
      }
    }

    return answerWords.length > 0 ? (double) overlap / answerWords.length : 0.0;
  }

  /**
   * 计算相关性（答案是否回答了问题）
   * 基于问题和答案的关键词重叠度
   */
  public double calculateRelevance(String question, String answer) {
    if (question == null || answer == null) {
      return 0.0;
    }

    String[] questionWords = question.toLowerCase().split("\\s+");
    String[] answerWords = answer.toLowerCase().split("\\s+");

    int overlap = 0;
    for (String qWord : questionWords) {
      for (String aWord : answerWords) {
        if (qWord.equals(aWord) && qWord.length() > 2) {
          overlap++;
          break;
        }
      }
    }

    return questionWords.length > 0 ? (double) overlap / questionWords.length : 0.0;
  }

  /**
   * 计算连贯性（答案逻辑是否清晰）
   * 基于答案长度、句子数量等启发式指标
   */
  public double calculateCoherence(String answer) {
    if (answer == null || answer.isEmpty()) {
      return 0.0;
    }

    // 句子数量
    int sentenceCount = answer.split("[。！？\n]").length;
    // 平均句子长度
    double avgSentenceLength = (double) answer.length() / sentenceCount;

    // 启发式评分：句子数 5-20 之间、平均长度 20-100 之间为最优
    double sentenceScore =
        sentenceCount >= 5 && sentenceCount <= 20
            ? 1.0
            : Math.max(0, 1.0 - Math.abs(sentenceCount - 12.5) / 12.5);
    double lengthScore =
        avgSentenceLength >= 20 && avgSentenceLength <= 100
            ? 1.0
            : Math.max(0, 1.0 - Math.abs(avgSentenceLength - 60) / 60);

    return sentenceScore * 0.5 + lengthScore * 0.5;
  }

  /**
   * 计算 Hit Rate@k（检索层指标）
   * 需要检索文档 ID 和 ground truth 文档 ID
   */
  public double calculateHitRate(java.util.List<String> retrievedDocIds,
      java.util.List<String> groundTruthDocIds) {
    if (groundTruthDocIds == null || groundTruthDocIds.isEmpty()) {
      return 0.0;
    }

    for (String docId : retrievedDocIds) {
      if (groundTruthDocIds.contains(docId)) {
        return 1.0;
      }
    }
    return 0.0;
  }

  /**
   * 计算 MRR（Mean Reciprocal Rank）
   * 正确答案的平均倒数排名
   */
  public double calculateMRR(java.util.List<String> retrievedDocIds,
      java.util.List<String> groundTruthDocIds) {
    if (groundTruthDocIds == null || groundTruthDocIds.isEmpty()) {
      return 0.0;
    }

    for (int i = 0; i < retrievedDocIds.size(); i++) {
      if (groundTruthDocIds.contains(retrievedDocIds.get(i))) {
        return 1.0 / (i + 1);
      }
    }
    return 0.0;
  }

  /**
   * 计算 Precision@k
   */
  public double calculatePrecision(java.util.List<String> retrievedDocIds,
      java.util.List<String> groundTruthDocIds, int k) {
    if (retrievedDocIds == null || retrievedDocIds.isEmpty()) {
      return 0.0;
    }

    int relevant = 0;
    int count = Math.min(k, retrievedDocIds.size());
    for (int i = 0; i < count; i++) {
      if (groundTruthDocIds.contains(retrievedDocIds.get(i))) {
        relevant++;
      }
    }
    return (double) relevant / count;
  }

  /**
   * 计算 Recall@k
   */
  public double calculateRecall(java.util.List<String> retrievedDocIds,
      java.util.List<String> groundTruthDocIds, int k) {
    if (groundTruthDocIds == null || groundTruthDocIds.isEmpty()) {
      return 0.0;
    }

    int relevant = 0;
    int count = Math.min(k, retrievedDocIds.size());
    for (int i = 0; i < count; i++) {
      if (groundTruthDocIds.contains(retrievedDocIds.get(i))) {
        relevant++;
      }
    }
    return (double) relevant / groundTruthDocIds.size();
  }

  /**
   * 生成评测结果摘要
   */
  public Map<String, Object> generateEvaluationSummary(
      double answerCorrectness,
      double faithfulness,
      double relevance,
      double coherence,
      long responseTimeMs) {
    Map<String, Object> summary = new HashMap<>();

    // 生成层指标
    Map<String, Double> generationMetrics = new HashMap<>();
    generationMetrics.put("faithfulness", faithfulness);
    generationMetrics.put("relevance", relevance);
    generationMetrics.put("coherence", coherence);
    summary.put("generation_metrics", generationMetrics);

    // 端到端指标
    Map<String, Object> e2eMetrics = new HashMap<>();
    e2eMetrics.put("answer_correctness", answerCorrectness);
    e2eMetrics.put("response_time_ms", responseTimeMs);
    summary.put("e2e_metrics", e2eMetrics);

    // 综合评分（0-100）
    double overallScore =
        (answerCorrectness * 0.4
                + faithfulness * 0.2
                + relevance * 0.2
                + coherence * 0.2)
            * 100;
    summary.put("overall_score", Math.round(overallScore * 100.0) / 100.0);

    return summary;
  }
}
