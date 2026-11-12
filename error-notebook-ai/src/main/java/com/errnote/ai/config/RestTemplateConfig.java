package com.errnote.ai.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RestTemplate（图片回环拉取用）：连接 5s / 读取 30s（本机回环，快失败）。 */
@Configuration
public class RestTemplateConfig {

  @Bean
  public org.springframework.web.client.RestTemplate imageRestTemplate(
      RestTemplateBuilder builder,
      @Value("${app.ai.connect-timeout-ms:5000}") int connectTimeoutMs) {
    return builder
        .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
        .setReadTimeout(Duration.ofMillis(30000))
        .build();
  }
}
