package com.errnote.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A-2 Feign 超时配置（架构 §5.2：连接 5s / 读取 90s）与鉴权头。 */
class FeignAuthConfigTest {

  private final FeignAuthConfig config = new FeignAuthConfig();

  @Test
  @DisplayName("超时配置：connect 5000ms / read 90000ms（yml app.ai.*）")
  void feignTimeouts() {
    Request.Options options = config.feignOptions(5000, 90000);
    assertThat(options.connectTimeout()).isEqualTo(5000);
    assertThat(options.readTimeout()).isEqualTo(90000);
  }

  @Test
  @DisplayName("鉴权拦截器：Authorization Bearer 注入（日志脱敏见 TC-S-03，key 不打印）")
  void authInterceptorAddsBearerHeader() {
    feign.RequestInterceptor interceptor = config.apiKeyInterceptor("sk-test-key");
    feign.RequestTemplate template = new feign.RequestTemplate();
    interceptor.apply(template);
    assertThat(template.headers().get("Authorization")).containsExactly("Bearer sk-test-key");
  }
}
