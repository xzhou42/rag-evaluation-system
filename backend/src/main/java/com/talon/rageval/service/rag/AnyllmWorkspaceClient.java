package com.talon.rageval.service.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AnyllmWorkspaceClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public AnyllmWorkspaceClient(RestClient.Builder builder) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(300).toMillis()); // 5 minutes
    this.restClient = builder.requestFactory(rf).build();
    this.objectMapper = new ObjectMapper();
  }

  public WorkspaceChatResponse chat(
      String baseUrl, String apiKey, String workspaceId, String message, String sessionId) {
    String url = normalizeWorkspaceChatUrl(baseUrl, workspaceId);
    WorkspaceChatRequest req = new WorkspaceChatRequest(message, "query", sessionId, false);

    try {
      // AnyLLM returns octet-stream, use toEntity to get raw response
      var response =
          restClient
              .post()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(req)
              .exchange(
                  (clientRequest, clientResponse) -> {
                    byte[] body = clientResponse.getBody().readAllBytes();
                    if (body.length == 0) {
                      throw new IllegalStateException(
                          "AnyLLM workspace chat returned empty response");
                    }
                    return objectMapper.readValue(body, WorkspaceChatResponse.class);
                  });

      return response;
    } catch (RestClientException e) {
      throw new IllegalStateException("AnyLLM workspace chat failed: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new IllegalStateException(
          "AnyLLM workspace chat response parsing failed: " + e.getMessage(), e);
    }
  }

  private static String normalizeWorkspaceChatUrl(String baseUrl, String workspaceId) {
    String b = baseUrl.trim();
    if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
    return b + "/workspace/" + workspaceId + "/chat";
  }

  public record WorkspaceChatRequest(String message, String mode, String sessionId, boolean reset) {}

  public static class WorkspaceChatResponse {
    public String id;
    public String type;
    public Boolean close;
    public String error;
    public Long chatId;
    public String textResponse;
    public List<Source> sources;
    public Metrics metrics;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Source {
    public String id;
    public String url;
    public String title;
    public String description;
    public String text;
    public Double score;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Metrics {
    public Integer prompt_tokens;
    public Integer completion_tokens;
    public Integer total_tokens;
    public Double outputTps;
    public Double duration;
    public String model;
    public String timestamp;
  }
}
