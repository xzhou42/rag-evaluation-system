package com.talon.rageval.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talon.rageval.dto.TestCaseDtos.CreateTestCaseRequest;
import com.talon.rageval.dto.TestCaseDtos.TestCaseResponse;
import com.talon.rageval.dto.TestCaseDtos.UpdateTestCaseRequest;
import com.talon.rageval.entity.DatasetEntity;
import com.talon.rageval.entity.TestCaseEntity;
import com.talon.rageval.repository.DatasetRepository;
import com.talon.rageval.repository.TestCaseRepository;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TestCaseController {

  private final DatasetRepository datasetRepository;
  private final TestCaseRepository testCaseRepository;
  private final ObjectMapper objectMapper;

  public TestCaseController(
      DatasetRepository datasetRepository,
      TestCaseRepository testCaseRepository,
      ObjectMapper objectMapper) {
    this.datasetRepository = datasetRepository;
    this.testCaseRepository = testCaseRepository;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/api/datasets/{datasetId}/cases")
  @ResponseStatus(HttpStatus.CREATED)
  public TestCaseResponse create(
      @PathVariable Long datasetId, @Valid @RequestBody CreateTestCaseRequest request) {
    DatasetEntity dataset =
        datasetRepository
            .findById(datasetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dataset not found"));

    TestCaseEntity entity = new TestCaseEntity();
    entity.setDataset(dataset);
    entity.setQuestion(request.question().trim());
    entity.setReferenceAnswer(request.referenceAnswer());
    
    // 保存groundTruthChunkIds为JSON字符串
    if (request.groundTruthChunkIds() != null && !request.groundTruthChunkIds().isEmpty()) {
      try {
        String jsonStr = objectMapper.writeValueAsString(request.groundTruthChunkIds());
        entity.setGroundTruthChunkIds(jsonStr);
      } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid groundTruthChunkIds format: " + e.getMessage());
      }
    }
    
    TestCaseEntity saved = testCaseRepository.save(entity);
    return toResponse(saved);
  }

  @GetMapping("/api/datasets/{datasetId}/cases")
  public Page<TestCaseResponse> listByDataset(
      @PathVariable Long datasetId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (!datasetRepository.existsById(datasetId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "dataset not found");
    }
    PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    return testCaseRepository.findByDataset_Id(datasetId, pr).map(this::toResponse);
  }

  @PutMapping("/api/cases/{id}")
  public TestCaseResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateTestCaseRequest request) {
    TestCaseEntity entity =
        testCaseRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "case not found"));
    entity.setQuestion(request.question().trim());
    entity.setReferenceAnswer(request.referenceAnswer());
    
    // 更新groundTruthChunkIds
    if (request.groundTruthChunkIds() != null && !request.groundTruthChunkIds().isEmpty()) {
      try {
        String jsonStr = objectMapper.writeValueAsString(request.groundTruthChunkIds());
        entity.setGroundTruthChunkIds(jsonStr);
      } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid groundTruthChunkIds format: " + e.getMessage());
      }
    } else {
      entity.setGroundTruthChunkIds(null);
    }
    
    TestCaseEntity saved = testCaseRepository.save(entity);
    return toResponse(saved);
  }

  @DeleteMapping("/api/cases/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    if (!testCaseRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "case not found");
    }
    testCaseRepository.deleteById(id);
  }

  private TestCaseResponse toResponse(TestCaseEntity entity) {
    String createdAt =
        entity.getCreatedAt() == null
            ? null
            : DateTimeFormatter.ISO_INSTANT.format(entity.getCreatedAt());
    
    // 解析groundTruthChunkIds JSON字符串
    List<String> groundTruthChunkIds = null;
    if (entity.getGroundTruthChunkIds() != null && !entity.getGroundTruthChunkIds().isEmpty()) {
      try {
        groundTruthChunkIds = objectMapper.readValue(
            entity.getGroundTruthChunkIds(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
      } catch (Exception e) {
        // 如果解析失败，返回null
        groundTruthChunkIds = null;
      }
    }
    
    return new TestCaseResponse(
        entity.getId(),
        entity.getDataset() == null ? null : entity.getDataset().getId(),
        entity.getQuestion(),
        entity.getReferenceAnswer(),
        groundTruthChunkIds,
        createdAt);
  }
}

