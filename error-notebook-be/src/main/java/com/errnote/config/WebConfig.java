package com.errnote.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 静态资源映射（B-5）：FE 图片回显与 AI 服务回环拉取共用 GET /files/images/**（架构 §2.1）。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${data.dir}")
  private String dataDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location =
        Paths.get(dataDir, "images").toAbsolutePath().normalize().toString().replace('\\', '/')
            + "/";
    registry.addResourceHandler("/files/images/**").addResourceLocations("file:" + location);
  }
}
