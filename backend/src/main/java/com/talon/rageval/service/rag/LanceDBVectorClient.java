package com.talon.rageval.service.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * LanceDB 向量数据客户端
 * 用于获取向量数据库中的完整向量数据（包含向量值和元数据）
 */
@Service
public class LanceDBVectorClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private static final String LANCEDB_SERVICE_URL = "http://localhost:8002";

  public LanceDBVectorClient(RestClient.Builder builder) {
    SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
    rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
    rf.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
    this.restClient = builder.requestFactory(rf).build();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * 获取 workspace 中的所有向量数据
   */
  public VectorDataResponse getAllVectorData(String workspaceId) {
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

      return objectMapper.readValue(responseBody, VectorDataResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to fetch vector data from LanceDB: " + e.getMessage(), e);
    }
  }

  /**
   * 获取 workspace 中的向量数据（分页）
   */
  public VectorDataResponse getAllVectorData(String workspaceId, int limit, int offset) {
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

      return objectMapper.readValue(responseBody, VectorDataResponse.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to fetch vector data from LanceDB: " + e.getMessage(), e);
    }
  }

  // DTO 类
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class VectorDataResponse {
    public String workspace_id;
    public String table;
    public List<VectorRecord> chunks;
    public int count;
    public int total;
    public int limit;
    public int offset;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class VectorRecord {
    public String id;
    public List<Double> vector;
    public String content;
    public java.util.Map<String, Object> metadata;
  }
}
