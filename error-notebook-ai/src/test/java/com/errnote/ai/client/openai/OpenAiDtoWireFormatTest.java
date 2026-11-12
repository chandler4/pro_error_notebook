package com.errnote.ai.client.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A-3 DTO 线格式（wire format）验证。
 *
 * <p>OpenAI 兼容端点按 JSON 键名严格匹配：top_p / image_url 若序列化为 camelCase，请求会被静默忽略或拒绝。
 */
class OpenAiDtoWireFormatTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("请求序列化使用 OpenAI 蛇形键名：top_p / image_url")
  void requestSerializesWithSnakeCaseKeys() throws Exception {
    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model("glm-5.3-flash")
            .temperature(1.0)
            .topP(0.95)
            .messages(
                Collections.singletonList(
                    ChatCompletionRequest.Message.builder()
                        .role("user")
                        .content(
                            Arrays.asList(
                                ChatCompletionRequest.ContentPart.builder()
                                    .type("image_url")
                                    .imageUrl(
                                        ChatCompletionRequest.ImageUrl.builder()
                                            .url("data:image/jpeg;base64,QUJD")
                                            .build())
                                    .build()))
                        .build()))
            .build();

    JsonNode json = mapper.readTree(mapper.writeValueAsString(request));

    assertThat(json.path("model").asText()).isEqualTo("glm-5.3-flash");
    assertThat(json.path("top_p").asDouble()).isEqualTo(0.95);
    assertThat(json.has("topP")).isFalse();
    assertThat(json.path("messages").get(0).path("role").asText()).isEqualTo("user");
    JsonNode part = json.path("messages").get(0).path("content").get(0);
    assertThat(part.path("type").asText()).isEqualTo("image_url");
    assertThat(part.path("image_url").path("url").asText())
        .isEqualTo("data:image/jpeg;base64,QUJD");
    assertThat(part.has("imageUrl")).isFalse();
  }

  @Test
  @DisplayName("响应反序列化读取 choices[0].message.content")
  void responseDeserializesFirstChoiceContent() throws Exception {
    String body =
        "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"subject\\\":\\\"物理\\\"}\"}}]}";

    ChatCompletionResponse response = mapper.readValue(body, ChatCompletionResponse.class);

    assertThat(response.getChoices()).hasSize(1);
    assertThat(response.getChoices().get(0).getMessage().getRole()).isEqualTo("assistant");
    assertThat(response.getChoices().get(0).getMessage().getContent())
        .isEqualTo("{\"subject\":\"物理\"}");
  }
}
