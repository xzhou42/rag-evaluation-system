package com.talon.rageval.dto;

import com.talon.rageval.entity.EvalJobEntity.Status;

public final class EvalDtos {
  private EvalDtos() {}

  public record CreateEvalJobRequest(
      Long datasetId,
      String baseUrl,
      String embeddingBaseUrl,
      String apiKey,
      String embeddingApiKey,
      String chatModel,
      String embeddingModel,
      Double temperature) {}

  public record EvalJobResponse(
      Long id,
      Long datasetId,
      Status status,
      int progress,
      int totalCases,
      String createdAt,
      String finishedAt,
      String errorMessage) {}

  public record EvalResultRow(
      Long id,
      Long testCaseId,
      String question,
      String referenceAnswer,
      String modelAnswer,
      Double answerRelevancyScore,
      String generatedQuestions,
      String perGenSimilarities,
      String createdAt) {}
}

