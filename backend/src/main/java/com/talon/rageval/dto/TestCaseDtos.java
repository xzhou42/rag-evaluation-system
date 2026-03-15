package com.talon.rageval.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class TestCaseDtos {
  private TestCaseDtos() {}

  public record CreateTestCaseRequest(
      @NotBlank String question,
      String referenceAnswer,
      List<String> groundTruthChunkIds) {}

  public record UpdateTestCaseRequest(
      @NotBlank String question,
      String referenceAnswer,
      List<String> groundTruthChunkIds) {}

  public record TestCaseResponse(
      Long id,
      Long datasetId,
      String question,
      String referenceAnswer,
      List<String> groundTruthChunkIds,
      String createdAt) {}
}

