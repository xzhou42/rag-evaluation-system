package com.talon.rageval.service.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 向量数据JSON文件读取服务
 * 直接从JSON文件读取向量数据，无需依赖LanceDB
 */
@Slf4j
@Service
public class VectorDataJsonService {

  private final ObjectMapper objectMapper;

  public VectorDataJsonService() {
    this.objectMapper = new ObjectMapper();
  }

  /**
   * 从JSON文件读取向量数据
   * @param filePath JSON文件路径
   * @return 向量数据列表
   */
  public List<VectorRecord> readVectorDataFromFile(String filePath) throws IOException {
    // 处理路径中的空格和特殊字符
    String normalizedPath = filePath.trim();
    File file = new File(normalizedPath);
    
    log.info("尝试读取文件: {}", normalizedPath);
    log.info("文件是否存在: {}", file.exists());
    log.info("文件绝对路径: {}", file.getAbsolutePath());
    
    if (!file.exists()) {
      throw new IOException("文件不存在: " + normalizedPath + " (绝对路径: " + file.getAbsolutePath() + ")");
    }

    try {
      // 读取嵌套的向量数据结构 [[{...}, {...}]]
      List<List<VectorRecord>> nestedData =
          objectMapper.readValue(file, new TypeReference<List<List<VectorRecord>>>() {});

      // 展平为单个列表
      List<VectorRecord> flatData = new ArrayList<>();
      for (List<VectorRecord> batch : nestedData) {
        flatData.addAll(batch);
      }
      log.info("成功读取 {} 条向量数据", flatData.size());
      return flatData;
    } catch (IOException e) {
      log.error("读取向量数据文件失败: {}", normalizedPath, e);
      throw e;
    }
  }

  /**
   * 从JSON文件读取向量数据（分页）
   * @param filePath JSON文件路径
   * @param limit 每页数量
   * @param offset 偏移量
   * @return 分页后的向量数据
   */
  public List<VectorRecord> readVectorDataFromFile(String filePath, int limit, int offset)
      throws IOException {
    List<VectorRecord> allData = readVectorDataFromFile(filePath);

    int start = Math.min(offset, allData.size());
    int end = Math.min(start + limit, allData.size());

    return allData.subList(start, end);
  }

  /**
   * 获取JSON文件中的向量数据统计信息
   */
  public VectorDataStats getStats(String filePath) throws IOException {
    List<VectorRecord> data = readVectorDataFromFile(filePath);

    VectorDataStats stats = new VectorDataStats();
    stats.totalVectors = data.size();

    if (!data.isEmpty() && data.get(0).values != null) {
      stats.vectorDimensions = data.get(0).values.size();
    }

    return stats;
  }

  // DTO 类
  public static class VectorRecord {
    public String id;
    public List<Double> values;
    public Map<String, Object> metadata;
  }

  public static class VectorDataStats {
    public int totalVectors;
    public int vectorDimensions;
  }
}
