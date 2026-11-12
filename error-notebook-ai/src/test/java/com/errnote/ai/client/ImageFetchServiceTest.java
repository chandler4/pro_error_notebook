package com.errnote.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.errnote.ai.common.BusinessException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

/**
 * A-2 图片获取（架构 §2.1 链路④）：AI 服务回环 GET {be-base-url}/files/images/** 拉取 BE 处理图并转 base64 data
 * URL（api-contract §7.1 固化方案）。
 */
class ImageFetchServiceTest {

  private RestTemplate restTemplate;
  private ImageFetchService service;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    service = new ImageFetchService(restTemplate, "http://127.0.0.1:8080");
  }

  @Test
  @DisplayName("拉取 jpg 转 data URL：mime 为 image/jpeg，base64 与源字节一致")
  void fetchJpgAsDataUrl() {
    byte[] bytes = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);
    given(
            restTemplate.getForObject(
                eq("http://127.0.0.1:8080/files/images/202611/a1b2.jpg"), eq(byte[].class)))
        .willReturn(bytes);

    String dataUrl = service.toDataUrl("images/202611/a1b2.jpg");

    assertThat(dataUrl).startsWith("data:image/jpeg;base64,");
    String base64 = dataUrl.substring("data:image/jpeg;base64,".length());
    assertThat(java.util.Base64.getDecoder().decode(base64)).isEqualTo(bytes);
  }

  @Test
  @DisplayName("拉取 png 转 data URL：mime 为 image/png")
  void fetchPngAsDataUrl() {
    byte[] bytes = new byte[] {1, 2, 3};
    given(
            restTemplate.getForObject(
                eq("http://127.0.0.1:8080/files/images/202611/c3d4.png"), eq(byte[].class)))
        .willReturn(bytes);

    assertThat(service.toDataUrl("images/202611/c3d4.png")).startsWith("data:image/png;base64,");
  }

  @Test
  @DisplayName("W-2：拉取失败映射 40000 参数错误并携带 url（供 BE 透传，可定位失效图片）")
  void fetchFailureThrows40000WithUrl() {
    given(
            restTemplate.getForObject(
                eq("http://127.0.0.1:8080/files/images/x/missing.jpg"), eq(byte[].class)))
        .willThrow(new RuntimeException("404 Not Found"));

    assertThatThrownBy(() -> service.toDataUrl("images/x/missing.jpg"))
        .isInstanceOf(BusinessException.class)
        .extracting("code", "message")
        .containsExactly(40000, "images: 图片拉取失败 images/x/missing.jpg");
  }

  @Test
  @DisplayName("W-2：拉取为空同样映射 40000 并携带 url")
  void fetchEmptyBytesThrows40000() {
    given(
            restTemplate.getForObject(
                eq("http://127.0.0.1:8080/files/images/x/empty.jpg"), eq(byte[].class)))
        .willReturn(null);

    assertThatThrownBy(() -> service.toDataUrl("images/x/empty.jpg"))
        .isInstanceOf(BusinessException.class)
        .extracting("code", "message")
        .containsExactly(40000, "images: 图片拉取为空 images/x/empty.jpg");
  }
}
