package com.errnote.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * W-5（2026-11-13 chandler 确认）：超时阶梯化防同值竞速。
 *
 * <p>AI 模型读超时 90s（error-notebook-ai AiModelProperties#readTimeoutMs 默认值）必须严格小于 BE Feign
 * 读超时（100s），否则长耗时真调时 AI 的错误信封（42001/42002/40000 + 明细，契约 v1.7）与 BE 传输超时同刻竞速， BE 抢先归为传输失败导致明细丢失（走查
 * W-5：90015ms 竞速复现）。同时 BE 100s 必须小于 FE 契约超时 120s （api-contract §5），保证错误响应能在前端超时前送达。
 */
class FeignTimeoutLadderTest {

  private static final long AI_MODEL_READ_TIMEOUT_MS = 90_000L; // AI 服务模型读超时（同步维护）
  private static final long FE_PARSE_TIMEOUT_MS = 120_000L; // 前端解析链路超时（契约 §5）

  @Test
  @DisplayName("W-5 超时阶梯：AI 90s < BE Feign 100s < FE 120s，信封永远先到")
  void beFeignReadTimeoutLeavesHeadroomAboveAiModelCap() {
    Properties props = applicationYmlProperties();

    long connect = Long.parseLong(props.getProperty("feign.client.config.default.connect-timeout"));
    long read = Long.parseLong(props.getProperty("feign.client.config.default.read-timeout"));

    assertThat(connect).isEqualTo(5_000L);
    assertThat(read)
        .as("BE Feign read-timeout 必须大于 AI 模型读超时（防竞速丢信封明细）")
        .isGreaterThan(AI_MODEL_READ_TIMEOUT_MS)
        .as("BE Feign read-timeout 必须小于前端解析超时（错误响应先于 FE 超时送达）")
        .isLessThan(FE_PARSE_TIMEOUT_MS)
        .as("W-5 落档值（改动需架构 §2.1/§5.7 同步并沟通记录留痕）")
        .isEqualTo(100_000L);
  }

  private Properties applicationYmlProperties() {
    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new ClassPathResource("application.yml"));
    Properties props = yaml.getObject();
    assertThat(props).isNotNull();
    return props;
  }
}
