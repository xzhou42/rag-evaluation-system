package com.talon.rageval.controller;

import com.talon.rageval.dto.DatasetDtos.CreateDatasetRequest;
import com.talon.rageval.dto.DatasetDtos.DatasetResponse;
import com.talon.rageval.entity.DatasetEntity;
import com.talon.rageval.repository.DatasetRepository;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

  private final DatasetRepository datasetRepository;

  public DatasetController(DatasetRepository datasetRepository) {
    this.datasetRepository = datasetRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DatasetResponse create(@Valid @RequestBody CreateDatasetRequest request) {
    DatasetEntity entity = new DatasetEntity();
    entity.setName(request.name().trim());
    DatasetEntity saved = datasetRepository.save(entity);
    return toResponse(saved);
  }

  @GetMapping
  public List<DatasetResponse> list() {
    return datasetRepository.findAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  public DatasetResponse get(@PathVariable Long id) {
    DatasetEntity entity =
        datasetRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dataset not found"));
    return toResponse(entity);
  }

  private DatasetResponse toResponse(DatasetEntity entity) {
    String createdAt =
        entity.getCreatedAt() == null
            ? null
            : DateTimeFormatter.ISO_INSTANT.format(entity.getCreatedAt());
    return new DatasetResponse(entity.getId(), entity.getName(), createdAt);
  }
}

