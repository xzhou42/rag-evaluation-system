package com.talon.rageval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "eval_job")
public class EvalJobEntity {

  public enum Status {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "dataset_id", nullable = false)
  private DatasetEntity dataset;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Status status;

  @Column(nullable = false)
  private int progress;

  @Column(name = "total_cases", nullable = false)
  private int totalCases;

  @Column(name = "llm_config_snapshot", columnDefinition = "json")
  private String llmConfigSnapshot;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (status == null) {
      status = Status.PENDING;
    }
  }
}

