package com.errnote.ai.config;

import com.errnote.ai.client.ChatRequestFactory;
import com.errnote.ai.prompt.PromptProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 请求组装工厂装配（systemPrompt 由 PromptProvider 提供，A-4 模板化后自动升级）。 */
@Configuration
public class AiClientConfig {

  @Bean
  public ChatRequestFactory chatRequestFactory(
      AiModelProperties properties, PromptProvider promptProvider) {
    return new ChatRequestFactory(
        properties.getModel(),
        properties.getTemperature(),
        properties.getTopP(),
        promptProvider.systemPrompt(),
        properties.getThinkingType());
  }
}
