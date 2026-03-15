package com.talon.rageval.dto;

import java.util.List;

public final class ImportDtos {
  private ImportDtos() {}

  public record ImportPreviewRow(
      int rowNumber, String question, String referenceAnswer, String error) {}

  public record ImportPreviewResponse(
      String token, int validCount, int errorCount, List<ImportPreviewRow> rows) {}

  public record CommitImportResponse(int insertedCount) {}
}

