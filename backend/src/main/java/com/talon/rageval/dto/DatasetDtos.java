package com.talon.rageval.dto;

import jakarta.validation.constraints.NotBlank;

public final class DatasetDtos {
  private DatasetDtos() {}

  public record CreateDatasetRequest(@NotBlank String name) {}

  public record DatasetResponse(Long id, String name, String createdAt) {}
}

