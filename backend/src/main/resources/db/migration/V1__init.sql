CREATE TABLE dataset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_case (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dataset_id BIGINT NOT NULL,
  question TEXT NOT NULL,
  reference_answer TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_case_dataset FOREIGN KEY (dataset_id) REFERENCES dataset(id) ON DELETE CASCADE
);

CREATE TABLE eval_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dataset_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  progress INT NOT NULL DEFAULT 0,
  total_cases INT NOT NULL DEFAULT 0,
  llm_config_snapshot JSON NULL,
  error_message TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP NULL,
  CONSTRAINT fk_eval_job_dataset FOREIGN KEY (dataset_id) REFERENCES dataset(id) ON DELETE CASCADE
);

CREATE TABLE eval_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  test_case_id BIGINT NOT NULL,
  model_answer TEXT NULL,
  answer_relevancy_score DOUBLE NULL,
  generated_questions JSON NULL,
  per_gen_similarities JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eval_result_job FOREIGN KEY (job_id) REFERENCES eval_job(id) ON DELETE CASCADE,
  CONSTRAINT fk_eval_result_case FOREIGN KEY (test_case_id) REFERENCES test_case(id) ON DELETE CASCADE,
  UNIQUE KEY uk_eval_result_job_case (job_id, test_case_id)
);
