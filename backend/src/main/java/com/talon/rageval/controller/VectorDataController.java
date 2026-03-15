package com.talon.rageval.controller;

import com.talon.rageval.model.VectorData;
import com.talon.rageval.util.VectorDataReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
public class VectorDataController {

    private final VectorDataReader vectorDataReader;

    /**
     * 读取向量数据文件
     * @param filePath 文件路径
     * @return 向量数据列表
     */
    @GetMapping("/read")
    public ResponseEntity<?> readVectorData(@RequestParam String filePath) {
        try {
            List<List<VectorData>> data = vectorDataReader.readVectorDataFile(filePath);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalBatches", data.size());
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
     * 读取单层向量数据
     * @param filePath 文件路径
     * @return 向量数据列表
     */
    @GetMapping("/read-flat")
    public ResponseEntity<?> readFlatVectorData(@RequestParam String filePath) {
        try {
            List<VectorData> data = vectorDataReader.readFlatVectorData(filePath);
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
     * 获取向量数据统计信息
     * @param filePath 文件路径
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getVectorStats(@RequestParam String filePath) {
        try {
            List<List<VectorData>> data = vectorDataReader.readVectorDataFile(filePath);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBatches", data.size());
            
            int totalVectors = 0;
            int totalDimensions = 0;
            
            for (List<VectorData> batch : data) {
                totalVectors += batch.size();
                if (!batch.isEmpty() && !batch.get(0).getValues().isEmpty()) {
                    totalDimensions = batch.get(0).getValues().size();
                }
            }
            
            stats.put("totalVectors", totalVectors);
            stats.put("vectorDimensions", totalDimensions);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", stats
            ));
        } catch (IOException e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取向量数据的元数据
     * @param filePath 文件路径
     * @param batchIndex 批次索引
     * @param vectorIndex 向量索引
     * @return 元数据信息
     */
    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata(
            @RequestParam String filePath,
            @RequestParam int batchIndex,
            @RequestParam int vectorIndex) {
        try {
            List<List<VectorData>> data = vectorDataReader.readVectorDataFile(filePath);
            
            if (batchIndex >= data.size()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "批次索引超出范围"
                ));
            }
            
            List<VectorData> batch = data.get(batchIndex);
            if (vectorIndex >= batch.size()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "向量索引超出范围"
                ));
            }
            
            VectorData vectorData = batch.get(vectorIndex);
            String metadataInfo = vectorDataReader.extractMetadataInfo(vectorData);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "metadata", metadataInfo,
                "vectorId", vectorData.getId()
            ));
        } catch (IOException e) {
            log.error("获取元数据失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
