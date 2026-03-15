package com.talon.rageval.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class RagDtos {
  private RagDtos() {}

  public record RagTestRequest(
      @NotBlank String baseUrl,
      @NotBlank String apiKey,
      @NotBlank String workspaceId,
      @NotBlank String message) {}

  public record RagTestSource(String id, String title, String description, String text, Double score) {}

  public record RagTestMetrics(
      Integer prompt_tokens,
      Integer completion_tokens,
      Integer total_tokens,
      Double outputTps,
      Double duration,
      String model,
      String timestamp) {}

  public record RagTestResponse(
      String textResponse,
      List<RagTestSource> sources,
      double latencySeconds,
      RagTestMetrics metrics,
      String error) {}
}
