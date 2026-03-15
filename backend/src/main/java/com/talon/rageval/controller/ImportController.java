package com.talon.rageval.controller;

import com.talon.rageval.dto.ImportDtos.CommitImportResponse;
import com.talon.rageval.dto.ImportDtos.ImportPreviewResponse;
import com.talon.rageval.dto.ImportDtos.ImportPreviewRow;
import com.talon.rageval.entity.DatasetEntity;
import com.talon.rageval.entity.TestCaseEntity;
import com.talon.rageval.repository.DatasetRepository;
import com.talon.rageval.repository.TestCaseRepository;
import com.talon.rageval.service.ExcelImportService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/datasets/{datasetId}/imports")
public class ImportController {

  private final DatasetRepository datasetRepository;
  private final TestCaseRepository testCaseRepository;
  private final ExcelImportService excelImportService;

  public ImportController(
      DatasetRepository datasetRepository,
      TestCaseRepository testCaseRepository,
      ExcelImportService excelImportService) {
    this.datasetRepository = datasetRepository;
    this.testCaseRepository = testCaseRepository;
    this.excelImportService = excelImportService;
  }

  @PostMapping("/excel")
  public ImportPreviewResponse previewExcel(
      @PathVariable Long datasetId, @RequestParam("file") MultipartFile file) {
    if (!datasetRepository.existsById(datasetId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "dataset not found");
    }
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is empty");
    }
    try (var in = file.getInputStream()) {
      return excelImportService.preview(in);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to parse excel: " + e.getMessage(), e);
    }
  }

  @PostMapping("/{token}/commit")
  @ResponseStatus(HttpStatus.CREATED)
  public CommitImportResponse commit(@PathVariable Long datasetId, @PathVariable String token) {
    DatasetEntity dataset =
        datasetRepository
            .findById(datasetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "dataset not found"));

    List<ImportPreviewRow> rows;
    try {
      rows = excelImportService.getPreviewRowsOrThrow(token);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    List<TestCaseEntity> toInsert = new ArrayList<>();
    for (ImportPreviewRow r : rows) {
      if (r.error() != null) continue;
      if (r.question() == null || r.question().isBlank()) continue;
      TestCaseEntity tc = new TestCaseEntity();
      tc.setDataset(dataset);
      tc.setQuestion(r.question().trim());
      tc.setReferenceAnswer(r.referenceAnswer());
      toInsert.add(tc);
    }

    testCaseRepository.saveAll(toInsert);
    excelImportService.consumeToken(token);
    return new CommitImportResponse(toInsert.size());
  }
}

