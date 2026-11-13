package com.errnote.ai.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.errnote.ai.client.ChatRequestFactory;
import com.errnote.ai.client.ImageFetchService;
import com.errnote.ai.client.openai.ChatCompletionResponse;
import com.errnote.ai.client.openai.ChatCompletionResponse.Choice;
import com.errnote.ai.client.openai.ChatCompletionResponse.ResponseMessage;
import com.errnote.ai.common.BusinessException;
import com.errnote.ai.dto.ParseResponse;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A-3 防腐层 OpenAiVisionAdapter（严格 TDD）。
 *
 * <p>TC-P-01 解析成功映射 / TC-P-02 非法输出重试 1 次后 42001 / TC-P-03 网络异常映射 42002 不泄漏细节 / TC-T-04 草稿 tags 恒空。
 */
class OpenAiVisionAdapterTest {

  private static final String VALID_DRAFT_JSON =
      "{\"subject\":\"physics\",\"gradeLevel\":\"grade9\",\"questionType\":\"fillBlank\","
          + "\"questionStem\":\"题干\",\"sourceName\":\"来源\",\"questionNumber\":\"14\","
          + "\"standardAnswer\":[\"12\",\"1:5\"],\"knowledgePoints\":[\"电压表读数\"],"
          + "\"knowledgeAnalysis\":\"解析\",\"tags\":[\"不应出现的标签\"]}";

  private com.errnote.ai.client.OpenAiModelClient modelClient;
  private ImageFetchService imageFetchService;
  private OpenAiVisionAdapter adapter;

  @BeforeEach
  void setUp() {
    modelClient = mock(com.errnote.ai.client.OpenAiModelClient.class);
    imageFetchService = mock(ImageFetchService.class);
    when(imageFetchService.toDataUrl(any())).thenReturn("data:image/jpeg;base64,AAAA");
    com.errnote.ai.config.AiModelProperties props = new com.errnote.ai.config.AiModelProperties();
    props.setModel("glm-5.3-flash");
    props.setTemperature(1.0);
    props.setTopP(0.95);
    ChatRequestFactory factory =
        new ChatRequestFactory(
            props.getModel(), props.getTemperature(), props.getTopP(), "SYSTEM_PROMPT");
    adapter = new OpenAiVisionAdapter(modelClient, factory, imageFetchService, props);
  }

  @Test
  @DisplayName("TC-P-01: 解析成功——草稿字段一一映射、raw 原文透出、含模型名与耗时")
  void parseSuccessMapsDraftAndKeepsRaw() {
    when(modelClient.chat(any())).thenReturn(responseWithContent(VALID_DRAFT_JSON));

    ParseResponse resp = adapter.parse(images());

    assertThat(resp.getModel()).isEqualTo("glm-5.3-flash");
    assertThat(resp.getDurationMs()).isGreaterThanOrEqualTo(0);
    assertThat(resp.getRawModelResponse()).isEqualTo(VALID_DRAFT_JSON);
    assertThat(resp.getDraft().getSubject()).isEqualTo("physics");
    assertThat(resp.getDraft().getGradeLevel()).isEqualTo("grade9");
    assertThat(resp.getDraft().getQuestionStem()).isEqualTo("题干");
    assertThat(resp.getDraft().getStandardAnswer()).containsExactly("12", "1:5");
    assertThat(resp.getDraft().getKnowledgePoints()).containsExactly("电压表读数");
    assertThat(resp.getDraft().getKnowledgeAnalysis()).isEqualTo("解析");
  }

  @Test
  @DisplayName("TC-T-04: 草稿 tags 恒为空数组（模型返回非空也强制清空——BR-11 手工标注）")
  void draftTagsAlwaysEmpty() {
    when(modelClient.chat(any())).thenReturn(responseWithContent(VALID_DRAFT_JSON));

    ParseResponse resp = adapter.parse(images());

    assertThat(resp.getDraft().getTags()).isEmpty();
  }

  @Test
  @DisplayName("模型输出带 markdown 代码围栏时剥离后解析成功")
  void stripsCodeFence() {
    when(modelClient.chat(any()))
        .thenReturn(responseWithContent("```json\n" + VALID_DRAFT_JSON + "\n```"));

    ParseResponse resp = adapter.parse(images());

    assertThat(resp.getDraft().getSubject()).isEqualTo("physics");
  }

  @Test
  @DisplayName("TC-P-02: 非法输出重试 1 次仍失败 → 42001，调用计数 = 2")
  void invalidOutputRetriesOnceThenThrows42001() {
    when(modelClient.chat(any())).thenReturn(responseWithContent("这不是 JSON"));

    assertThatThrownBy(() -> adapter.parse(images()))
        .isInstanceOf(BusinessException.class)
        .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(42001));

    verify(modelClient, times(2)).chat(any());
  }

  @Test
  @DisplayName("第一次非法、重试后成功 → 返回草稿（调用 2 次）")
  void retrySucceedsOnSecondAttempt() {
    when(modelClient.chat(any()))
        .thenReturn(responseWithContent("bad"))
        .thenReturn(responseWithContent(VALID_DRAFT_JSON));

    ParseResponse resp = adapter.parse(images());

    assertThat(resp.getDraft().getSubject()).isEqualTo("physics");
    verify(modelClient, times(2)).chat(any());
  }

  @Test
  @DisplayName("TC-P-03 + W-6: 网络/超时异常重试 1 次仍失败 → 42002，message 统一文案不泄漏底层细节，调用计数 = 2")
  void networkErrorRetriesOnceThenMapsTo42002() {
    RuntimeException timeout =
        new RuntimeException(new SocketTimeoutException("Read timed out after 90000ms"));
    when(modelClient.chat(any())).thenThrow(timeout);

    assertThatThrownBy(() -> adapter.parse(images()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            e -> {
              assertThat(((BusinessException) e).getCode()).isEqualTo(42002);
              assertThat(e.getMessage()).contains("AI 服务不可达");
              assertThat(e.getMessage()).doesNotContain("SocketTimeoutException");
              assertThat(e.getMessage()).doesNotContain("timed out");
            });

    verify(modelClient, times(2)).chat(any());
  }

  @Test
  @DisplayName("W-6: 首调断连/超时、重试成功 → 返回草稿（外部接口间歇性故障自愈，调用计数 = 2）")
  void networkErrorRetriedOnceSucceeds() {
    RuntimeException drop = new RuntimeException(new SocketTimeoutException("Connection reset"));
    when(modelClient.chat(any())).thenThrow(drop).thenReturn(responseWithContent(VALID_DRAFT_JSON));

    ParseResponse resp = adapter.parse(images());

    assertThat(resp.getDraft().getSubject()).isEqualTo("physics");
    assertThat(resp.getRawModelResponse()).isEqualTo(VALID_DRAFT_JSON);
    verify(modelClient, times(2)).chat(any());
  }

  private List<com.errnote.ai.dto.ParseRequest.ImageRef> images() {
    return Collections.singletonList(
        new com.errnote.ai.dto.ParseRequest.ImageRef("images/202611/q.jpg", "QUESTION"));
  }

  private ChatCompletionResponse responseWithContent(String content) {
    ResponseMessage msg = new ResponseMessage("assistant", content);
    return new ChatCompletionResponse(Arrays.asList(new Choice(msg)));
  }
}
