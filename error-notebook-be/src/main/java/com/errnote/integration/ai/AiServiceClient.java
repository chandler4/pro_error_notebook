package com.errnote.integration.ai;

import com.errnote.common.ApiResponse;
import com.errnote.integration.ai.dto.AiParseRequest;
import com.errnote.integration.ai.dto.AiParseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * BE → AI 服务内部契约客户端（api-contract 第 7 章：POST /internal/parse，仅回环、无鉴权）。
 *
 * <p>超时配置（连接 5s/读取 90s）随 B-4 在 FeignConfig 固化；BE 不直连模型（架构 v3.0 边界）。
 */
@FeignClient(name = "error-notebook-ai", url = "${app.ai.base-url}")
public interface AiServiceClient {

  @PostMapping("/internal/parse")
  ApiResponse<AiParseResponse> parse(@RequestBody AiParseRequest request);
}
