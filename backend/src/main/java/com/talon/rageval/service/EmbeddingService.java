package com.talon.rageval.service;

import com.talon.rageval.service.llm.LlmConfig;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Text embedding + cosine similarity based on an external embedding model.
 *
 * This service calls an OpenAI-compatible /v1/embeddings endpoint using
 * the provided {@link LlmConfig} (baseUrl, apiKey, model).
 */
@Service
public class EmbeddingService {

  private final RestClient restClient;

  public EmbeddingService(RestClient.Builder builder) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(120).toMillis());
    this.restClient =
        builder
            .requestFactory(rf)
            .build();
  }

  public double cosineSimilarity(String a, String b, LlmConfig config) {
    double[] va = embed(a, config);
    double[] vb = embed(b, config);
    int dim = Math.min(va.length, vb.length);
    if (dim == 0) {
      return 0.0;
    }
    double dot = 0.0;
    double na = 0.0;
    double nb = 0.0;
    for (int i = 0; i < dim; i++) {
      dot += va[i] * vb[i];
      na += va[i] * va[i];
      nb += vb[i] * vb[i];
    }
    if (na == 0 || nb == 0) {
      return 0.0;
    }
    return dot / (Math.sqrt(na) * Math.sqrt(nb));
  }

  public double[] embed(String text, LlmConfig config) {
    if (text == null || text.isBlank()) {
      return new double[0];
    }
    String url = normalizeEmbeddingsUrl(config.resolvedEmbeddingBaseUrl());
    EmbeddingsRequest req = new EmbeddingsRequest(config.resolvedEmbeddingModel(), text);
    try {
      EmbeddingsResponse resp =
          restClient
              .post()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.resolvedEmbeddingApiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(req)
              .retrieve()
              .body(EmbeddingsResponse.class);

      if (resp == null || resp.data == null || resp.data.isEmpty()) {
        throw new IllegalStateException("empty response from embeddings endpoint");
      }
      List<Double> emb = resp.data.get(0).embedding;
      if (emb == null || emb.isEmpty()) {
        throw new IllegalStateException("missing embedding vector in response");
      }
      double[] v = new double[emb.size()];
      for (int i = 0; i < emb.size(); i++) {
        Double d = emb.get(i);
        v[i] = d == null ? 0.0 : d;
      }
      return v;
    } catch (RestClientException e) {
      throw new IllegalStateException("Embeddings request failed: " + e.getMessage(), e);
    }
  }

  private static String normalizeEmbeddingsUrl(String baseUrl) {
    String b = baseUrl.trim();
    if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
    // If user passes base URL like https://host/.../v1
    if (b.endsWith("/v1")) {
      return b + "/embeddings";
    }
    // If user passes full chat completions URL, map to embeddings
    if (b.endsWith("/v1/chat/completions")) {
      return b.substring(0, b.length() - "/chat/completions".length()) + "/embeddings";
    }
    // If user passes a specific embeddings URL under /v1/, trust it
    if (b.contains("/v1/") && b.contains("embeddings")) {
      return b;
    }
    // Fallback: append /v1/embeddings
    return b + "/v1/embeddings";
  }

  // Minimal OpenAI-compatible DTOs for embeddings
  public record EmbeddingsRequest(String model, String input) {}

  public static class EmbeddingsResponse {
    public List<EmbeddingData> data;
  }

  public static class EmbeddingData {
    public List<Double> embedding;
  }
}

