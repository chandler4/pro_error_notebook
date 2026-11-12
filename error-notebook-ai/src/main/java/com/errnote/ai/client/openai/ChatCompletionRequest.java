package com.errnote.ai.client.openai;

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
