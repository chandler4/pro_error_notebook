package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.integration.ai.AiServiceClient;
import com.errnote.integration.ai.dto.AiParseRequest;
import com.errnote.integration.ai.dto.AiParseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * T0-2 联通演示端点：FE → BE → AI(mock)，浏览器演示页端到端返回 mock 数据。
 *
 * <p>临时切片：正式解析接口 POST /api/v1/questions/parse（草稿入库 + recordAi）随 B-4 落地后移除本类。
 */
@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
@Tag(name = "demo", description = "T0-2 走通切片（临时，B-4 后移除）")
public class DemoController {

  private final AiServiceClient aiServiceClient;

  @PostMapping("/parse")
  @Operation(summary = "透传 AI 服务内部契约 mock 响应")
  public ApiResponse<AiParseResponse> parse(@RequestBody AiParseRequest request) {
    return aiServiceClient.parse(request);
  }
}
