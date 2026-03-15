package com.talon.rageval.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talon.rageval.model.VectorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class VectorDataReader {

    private final ObjectMapper objectMapper;

    public VectorDataReader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 读取向量数据JSON文件
     * @param filePath JSON文件路径
     * @return 向量数据列表的列表（支持嵌套数组结构）
     */
    public List<List<VectorData>> readVectorDataFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        try {
            // 读取嵌套的向量数据结构 [[{...}, {...}]]
            return objectMapper.readValue(file, new TypeReference<List<List<VectorData>>>() {});
        } catch (IOException e) {
            log.error("读取向量数据文件失败: {}", filePath, e);
            throw e;
        }
    }

    /**
     * 读取单层向量数据数组
     * @param filePath JSON文件路径
     * @return 向量数据列表
     */
    public List<VectorData> readFlatVectorData(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }

        try {
            return objectMapper.readValue(file, new TypeReference<List<VectorData>>() {});
        } catch (IOException e) {
            log.error("读取向量数据文件失败: {}", filePath, e);
            throw e;
        }
    }

    /**
     * 从JSON字符串解析向量数据
     * @param jsonString JSON字符串
     * @return 向量数据列表的列表
     */
    public List<List<VectorData>> parseVectorDataString(String jsonString) throws IOException {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<List<VectorData>>>() {});
        } catch (IOException e) {
            log.error("解析向量数据字符串失败", e);
            throw e;
        }
    }

    /**
     * 提取文档元数据
     * @param vectorData 向量数据对象
     * @return 格式化的元数据信息
     */
    public String extractMetadataInfo(VectorData vectorData) {
        Map<String, Object> metadata = vectorData.getMetadata();
        if (metadata == null) {
            return "无元数据";
        }

        StringBuilder info = new StringBuilder();
        info.append("文档ID: ").append(metadata.get("id")).append("\n");
        info.append("标题: ").append(metadata.get("title")).append("\n");
        info.append("描述: ").append(metadata.get("description")).append("\n");
        info.append("作者: ").append(metadata.get("docAuthor")).append("\n");
        info.append("来源: ").append(metadata.get("docSource")).append("\n");
        info.append("发布时间: ").append(metadata.get("published")).append("\n");
        info.append("字数: ").append(metadata.get("wordCount")).append("\n");
        info.append("Token估计: ").append(metadata.get("token_count_estimate")).append("\n");

        return info.toString();
    }

    /**
     * 提取文档文本内容
     * @param vectorData 向量数据对象
     * @return 文本内容
     */
    public String extractText(VectorData vectorData) {
        Map<String, Object> metadata = vectorData.getMetadata();
        if (metadata != null && metadata.containsKey("text")) {
            return (String) metadata.get("text");
        }
        return "";
    }
}
