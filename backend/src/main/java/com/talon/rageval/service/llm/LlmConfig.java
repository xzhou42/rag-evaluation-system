package com.talon.rageval.service.llm;

public record LlmConfig(
    String baseUrl,
    String embeddingBaseUrl,
    String apiKey,
    String embeddingApiKey,
    String chatModel,
    String embeddingModel,
    Double temperature) {

  public LlmConfig {
    if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("baseUrl is blank");
    if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("apiKey is blank");
    if (chatModel == null || chatModel.isBlank()) {
      throw new IllegalArgumentException("chatModel is blank");
    }
  }

  public double resolvedTemperature() {
    return temperature == null ? 0.2 : temperature;
  }

  public String resolvedEmbeddingModel() {
    return embeddingModel == null || embeddingModel.isBlank() ? chatModel : embeddingModel;
  }

  public String resolvedEmbeddingBaseUrl() {
    return (embeddingBaseUrl == null || embeddingBaseUrl.isBlank()) ? baseUrl : embeddingBaseUrl;
  }

  public String resolvedEmbeddingApiKey() {
    return (embeddingApiKey == null || embeddingApiKey.isBlank()) ? apiKey : embeddingApiKey;
  }
}

