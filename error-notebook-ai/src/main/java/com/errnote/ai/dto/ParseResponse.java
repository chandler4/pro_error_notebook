package com.errnote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 内部契约响应 data（api-contract §7.1）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "解析结果：草稿 + 模型原文 + 元信息")
public class ParseResponse {

  @Schema(description = "解析草稿（tags 恒空）", required = true)
  private WrongQuestionDraft draft;

  /** 模型返回原文全文（BE 落 business_log 用，BR-09/TC-G-03）。 */
  @Schema(description = "模型返回原文全文（供 BE 落业务日志）", required = true)
  private String rawModelResponse;

  @Schema(description = "模型名（如 glm-5.3-flash）", required = true)
  private String model;

  @Schema(description = "模型调用耗时毫秒", required = true)
  private Long durationMs;
}
