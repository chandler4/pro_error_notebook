package com.errnote.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 基础装配：Clock 注入点（test-strategy §2：时间在测试中可控可断言）。 */
@Configuration
public class AppConfig {

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
