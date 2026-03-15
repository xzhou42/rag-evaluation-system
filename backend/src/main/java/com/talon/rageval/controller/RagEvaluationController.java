package com.talon.rageval.controller;

import com.talon.rageval.service.rag.RagEvaluationService;
import com.talon.rageval.service.rag.RagEvaluationService.RagEvaluationResult;
import com.talon.rageval.service.rag.RagEvaluationService.RagTestCase;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag-eval")
public class RagEvaluationController {

  private final RagEvaluationService ragEvaluationService;

  public RagEvaluationController(RagEvaluationService ragEvaluationService) {
    this.ragEvaluationService = ragEvaluationService;
  }

  @PostMapping("/run")
  public RagEvaluationResult runEvaluation(@RequestBody RagEvaluationRequest request) {
    return ragEvaluationService.runEvaluation(
        request.baseUrl, request.apiKey, request.workspaceId, request.testCases);
  }

  public static class RagEvaluationRequest {
    public String baseUrl;
    public String apiKey;
    public String workspaceId;
    public List<RagTestCase> testCases;
  }
}
