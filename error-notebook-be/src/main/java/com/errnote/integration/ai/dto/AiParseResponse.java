package com.errnote.integration.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 内部契约响应 data（api-contract §7.1）：draft + rawModelResponse + model + durationMs。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParseResponse {

  private WrongQuestionDraftDto draft;

  /** 模型返回原文全文（供 BE 落 business_log，BR-09/TC-G-03）。 */
  private String rawModelResponse;

  private String model;

  private Long durationMs;
}
