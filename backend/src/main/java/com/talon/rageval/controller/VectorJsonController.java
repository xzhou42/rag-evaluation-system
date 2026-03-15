package com.talon.rageval.controller;

import com.talon.rageval.service.rag.VectorDataJsonService;
import com.talon.rageval.service.rag.VectorDataJsonService.VectorRecord;
import com.talon.rageval.service.rag.VectorDataJsonService.VectorDataStats;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/vector-json")
@RequiredArgsConstructor
public class VectorJsonController {

  private final VectorDataJsonService vectorDataJsonService;

  /**
   * 测试文件路径是否存在
   */
  @GetMapping("/test-path")
  public ResponseEntity<?> testPath(@RequestParam String filePath) {
    try {
      String normalizedPath = filePath.trim();
      java.io.File file = new java.io.File(normalizedPath);
      
      Map<String, Object> response = new HashMap<>();
      response.put("inputPath", filePath);
      response.put("normalizedPath", normalizedPath);
      response.put("absolutePath", file.getAbsolutePath());
      response.put("exists", file.exists());
      response.put("isFile", file.isFile());
      response.put("canRead", file.canRead());
      response.put("length", file.length());
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", e.getMessage()
      ));
    }
  }

  /**
   * 从JSON文件读取向量数据
   * @param filePath JSON文件的完整路径
   * @return 向量数据列表
   */
  @GetMapping("/read")
  public ResponseEntity<?> readVectorData(@RequestParam String filePath) {
    try {
      List<VectorRecord> data = vectorDataJsonService.readVectorDataFromFile(filePath);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("total", data.size());
      response.put("data", data);
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      log.error("读取向量数据失败", e);
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "error", e.getMessage()
      ));
    }
  }

  /**
   * 从JSON文件读取向量数据（分页）
   * @param filePath JSON文件的完整路径
   * @param limit 每页数量
   * @param offset 偏移量
   * @return 分页后的向量数据
   */
  @GetMapping("/read-paginated")
  public ResponseEntity<?> readVectorDataPaginated(
      @RequestParam String filePath,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "0") int offset) {
    try {
      List<VectorRecord> data = vectorDataJsonService.readVectorDataFromFile(filePath, limit, offset);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("limit", limit);
      response.put("offset", offset);
      response.put("count", data.size());
      response.put("data", data);
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      log.error("读取向量数据失败", e);
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "error", e.getMessage()
      ));
    }
  }

  /**
   * 获取向量数据统计信息
   * @param filePath JSON文件的完整路径
   * @return 统计信息
   */
  @GetMapping("/stats")
  public ResponseEntity<?> getStats(@RequestParam String filePath) {
    try {
      VectorDataStats stats = vectorDataJsonService.getStats(filePath);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("stats", stats);
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      log.error("获取统计信息失败", e);
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "error", e.getMessage()
      ));
    }
  }

  /**
   * POST方式读取向量数据（便于前端传递文件路径）
   */
  @PostMapping("/read")
  public ResponseEntity<?> readVectorDataPost(@RequestBody VectorDataRequest request) {
    try {
      List<VectorRecord> data = vectorDataJsonService.readVectorDataFromFile(request.filePath);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("total", data.size());
      response.put("data", data);
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      log.error("读取向量数据失败", e);
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "error", e.getMessage()
      ));
    }
  }

  /**
   * POST方式读取向量数据（分页）
   */
  @PostMapping("/read-paginated")
  public ResponseEntity<?> readVectorDataPaginatedPost(@RequestBody VectorDataPaginatedRequest request) {
    try {
      List<VectorRecord> data =
          vectorDataJsonService.readVectorDataFromFile(request.filePath, request.limit, request.offset);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("limit", request.limit);
      response.put("offset", request.offset);
      response.put("count", data.size());
      response.put("data", data);
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      log.error("读取向量数据失败", e);
      return ResponseEntity.badRequest().body(Map.of(
          "success", false,
          "error", e.getMessage()
      ));
    }
  }

  // DTO 类
  @lombok.Data
  public static class VectorDataRequest {
    public String filePath;
  }

  @lombok.Data
  public static class VectorDataPaginatedRequest {
    public String filePath;
    public int limit = 10;
    public int offset = 0;
  }
}
