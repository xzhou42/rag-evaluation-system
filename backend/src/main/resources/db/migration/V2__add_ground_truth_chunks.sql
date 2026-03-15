-- 添加groundTruthChunkIds字段到test_case表
ALTER TABLE test_case ADD COLUMN ground_truth_chunk_ids JSON NULL COMMENT '作为ground truth的chunk ID列表，JSON数组格式';

-- 创建索引以提高查询性能
CREATE INDEX idx_test_case_dataset_id ON test_case(dataset_id);
