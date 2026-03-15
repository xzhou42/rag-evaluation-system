package com.talon.rageval.service.rag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG Chunk 管理服务
 * 用于获取和管理 AnythingLLM 中的 chunk 信息
 */
@Slf4j
@Service
public class RagChunkService {

  private final AnythingLLMDocumentClient documentClient;
  private final VectorDataJsonService vectorDataJsonService;

  public RagChunkService(AnythingLLMDocumentClient documentClient, VectorDataJsonService vectorDataJsonService) {
    this.documentClient = documentClient;
    this.vectorDataJsonService = vectorDataJsonService;
  }

  /**
   * 获取 workspace 中所有的 chunk
   */
  public List<ChunkInfo> getAllChunks(String workspaceId) {
    List<ChunkInfo> allChunks = new ArrayList<>();

    try {
      // 从 LanceDB 获取所有 chunk
      AnythingLLMDocumentClient.ChunkListResponse response =
          documentClient.getAllChunks(workspaceId);

      if (response.chunks == null) {
        return allChunks;
      }

      for (AnythingLLMDocumentClient.Chunk chunk : response.chunks) {
        ChunkInfo chunkInfo = new ChunkInfo();
        chunkInfo.id = chunk.id;
        chunkInfo.content = chunk.content;
        chunkInfo.vectorSize = chunk.vector_size;

        allChunks.add(chunkInfo);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch chunks: " + e.getMessage(), e);
    }

    return allChunks;
  }

  /**
   * 获取 workspace 中所有的 chunk（分页）
   */
  public List<ChunkInfo> getAllChunks(String workspaceId, int limit, int offset) {
    List<ChunkInfo> allChunks = new ArrayList<>();

    try {
      // 从 LanceDB 获取 chunk（分页）
      AnythingLLMDocumentClient.ChunkListResponse response =
          documentClient.getAllChunks(workspaceId, limit, offset);

      if (response.chunks == null) {
        return allChunks;
      }

      for (AnythingLLMDocumentClient.Chunk chunk : response.chunks) {
        ChunkInfo chunkInfo = new ChunkInfo();
        chunkInfo.id = chunk.id;
        chunkInfo.content = chunk.content;
        chunkInfo.vectorSize = chunk.vector_size;

        allChunks.add(chunkInfo);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to fetch chunks: " + e.getMessage(), e);
    }

    return allChunks;
  }

  /**
   * 从JSON文件读取向量数据
   */
  public List<List<VectorChunkInfo>> getAllVectorData(String jsonFilePath) {
    try {
      List<VectorDataJsonService.VectorRecord> records =
          vectorDataJsonService.readVectorDataFromFile(jsonFilePath);

      List<List<VectorChunkInfo>> result = new ArrayList<>();
      List<VectorChunkInfo> batchInfo = new ArrayList<>();

      for (VectorDataJsonService.VectorRecord record : records) {
        VectorChunkInfo info = new VectorChunkInfo();
        info.id = record.id;
        info.values = record.values != null ? record.values : new ArrayList<>();
        info.metadata = record.metadata;
        batchInfo.add(info);
      }

      result.add(batchInfo);
      return result;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch vector data: " + e.getMessage(), e);
    }
  }

  /**
   * 从JSON文件读取向量数据（分页）
   */
  public List<List<VectorChunkInfo>> getAllVectorData(String jsonFilePath, int limit, int offset) {
    try {
      List<VectorDataJsonService.VectorRecord> records =
          vectorDataJsonService.readVectorDataFromFile(jsonFilePath, limit, offset);

      List<List<VectorChunkInfo>> result = new ArrayList<>();
      List<VectorChunkInfo> batchInfo = new ArrayList<>();

      for (VectorDataJsonService.VectorRecord record : records) {
        VectorChunkInfo info = new VectorChunkInfo();
        info.id = record.id;
        info.values = record.values != null ? record.values : new ArrayList<>();
        info.metadata = record.metadata;
        batchInfo.add(info);
      }

      result.add(batchInfo);
      return result;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch vector data: " + e.getMessage(), e);
    }
  }

  // DTO 类
  public static class ChunkInfo {
    public String id;
    public String content;
    public int vectorSize;

    @Override
    public String toString() {
      return "ChunkInfo{"
          + "id='"
          + id
          + '\''
          + ", vectorSize="
          + vectorSize
          + '}';
    }
  }

  public static class VectorChunkInfo {
    public String id;
    public List<Double> values;
    public Map<String, Object> metadata;
  }
}
