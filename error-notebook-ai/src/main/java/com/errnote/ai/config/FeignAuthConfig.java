package com.errnote.ai.config;

import feign.Request;
import feign.RequestInterceptor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign 定制（不含 @Configuration：仅作用于 OpenAiModelClient，不全局生效）。
 *
 * <p>超时：连接 5s / 读取 90s（架构 §5.2，yml app.ai.connect-timeout-ms / read-timeout-ms）。 鉴权：Authorization
 * Bearer（api-key 仅环境变量注入；日志只打 base-url 与模型名，TC-S-03）。
 */
public class FeignAuthConfig {

  @Bean
  public RequestInterceptor apiKeyInterceptor(@Value("${app.ai.api-key:}") String apiKey) {
    return template -> template.header("Authorization", "Bearer " + apiKey);
  }

  @Bean
  public Request.Options feignOptions(
      @Value("${app.ai.connect-timeout-ms:5000}") int connectTimeoutMs,
      @Value("${app.ai.read-timeout-ms:90000}") int readTimeoutMs) {
    return new Request.Options(
        connectTimeoutMs, TimeUnit.MILLISECONDS, readTimeoutMs, TimeUnit.MILLISECONDS, true);
  }
}
