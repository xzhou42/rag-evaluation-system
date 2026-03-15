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

/**
 * AnythingLLM 文档管理客户端
 * 用于获取 workspace 中的文档和 chunk 信息
 * 通过本地 LanceDB 服务获取 chunk 数据
 */
@Service
public class AnythingLLMDocumentClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private static final String LANCEDB_SERVICE_URL = "http://localhost:8002";

  public AnythingLLMDocumentClient(RestClient.Builder builder) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
    this.restClient = builder.requestFactory(rf).build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * 获取 workspace 中的所有 chunk（从 LanceDB）
   */
  public ChunkListResponse getAllChunks(String workspaceId) {
    String url = LANCEDB_SERVICE_URL + "/chunks/" + workspaceId;

    try {
      byte[] responseBody =
          restClient
              .get()
              .uri(url)
              .retrieve()
              .body(byte[].class);

      if (responseBody == null || responseBody.length == 0) {
        throw new IllegalStateException("LanceDB service returned empty response");
      }

      return objectMapper.readValue(responseBody, ChunkListResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to fetch chunks from LanceDB: " + e.getMessage(), e);
    }
  }

  /**
   * 获取 workspace 中的所有 chunk（分页）
   */
  public ChunkListResponse getAllChunks(String workspaceId, int limit, int offset) {
    String url = LANCEDB_SERVICE_URL + "/chunks/" + workspaceId + "?limit=" + limit + "&offset=" + offset;

    try {
      byte[] responseBody =
          restClient
              .get()
              .uri(url)
              .retrieve()
              .body(byte[].class);

      if (responseBody == null || responseBody.length == 0) {
        throw new IllegalStateException("LanceDB service returned empty response");
      }

      return objectMapper.readValue(responseBody, ChunkListResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to fetch chunks from LanceDB: " + e.getMessage(), e);
    }
  }

  /**
   * 列出所有可用的 workspace
   */
  public WorkspaceListResponse listWorkspaces() {
    String url = LANCEDB_SERVICE_URL + "/workspaces";

    try {
      byte[] responseBody =
          restClient
              .get()
              .uri(url)
              .retrieve()
              .body(byte[].class);

      if (responseBody == null || responseBody.length == 0) {
        throw new IllegalStateException("LanceDB service returned empty response");
      }

      return objectMapper.readValue(responseBody, WorkspaceListResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to fetch workspaces from LanceDB: " + e.getMessage(), e);
    }
  }

  // DTO 类
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ChunkListResponse {
    public String workspace_id;
    public String table;
    public List<Chunk> chunks;
    public int count;
    public int total;
    public int limit;
    public int offset;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Chunk {
    public String id;
    public String content;
    public Object metadata;
    public int vector_size;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class WorkspaceListResponse {
    public List<Workspace> workspaces;
    public int count;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Workspace {
    public String id;
    public String path;
  }

  // 保留原来的 DTO 类以保持兼容性
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DocumentListResponse {
    public List<Document> documents;
    public int count;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Document {
    public String id;
    public String name;
    public String docSource;
    public String published;
    public int wordCount;
    public int token_count_estimate;
    public List<OldChunk> chunks;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OldChunk {
    public String id;
    public String content;
    public String text;
    public int token_count;
    public Double score;
    public String metadata;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DocumentDetailResponse {
    public Document document;
    public List<OldChunk> chunks;
  }
}
