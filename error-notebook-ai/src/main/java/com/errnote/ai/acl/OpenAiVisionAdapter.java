package com.errnote.ai.acl;

import com.errnote.ai.client.ChatRequestFactory;
import com.errnote.ai.client.ImageFetchService;
import com.errnote.ai.client.OpenAiModelClient;
import com.errnote.ai.client.openai.ChatCompletionRequest;
import com.errnote.ai.client.openai.ChatCompletionResponse;
import com.errnote.ai.common.BusinessException;
import com.errnote.ai.config.AiModelProperties;
import com.errnote.ai.dto.ParseResponse;
import com.errnote.ai.dto.WrongQuestionDraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 防腐层（2026-11-11 16:04 ACL 决策）：OpenAI 原生响应 ↔ 草稿 DTO 转换、严格 JSON 校验、失败重试 1 次、 异常映射（42001 输出非法已重试 /
 * 42002 不可达·超时，不泄漏底层细节）；raw 原文透出（供 BE 落 business_log）；draft.tags 恒为空数组（TC-T-04）。
 */
@Slf4j
@Service
public class OpenAiVisionAdapter {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final OpenAiModelClient modelClient;
  private final ChatRequestFactory factory;
  private final ImageFetchService imageFetchService;
  private final AiModelProperties properties;

  public OpenAiVisionAdapter(
      OpenAiModelClient modelClient,
      ChatRequestFactory factory,
      ImageFetchService imageFetchService,
      AiModelProperties properties) {
    this.modelClient = modelClient;
    this.factory = factory;
    this.imageFetchService = imageFetchService;
    this.properties = properties;
  }

  public ParseResponse parse(List<com.errnote.ai.dto.ParseRequest.ImageRef> images) {
    long start = System.nanoTime();
    ChatCompletionRequest request = factory.create(images, imageFetchService::toDataUrl);

    String raw = callModel(request);
    WrongQuestionDraft draft = tryParse(raw);
    if (draft == null) {
      // 失败重试 1 次（TC-P-02：非法输出重试后仍失败 → 42001，调用计数 = 2）
      String retried = callModel(request);
      draft = tryParse(retried);
      if (draft == null) {
        throw new BusinessException(42001, "AI 解析失败：模型输出无效（已重试 1 次）");
      }
      return build(draft, retried, start);
    }
    return build(draft, raw, start);
  }

  /** 模型调用：网络/超时异常统一映射 42002（TC-P-03：不重试、不泄漏底层异常细节；日志脱敏只记异常类别）。 */
  private String callModel(ChatCompletionRequest request) {
    try {
      ChatCompletionResponse resp = modelClient.chat(request);
      if (resp == null
          || resp.getChoices() == null
          || resp.getChoices().isEmpty()
          || resp.getChoices().get(0).getMessage() == null) {
        return "";
      }
      String content = resp.getChoices().get(0).getMessage().getContent();
      return content == null ? "" : content;
    } catch (RuntimeException e) {
      log.warn("模型调用失败，类别: {}（不打印堆栈细节与密钥）", e.getClass().getSimpleName());
      throw new BusinessException(42002, "AI 服务不可达（网络错误或超时）");
    }
  }

  /** 严格 JSON 校验：剥离 markdown 围栏与杂文字后解析；失败返回 null（参与重试）。 */
  private WrongQuestionDraft tryParse(String raw) {
    String json = extractJsonBlock(raw);
    if (json == null) {
      return null;
    }
    try {
      WrongQuestionDraft draft = MAPPER.readValue(json, WrongQuestionDraft.class);
      // TC-T-04 / BR-11：标签纯手工标注，AI 草稿 tags 恒为空数组
      draft.setTags(Collections.emptyList());
      return draft;
    } catch (Exception e) {
      return null;
    }
  }

  private String extractJsonBlock(String raw) {
    if (raw == null) {
      return null;
    }
    int left = raw.indexOf('{');
    int right = raw.lastIndexOf('}');
    if (left < 0 || right <= left) {
      return null;
    }
    return raw.substring(left, right + 1);
  }

  private ParseResponse build(WrongQuestionDraft draft, String raw, long startNanos) {
    return ParseResponse.builder()
        .draft(draft)
        .rawModelResponse(raw)
        .model(properties.getModel())
        .durationMs((System.nanoTime() - startNanos) / 1_000_000)
        .build();
  }
}
