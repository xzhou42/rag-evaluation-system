package com.talon.rageval.service.llm;

public interface LlmClient {
  String answerQuestion(LlmConfig config, String question);

  String generateQuestionFromAnswer(LlmConfig config, String answer);
}

