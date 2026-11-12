package com.errnote.ai.client;

import com.errnote.ai.client.openai.ChatCompletionRequest;
import com.errnote.ai.client.openai.ChatCompletionResponse;
import com.errnote.ai.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 模型客户端（OpenAI 兼容原生同构，2026-11-11 16:04 决策）：智谱 GLM Coding Plan
 * glm-5.3-flash（https://open.bigmodel.cn/api/coding/paas/v4/chat/completions）。
 *
 * <p>鉴权 Bearer 由 FeignAuthConfig 注入；连接 5s / 读取 90s；日志脱敏——客户端层不打印 api-key （security.md §5 /
 * TC-S-03）。
 */
@FeignClient(
    name = "openai-model",
    url = "${app.ai.base-url}",
    configuration = FeignAuthConfig.class)
public interface OpenAiModelClient {

  @PostMapping("/chat/completions")
  ChatCompletionResponse chat(@RequestBody ChatCompletionRequest request);
}
