package com.talon.rageval.controller;

import com.talon.rageval.service.rag.EvaluationDatasetBuilder;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dataset-builder")
public class DatasetBuilderController {

  private final EvaluationDatasetBuilder datasetBuilder;

  public DatasetBuilderController(EvaluationDatasetBuilder datasetBuilder) {
    this.datasetBuilder = datasetBuilder;
  }

  /**
   * 构建评测数据集
   */
  @PostMapping("/build")
  public EvaluationDatasetBuilder.DatasetBuildResponse buildDataset(
      @RequestBody EvaluationDatasetBuilder.DatasetBuildRequest request) {
    if (request.documents == null || request.documents.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documents cannot be empty");
    }

    if (request.targetSize <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetSize must be positive");
    }

    try {
      List<EvaluationDatasetBuilder.EvaluationData> data =
          datasetBuilder.buildEvaluationDataset(
              request.documents,
              request.targetSize,
              request.baseUrl,
              request.apiKey,
              request.workspaceId);

      // 构建响应
      EvaluationDatasetBuilder.DatasetBuildResponse response =
          new EvaluationDatasetBuilder.DatasetBuildResponse();
      response.data = data;
      response.totalCount = data.size();

      // 计算难度分布
      response.difficultyDistribution = new HashMap<>();
      data.stream()
          .collect(Collectors.groupingByConcurrent(d -> d.difficulty, Collectors.counting()))
          .forEach((k, v) -> response.difficultyDistribution.put(k, v.intValue()));

      // 计算类型分布
      response.categoryDistribution = new HashMap<>();
      data.stream()
          .collect(Collectors.groupingByConcurrent(d -> d.category, Collectors.counting()))
          .forEach((k, v) -> response.categoryDistribution.put(k, v.intValue()));

      // 计算来源分布
      response.sourceDistribution = new HashMap<>();
      data.stream()
          .collect(Collectors.groupingByConcurrent(d -> d.source, Collectors.counting()))
          .forEach((k, v) -> response.sourceDistribution.put(k, v.intValue()));

      return response;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build dataset: " + e.getMessage(), e);
    }
  }

  /**
   * 获取数据集统计信息
   */
  @PostMapping("/stats")
  public Map<String, Object> getDatasetStats(
      @RequestBody EvaluationDatasetBuilder.DatasetBuildRequest request) {
    if (request.documents == null || request.documents.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documents cannot be empty");
    }

    Map<String, Object> stats = new HashMap<>();
    stats.put("documentCount", request.documents.size());
    stats.put("targetSize", request.targetSize);
    stats.put("estimatedSyntheticQueries", (int) (request.targetSize * 0.4));
    stats.put("estimatedRealQueries", (int) (request.targetSize * 0.4));
    stats.put("estimatedAdversarialQueries", (int) (request.targetSize * 0.2));

    return stats;
  }
}
