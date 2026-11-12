package com.errnote.ai.controller;

import com.errnote.ai.acl.OpenAiVisionAdapter;
import com.errnote.ai.common.ApiResponse;
import com.errnote.ai.dto.ParseRequest;
import com.errnote.ai.dto.ParseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部契约端点（api-contract §7.1）：POST /internal/parse。
 *
 * <p>Controller 只做协议适配：参数校验 → 防腐层（Prompt 组装/模型调用/严格 JSON 校验/重试/错误映射在 A-2~A-4 落地）；异常由
 * GlobalExceptionHandler 统一转 {code, message, data} 信封，HTTP 恒 200。
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@Tag(name = "internal", description = "BE↔AI 内部契约（仅回环 127.0.0.1:8081，无鉴权）")
public class ParseController {

  private final OpenAiVisionAdapter visionAdapter;

  public ParseController(OpenAiVisionAdapter visionAdapter) {
    this.visionAdapter = visionAdapter;
  }

  @PostMapping("/parse")
  @Operation(summary = "错题解析：多模态模型调用 + 严格 JSON 校验（失败重试 1 次），42001/42002 见统一错误码表")
  public ApiResponse<ParseResponse> parse(@Valid @RequestBody ParseRequest request) {
    log.info("parse request images={} ", request.getImages().size());
    return ApiResponse.ok(visionAdapter.parse(request.getImages()));
  }
}
