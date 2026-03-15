package com.talon.rageval.controller;

import com.talon.rageval.dto.EvalDtos.CreateEvalJobRequest;
import com.talon.rageval.dto.EvalDtos.EvalJobResponse;
import com.talon.rageval.dto.EvalDtos.EvalResultRow;
import com.talon.rageval.entity.DatasetEntity;
import com.talon.rageval.entity.EvalJobEntity;
import com.talon.rageval.entity.EvalResultEntity;
import com.talon.rageval.repository.DatasetRepository;
import com.talon.rageval.repository.EvalJobRepository;
import com.talon.rageval.repository.EvalResultRepository;
import com.talon.rageval.repository.TestCaseRepository;
import com.talon.rageval.service.EvalJobService;
import com.talon.rageval.service.llm.LlmConfig;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/eval-jobs")
public class EvalJobController {

  private final DatasetRepository datasetRepository;
  private final TestCaseRepository testCaseRepository;
  private final EvalJobRepository evalJobRepository;
  private final EvalResultRepository evalResultRepository;
  private final EvalJobService evalJobService;

  public EvalJobController(
      DatasetRepository datasetRepository,
      TestCaseRepository testCaseRepository,
      EvalJobRepository evalJobRepository,
      EvalResultRepository evalResultRepository,
      EvalJobService evalJobService) {
    this.datasetRepository = datasetRepository;
    this.testCaseRepository = testCaseRepository;
    this.evalJobRepository = evalJobRepository;
    this.evalResultRepository = evalResultRepository;
    this.evalJobService = evalJobService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EvalJobResponse createJob(@Valid @RequestBody CreateEvalJobRequest req) {
    if (req.datasetId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "datasetId is required");
    }
    DatasetEntity dataset =
        datasetRepository
            .findById(req.datasetId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dataset not found"));

    int totalCases =
        (int)
            testCaseRepository
                .findByDataset_Id(dataset.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();

    LlmConfig config =
        new LlmConfig(
            req.baseUrl(),
            req.embeddingBaseUrl(),
            req.apiKey(),
            req.embeddingApiKey(),
            req.chatModel(),
            req.embeddingModel(),
            req.temperature());

    EvalJobEntity job = evalJobService.createJob(dataset, config, totalCases);
    evalJobService.runJobAsync(job.getId(), config);
    return toJobResponse(job);
  }

  @GetMapping("/{id}")
  public EvalJobResponse get(@PathVariable Long id) {
    EvalJobEntity job =
        evalJobRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "job not found"));
    return toJobResponse(job);
  }

  @GetMapping("/{id}/results")
  public Page<EvalResultRow> listResults(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (!evalJobRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "job not found");
    }
    PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return evalResultRepository.findByJob_Id(id, pr).map(this::toResultRow);
  }

  private EvalJobResponse toJobResponse(EvalJobEntity job) {
    return new EvalJobResponse(
        job.getId(),
        job.getDataset() == null ? null : job.getDataset().getId(),
        job.getStatus(),
        job.getProgress(),
        job.getTotalCases(),
        EvalJobService.formatInstant(job.getCreatedAt()),
        EvalJobService.formatInstant(job.getFinishedAt()),
        job.getErrorMessage());
  }

  private EvalResultRow toResultRow(EvalResultEntity er) {
    return new EvalResultRow(
        er.getId(),
        er.getTestCase() == null ? null : er.getTestCase().getId(),
        er.getTestCase() == null ? null : er.getTestCase().getQuestion(),
        er.getTestCase() == null ? null : er.getTestCase().getReferenceAnswer(),
        er.getModelAnswer(),
        er.getAnswerRelevancyScore(),
        er.getGeneratedQuestions(),
        er.getPerGenSimilarities(),
        EvalJobService.formatInstant(er.getCreatedAt()));
  }
}

