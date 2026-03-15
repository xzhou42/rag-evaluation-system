package com.talon.rageval.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talon.rageval.entity.DatasetEntity;
import com.talon.rageval.entity.EvalJobEntity;
import com.talon.rageval.entity.EvalJobEntity.Status;
import com.talon.rageval.entity.EvalResultEntity;
import com.talon.rageval.entity.TestCaseEntity;
import com.talon.rageval.repository.EvalJobRepository;
import com.talon.rageval.repository.EvalResultRepository;
import com.talon.rageval.repository.TestCaseRepository;
import com.talon.rageval.service.llm.LlmClient;
import com.talon.rageval.service.llm.LlmConfig;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EvalJobService {

  private static final Logger log = LoggerFactory.getLogger(EvalJobService.class);

  private final EvalJobRepository evalJobRepository;
  private final EvalResultRepository evalResultRepository;
  private final TestCaseRepository testCaseRepository;
  private final LlmClient llmClient;
  private final EmbeddingService embeddingService;
  private final ObjectMapper objectMapper;

  public EvalJobService(
      EvalJobRepository evalJobRepository,
      EvalResultRepository evalResultRepository,
      TestCaseRepository testCaseRepository,
      LlmClient llmClient,
      EmbeddingService embeddingService,
      ObjectMapper objectMapper) {
    this.evalJobRepository = evalJobRepository;
    this.evalResultRepository = evalResultRepository;
    this.testCaseRepository = testCaseRepository;
    this.llmClient = llmClient;
    this.embeddingService = embeddingService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public EvalJobEntity createJob(DatasetEntity dataset, LlmConfig config, int totalCases) {
    EvalJobEntity job = new EvalJobEntity();
    job.setDataset(dataset);
    job.setStatus(Status.PENDING);
    job.setProgress(0);
    job.setTotalCases(totalCases);
    job.setLlmConfigSnapshot(maskConfig(config));
    return evalJobRepository.save(job);
  }

  private String maskConfig(LlmConfig cfg) {
    Map<String, Object> map = new HashMap<>();
    map.put("baseUrl", cfg.baseUrl());
    map.put("embeddingBaseUrl", cfg.resolvedEmbeddingBaseUrl());
    map.put("chatModel", cfg.chatModel());
    map.put("embeddingModel", cfg.resolvedEmbeddingModel());
    String apiKey = cfg.apiKey();
    if (apiKey != null && apiKey.length() > 6) {
      map.put("apiKey", apiKey.substring(0, 3) + "****" + apiKey.substring(apiKey.length() - 3));
    } else {
      map.put("apiKey", "****");
    }
    String embKey = cfg.resolvedEmbeddingApiKey();
    if (embKey != null && embKey.length() > 6) {
      map.put(
          "embeddingApiKey",
          embKey.substring(0, 3) + "****" + embKey.substring(embKey.length() - 3));
    }
    map.put("temperature", cfg.resolvedTemperature());
    try {
      return objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  @Async
  public void runJobAsync(Long jobId, LlmConfig config) {
    try {
      runJob(jobId, config);
    } catch (Exception e) {
      log.error("Eval job {} failed", jobId, e);
    }
  }

  @Transactional
  protected void updateJobStatus(Long jobId, Status status, String error, boolean finish) {
    EvalJobEntity job = evalJobRepository.findById(jobId).orElse(null);
    if (job == null) return;
    job.setStatus(status);
    job.setErrorMessage(error);
    if (finish) {
      job.setFinishedAt(Instant.now());
    }
    evalJobRepository.save(job);
  }

  @Transactional
  protected void updateJobProgress(Long jobId, int progress) {
    EvalJobEntity job = evalJobRepository.findById(jobId).orElse(null);
    if (job == null) return;
    job.setProgress(progress);
    evalJobRepository.save(job);
  }

  @Transactional
  protected void runJob(Long jobId, LlmConfig config) {
    EvalJobEntity job =
        evalJobRepository
            .findById(jobId)
            .orElseThrow(() -> new IllegalStateException("job not found"));
    if (job.getStatus() != Status.PENDING) {
      return;
    }
    job.setStatus(Status.RUNNING);
    evalJobRepository.save(job);

    List<TestCaseEntity> cases =
        testCaseRepository.findByDataset_Id(job.getDataset().getId(), null).getContent();
    int total = cases.size();
    if (total == 0) {
      job.setProgress(0);
      job.setTotalCases(0);
      job.setStatus(Status.SUCCEEDED);
      job.setFinishedAt(Instant.now());
      evalJobRepository.save(job);
      return;
    }

    int completed = 0;
    for (TestCaseEntity tc : cases) {
      try {
        processOne(job, tc, config);
      } catch (Exception e) {
        log.error("Failed to process case {} in job {}", tc.getId(), jobId, e);
      }
      completed++;
      updateJobProgress(jobId, completed);
    }

    job.setStatus(Status.SUCCEEDED);
    job.setFinishedAt(Instant.now());
    evalJobRepository.save(job);
  }

  @Transactional
  protected void processOne(EvalJobEntity job, TestCaseEntity tc, LlmConfig config)
      throws JsonProcessingException {
    String question = tc.getQuestion();
    String referenceAnswer = tc.getReferenceAnswer();
    String modelAnswer = llmClient.answerQuestion(config, question);
    String answerForGeneration =
        (referenceAnswer != null && !referenceAnswer.isBlank()) ? referenceAnswer : modelAnswer;

    List<String> genQuestions = new ArrayList<>();
    List<Double> sims = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String q = llmClient.generateQuestionFromAnswer(config, answerForGeneration);
      genQuestions.add(q);
      double s = embeddingService.cosineSimilarity(question, q, config);
      sims.add(s);
    }
    double avg =
        sims.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    EvalResultEntity er = new EvalResultEntity();
    er.setJob(job);
    er.setTestCase(tc);
    er.setModelAnswer(modelAnswer);
    er.setAnswerRelevancyScore(avg);
    er.setGeneratedQuestions(objectMapper.writeValueAsString(genQuestions));
    er.setPerGenSimilarities(objectMapper.writeValueAsString(sims));
    evalResultRepository.save(er);
  }

  public static String formatInstant(Instant instant) {
    return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
  }
}

