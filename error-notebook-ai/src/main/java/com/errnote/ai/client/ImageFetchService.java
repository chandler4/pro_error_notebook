package com.errnote.ai.client;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 图片获取（api-contract §7.1 固化方案 / 架构 §2.1 链路④）：AI 服务回环 GET {be-base-url}/files/{relativePath} 从 BE
 * 静态映射拉取处理图，转 base64 data URL 供模型多模态输入。
 */
@Service
public class ImageFetchService {

  private static final String DATA_URL_TEMPLATE = "data:image/%s;base64,%s";

  private final RestTemplate restTemplate;

  private final String beBaseUrl;

  public ImageFetchService(
      RestTemplate restTemplate, @Value("${app.ai.be-base-url}") String beBaseUrl) {
    this.restTemplate = restTemplate;
    this.beBaseUrl = beBaseUrl;
  }

  /** @param relativePath BE 文件存储相对路径（如 images/202611/a1b2.jpg） */
  public String toDataUrl(String relativePath) {
    byte[] bytes;
    try {
      bytes = restTemplate.getForObject(beBaseUrl + "/files/" + relativePath, byte[].class);
    } catch (RuntimeException e) {
      throw new IllegalStateException("拉取图片失败: " + relativePath, e);
    }
    if (bytes == null) {
      throw new IllegalStateException("拉取图片为空: " + relativePath);
    }
    return String.format(
        DATA_URL_TEMPLATE, mimeOf(relativePath), Base64.getEncoder().encodeToString(bytes));
  }

  private String mimeOf(String path) {
    if (path.toLowerCase().endsWith(".png")) {
      return "png";
    }
    return "jpeg";
  }
}
