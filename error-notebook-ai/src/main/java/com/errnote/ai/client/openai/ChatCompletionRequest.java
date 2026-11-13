package com.errnote.ai.client.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OpenAI 兼容 chat/completions 请求（原生同构结构，不越出 client 包——测试策略 §2.2）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

  private String model;

  private List<Message> messages;

  private Double temperature;

  @JsonProperty("top_p")
  private Double topP;

  /**
   * 思考模式开关（智谱 GLM 扩展字段，W-7 2026-11-13 chandler 确认默认 disabled）：错题结构化提取实测 禁用思考 5.6s
   * 出完整草稿（含解出的答案）、默认深思考 168s 超一切超时阶梯；空值时省略该字段（模型走端点默认）。
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Thinking thinking;

  /** 思考控制：type = enabled / disabled。 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Thinking {

    private String type;
  }

  /** 消息：role=system/user；content 为多模态分块数组（text / image_url）。 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Message {

    private String role;

    private List<ContentPart> content;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ContentPart {

    /** text / image_url。 */
    private String type;

    private String text;

    @JsonProperty("image_url")
    private ImageUrl imageUrl;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImageUrl {

    /** http(s) URL 或 data URL（base64）。 */
    private String url;
  }
}
