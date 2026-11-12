package com.errnote.ai.client;

import com.errnote.ai.client.openai.ChatCompletionRequest;
import com.errnote.ai.client.openai.ChatCompletionRequest.ContentPart;
import com.errnote.ai.client.openai.ChatCompletionRequest.ImageUrl;
import com.errnote.ai.client.openai.ChatCompletionRequest.Message;
import com.errnote.ai.dto.ParseRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 请求组装纯函数（可测试性设计 §2.3：无 IO，表驱动直测）。
 *
 * <p>多模态消息：system（仅输出严格 JSON 的 Prompt，A-4 注入）+ user（文本指令 + 图片 data URL 数组）。
 * 图片按角色排序：题目图（QUESTION）在前、解答图（ANSWER_SHEET）在后——答案可能在答题纸照片中， 题面优先供模型识别（TC-P-04、BR-06）。
 */
public class ChatRequestFactory {

  private static final String QUESTION = "QUESTION";

  private static final String USER_INSTRUCTION = "请解析以上图片中的错题内容，按系统指令仅输出严格 JSON。";

  private final String model;
  private final Double temperature;
  private final Double topP;
  private final String systemPrompt;

  public ChatRequestFactory(String model, Double temperature, Double topP, String systemPrompt) {
    this.model = model;
    this.temperature = temperature;
    this.topP = topP;
    this.systemPrompt = systemPrompt;
  }

  /** @param urlToDataUrl 图片相对路径到 data URL 的转换（ImageFetchService 提供，测试可替换） */
  public ChatCompletionRequest create(
      List<ParseRequest.ImageRef> images, Function<String, String> urlToDataUrl) {
    List<ParseRequest.ImageRef> ordered = new ArrayList<>(images);
    ordered.sort(Comparator.comparingInt(this::roleOrder));

    List<ContentPart> userParts = new ArrayList<>();
    userParts.add(ContentPart.builder().type("text").text(USER_INSTRUCTION).build());
    for (ParseRequest.ImageRef img : ordered) {
      userParts.add(
          ContentPart.builder()
              .type("image_url")
              .imageUrl(ImageUrl.builder().url(urlToDataUrl.apply(img.getUrl())).build())
              .build());
    }

    return ChatCompletionRequest.builder()
        .model(model)
        .temperature(temperature)
        .topP(topP)
        .messages(
            java.util.Arrays.asList(
                Message.builder()
                    .role("system")
                    .content(
                        java.util.Collections.singletonList(
                            ContentPart.builder().type("text").text(systemPrompt).build()))
                    .build(),
                Message.builder().role("user").content(userParts).build()))
        .build();
  }

  private int roleOrder(ParseRequest.ImageRef img) {
    return QUESTION.equals(img.getRole()) ? 0 : 1;
  }
}
