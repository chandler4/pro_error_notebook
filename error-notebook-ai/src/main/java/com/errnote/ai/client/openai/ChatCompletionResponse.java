package com.errnote.ai.client.openai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OpenAI 兼容 chat/completions 响应（防腐层只取 choices[0].message.content 作为原文）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse {

  private List<Choice> choices;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Choice {

    private ResponseMessage message;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ResponseMessage {

    private String role;

    private String content;
  }
}
