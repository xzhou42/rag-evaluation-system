package com.talon.rageval.repository;

import com.talon.rageval.entity.TestCaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseRepository extends JpaRepository<TestCaseEntity, Long> {
  Page<TestCaseEntity> findByDataset_Id(Long datasetId, Pageable pageable);
}

