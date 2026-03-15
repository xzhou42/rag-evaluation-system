package com.talon.rageval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "eval_result")
public class EvalResultEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "job_id", nullable = false)
  private EvalJobEntity job;

  @ManyToOne(optional = false)
  @JoinColumn(name = "test_case_id", nullable = false)
  private TestCaseEntity testCase;

  @Column(name = "model_answer", columnDefinition = "text")
  private String modelAnswer;

  @Column(name = "answer_relevancy_score")
  private Double answerRelevancyScore;

  @Column(name = "generated_questions", columnDefinition = "json")
  private String generatedQuestions;

  @Column(name = "per_gen_similarities", columnDefinition = "json")
  private String perGenSimilarities;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}

