package com.errnote.ai.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.errnote.ai.client.openai.ChatCompletionRequest;
import com.errnote.ai.client.openai.ChatCompletionRequest.ContentPart;
import com.errnote.ai.dto.ParseRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A-2 模型客户端请求组装。
 *
 * <p>TC-P-04 规约2：多图排序——Prompt 中题目图在前、解答图在后。
 */
class ChatRequestFactoryTest {

  private final ChatRequestFactory factory =
      new ChatRequestFactory("glm-5.3-flash", 1.0, 0.95, "SYSTEM_PROMPT_PLACEHOLDER", "disabled");

  @Test
  @DisplayName("TC-P-04: 图片按角色排序——题目图在前、解答图在后")
  void questionImagesComeBeforeAnswerSheets() {
    // 乱序输入：解答图在前
    List<ParseRequest.ImageRef> images =
        Arrays.asList(
            new ParseRequest.ImageRef("images/ans.jpg", "ANSWER_SHEET"),
            new ParseRequest.ImageRef("images/q1.jpg", "QUESTION"),
            new ParseRequest.ImageRef("images/ans2.jpg", "ANSWER_SHEET"));

    ChatCompletionRequest req =
        factory.create(
            images,
            url -> {
              if (url.endsWith("q1.jpg")) {
                return "data:q1";
              }
              if (url.endsWith("ans.jpg")) {
                return "data:ans";
              }
              return "data:ans2";
            });

    List<ContentPart> imageParts = imagePartsOf(req);
    assertThat(imageParts)
        .extracting(p -> p.getImageUrl().getUrl())
        .containsExactly("data:q1", "data:ans", "data:ans2");
  }

  @Test
  @DisplayName("请求体结构：model/采样参数/system+user 两条消息")
  void requestBodyStructure() {
    ChatCompletionRequest req =
        factory.create(
            Arrays.asList(new ParseRequest.ImageRef("images/q.jpg", "QUESTION")), u -> "data:q");

    assertThat(req.getModel()).isEqualTo("glm-5.3-flash");
    assertThat(req.getTemperature()).isEqualTo(1.0);
    assertThat(req.getTopP()).isEqualTo(0.95);
    // W-7：思考模式开关随请求下发（实测 disabled 5.6s / 默认深思考 168s）
    assertThat(req.getThinking()).isNotNull();
    assertThat(req.getThinking().getType()).isEqualTo("disabled");
    assertThat(req.getMessages()).hasSize(2);
    assertThat(req.getMessages().get(0).getRole()).isEqualTo("system");
    assertThat(req.getMessages().get(0).getContent().get(0).getText())
        .isEqualTo("SYSTEM_PROMPT_PLACEHOLDER");
    assertThat(req.getMessages().get(1).getRole()).isEqualTo("user");
    // user 消息：文本指令在前，图片在后
    List<ContentPart> userParts = req.getMessages().get(1).getContent();
    assertThat(userParts.get(0).getType()).isEqualTo("text");
    assertThat(userParts.get(1).getType()).isEqualTo("image_url");
    assertThat(userParts.get(1).getImageUrl().getUrl()).isEqualTo("data:q");
  }

  @Test
  @DisplayName("W-7: thinkingType 为空时省略思考字段（模型走端点默认），非空时原样下发")
  void thinkingFieldOmittedWhenEmpty() {
    ChatRequestFactory omitFactory =
        new ChatRequestFactory("glm-5.3-flash", 1.0, 0.95, "SYSTEM_PROMPT_PLACEHOLDER", "");
    ChatCompletionRequest req =
        omitFactory.create(
            Arrays.asList(new ParseRequest.ImageRef("images/q.jpg", "QUESTION")), u -> "data:q");
    assertThat(req.getThinking()).isNull();

    ChatRequestFactory enabledFactory =
        new ChatRequestFactory("glm-5.3-flash", 1.0, 0.95, "SYSTEM_PROMPT_PLACEHOLDER", "enabled");
    ChatCompletionRequest req2 =
        enabledFactory.create(
            Arrays.asList(new ParseRequest.ImageRef("images/q.jpg", "QUESTION")), u -> "data:q");
    assertThat(req2.getThinking().getType()).isEqualTo("enabled");
  }

  private List<ContentPart> imagePartsOf(ChatCompletionRequest req) {
    List<ContentPart> result = new ArrayList<>();
    for (ContentPart p : req.getMessages().get(1).getContent()) {
      if ("image_url".equals(p.getType())) {
        result.add(p);
      }
    }
    return result;
  }
}
