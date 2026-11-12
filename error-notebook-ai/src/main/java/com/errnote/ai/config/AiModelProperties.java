package com.errnote.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型配置（application.yml app.ai.*）：智谱 GLM Coding Plan · glm-5.3-flash（2026-11-12 选型）。
 *
 * <p>api-key 仅环境变量注入（security.md §5）；采样参数为官方推荐值（思考型模型，A-4 任务留痕）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiModelProperties {

  private String baseUrl = "https://open.bigmodel.cn/api/coding/paas/v4";

  private String apiKey = "";

  private String model = "glm-5.3-flash";

  private Double temperature = 1.0;

  private Double topP = 0.95;

  private Integer connectTimeoutMs = 5000;

  private Integer readTimeoutMs = 90000;

  /** BE 静态图片访问（架构 §2.1 链路④：回环拉取处理图）。 */
  private String beBaseUrl = "http://127.0.0.1:8080";
}
