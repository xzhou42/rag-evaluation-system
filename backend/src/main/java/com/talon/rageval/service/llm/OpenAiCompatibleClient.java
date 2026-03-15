package com.talon.rageval.service.llm;

import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
public class OpenAiCompatibleClient implements LlmClient {

  private final RestClient restClient;

  public OpenAiCompatibleClient(RestClient.Builder builder) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) Duration.ofSeconds(20).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(120).toMillis());
    this.restClient =
        builder
            .requestFactory(rf)
            .build();
  }

  @Override
  public String answerQuestion(LlmConfig config, String question) {
    return chat(
        "You are a helpful assistant. Answer the user's question clearly.",
        question,
        config);
  }

  @Override
  public String generateQuestionFromAnswer(LlmConfig config, String answer) {
    String sys =
        "You generate a single concise question that can be answered by the given answer. "
            + "Return only the question text, no quotes, no extra formatting.";
    String user = "Answer:\n" + answer;
    return chat(sys, user, config);
  }

  private String chat(String systemPrompt, String userPrompt, LlmConfig cfg) {
    String url = normalizeChatCompletionsUrl(cfg.baseUrl());
    ChatCompletionsRequest req =
        new ChatCompletionsRequest(
            cfg.chatModel(),
            cfg.resolvedTemperature(),
            List.of(new Message("system", systemPrompt), new Message("user", userPrompt)));

    try {
      ChatCompletionsResponse resp =
          restClient
              .post()
              .uri(url)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.apiKey())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .body(req)
              .retrieve()
              .body(ChatCompletionsResponse.class);

      if (resp == null || resp.choices == null || resp.choices.isEmpty()) {
        throw new IllegalStateException("empty response from LLM");
      }
      String content = resp.choices.get(0).message == null ? null : resp.choices.get(0).message.content;
      if (content == null) {
        throw new IllegalStateException("missing content in LLM response");
      }
      return content.trim();
    } catch (RestClientException e) {
      throw new IllegalStateException("LLM request failed: " + e.getMessage(), e);
    }
  }

  private static String normalizeChatCompletionsUrl(String baseUrl) {
    String b = baseUrl.trim();
    if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
    if (b.endsWith("/v1")) {
      return b + "/chat/completions";
    }
    if (b.contains("/v1/")) {
      // user passed full endpoint path
      return b;
    }
    return b + "/v1/chat/completions";
  }

  // Minimal OpenAI-compatible DTOs
  public record Message(String role, String content) {}

  public record ChatCompletionsRequest(String model, double temperature, List<Message> messages) {}

  public static class ChatCompletionsResponse {
    public List<Choice> choices;
  }

  public static class Choice {
    public Message message;
  }
}

