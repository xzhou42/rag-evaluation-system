package com.talon.rageval.controller;

import com.talon.rageval.dto.RagDtos.RagTestRequest;
import com.talon.rageval.dto.RagDtos.RagTestResponse;
import com.talon.rageval.dto.RagDtos.RagTestSource;
import com.talon.rageval.dto.RagDtos.RagTestMetrics;
import com.talon.rageval.service.rag.AnyllmWorkspaceClient;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagTestController {

  private final AnyllmWorkspaceClient workspaceClient;

  public RagTestController(AnyllmWorkspaceClient workspaceClient) {
    this.workspaceClient = workspaceClient;
  }

  @PostMapping("/test")
  public RagTestResponse test(@Valid @RequestBody RagTestRequest req) {
    Instant start = Instant.now();
    try {
      AnyllmWorkspaceClient.WorkspaceChatResponse resp =
          workspaceClient.chat(
              req.baseUrl(),
              req.apiKey(),
              req.workspaceId(),
              req.message(),
              "rag-test-" + UUID.randomUUID());

      double latencySeconds =
          (Instant.now().toEpochMilli() - start.toEpochMilli()) / 1000.0;

      List<RagTestSource> sources = new ArrayList<>();
      if (resp.sources != null) {
        for (AnyllmWorkspaceClient.Source s : resp.sources) {
          sources.add(
              new RagTestSource(
                  s.id,
                  s.title,
                  s.description,
                  s.text,
                  s.score));
        }
      }

      RagTestMetrics metrics = null;
      if (resp.metrics != null) {
        metrics =
            new RagTestMetrics(
                resp.metrics.prompt_tokens,
                resp.metrics.completion_tokens,
                resp.metrics.total_tokens,
                resp.metrics.outputTps,
                resp.metrics.duration,
                resp.metrics.model,
                resp.metrics.timestamp);
      }

      return new RagTestResponse(
          resp.textResponse, sources, latencySeconds, metrics, resp.error);
    } catch (Exception e) {
      double latencySeconds =
          (Instant.now().toEpochMilli() - start.toEpochMilli()) / 1000.0;
      return new RagTestResponse(
          null, List.of(), latencySeconds, null, e.getMessage());
    }
  }
}
