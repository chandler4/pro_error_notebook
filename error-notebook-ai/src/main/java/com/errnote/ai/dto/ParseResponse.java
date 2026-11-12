package com.errnote.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 内部契约响应 data（api-contract §7.1）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResponse {

  private WrongQuestionDraft draft;

  /** 模型返回原文全文（BE 落 business_log 用，BR-09/TC-G-03）。 */
  private String rawModelResponse;

  private String model;

  private Long durationMs;
}
