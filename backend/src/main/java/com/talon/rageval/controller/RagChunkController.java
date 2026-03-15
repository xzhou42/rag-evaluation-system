package com.talon.rageval.controller;

import com.talon.rageval.service.rag.RagChunkService;
import com.talon.rageval.service.rag.RagChunkService.ChunkInfo;
import com.talon.rageval.service.rag.RagChunkService.VectorChunkInfo;
import java.util.List;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag-chunks")
public class RagChunkController {

  private final RagChunkService ragChunkService;

  public RagChunkController(RagChunkService ragChunkService) {
    this.ragChunkService = ragChunkService;
  }

  /**
   * 获取 workspace 中所有的 chunk
   */
  @PostMapping("/list")
  public ChunkListResponse listChunks(@RequestBody ChunkListRequest request) {
    List<ChunkInfo> chunks = ragChunkService.getAllChunks(request.workspaceId);

    ChunkListResponse response = new ChunkListResponse();
    response.chunks = chunks;
    response.count = chunks.size();
    return response;
  }

  /**
   * 获取 workspace 中所有的 chunk（分页）
   */
  @PostMapping("/list-paginated")
  public ChunkListResponse listChunksPaginated(@RequestBody ChunkListPaginatedRequest request) {
    List<ChunkInfo> chunks =
        ragChunkService.getAllChunks(request.workspaceId, request.limit, request.offset);

    ChunkListResponse response = new ChunkListResponse();
    response.chunks = chunks;
    response.count = chunks.size();
    return response;
  }

  /**
   * 获取 workspace 中所有的向量数据（包含向量值和元数据）
   */
  @PostMapping("/vector-data")
  public VectorDataListResponse getVectorData(@RequestBody VectorDataRequest request) {
    List<List<VectorChunkInfo>> vectorData = ragChunkService.getAllVectorData(request.jsonFilePath);

    VectorDataListResponse response = new VectorDataListResponse();
    response.data = vectorData;
    response.count = vectorData.stream().mapToInt(List::size).sum();
    return response;
  }

  /**
   * 获取 workspace 中所有的向量数据（分页）
   */
  @PostMapping("/vector-data-paginated")
  public VectorDataListResponse getVectorDataPaginated(@RequestBody VectorDataPaginatedRequest request) {
    List<List<VectorChunkInfo>> vectorData =
        ragChunkService.getAllVectorData(request.jsonFilePath, request.limit, request.offset);

    VectorDataListResponse response = new VectorDataListResponse();
    response.data = vectorData;
    response.count = vectorData.stream().mapToInt(List::size).sum();
    return response;
  }

  // DTO 类
  @Data
  public static class ChunkListRequest {
    public String workspaceId;
  }

  @Data
  public static class ChunkListPaginatedRequest {
    public String workspaceId;
    public int limit;
    public int offset;
  }

  @Data
  public static class VectorDataRequest {
    public String jsonFilePath;
  }

  @Data
  public static class VectorDataPaginatedRequest {
    public String jsonFilePath;
    public int limit = 10;
    public int offset = 0;
  }

  @Data
  public static class ChunkListResponse {
    public List<ChunkInfo> chunks;
    public int count;
  }

  @Data
  public static class VectorDataListResponse {
    public List<List<VectorChunkInfo>> data;
    public int count;
  }
}
