package com.talon.rageval.repository;

import com.talon.rageval.entity.EvalResultEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvalResultRepository extends JpaRepository<EvalResultEntity, Long> {
  Page<EvalResultEntity> findByJob_Id(Long jobId, Pageable pageable);
}

