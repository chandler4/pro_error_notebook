package com.errnote.service.impl;

import com.errnote.common.ApiResponse;
import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.common.IdGenerator;
import com.errnote.dto.ImageRef;
import com.errnote.entity.WrongQuestion;
import com.errnote.integration.ai.AiServiceClient;
import com.errnote.integration.ai.dto.AiParseRequest;
import com.errnote.integration.ai.dto.AiParseResponse;
import com.errnote.integration.ai.dto.WrongQuestionDraftDto;
import com.errnote.log.service.BusinessLogService;
import com.errnote.mapper.WrongQuestionMapper;
import com.errnote.service.ParseService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 解析编排实现（FR-A3/A5）。
 *
 * <p>防腐层职责：网络/反序列化异常统一映射 42002 且不泄漏细节（TC-P-03）；AI 业务错误 42001 原样透传、 未知码归并 42001（契约 3.2 仅暴露
 * 42001/42002/40000）；data/draft 缺失视为 AI 输出无效。成功路径：草稿 PENDING_CONFIRM 入库（TC-P-05）+ recordAi raw
 * 全文留痕（TC-G-03/BR-09）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseServiceImpl implements ParseService {

  private final AiServiceClient aiServiceClient;
  private final WrongQuestionMapper wrongQuestionMapper;
  private final BusinessLogService businessLogService;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  public WrongQuestion parse(List<ImageRef> images) {
    long start = clock.millis();
    try {
      AiParseResponse ai = callAi(images);
      WrongQuestion entity = toPendingDraft(ai.getDraft(), images);
      wrongQuestionMapper.insert(entity);
      businessLogService.recordAi(
          ai.getModel() == null ? "" : ai.getModel(),
          ai.getDurationMs() == null ? 0L : ai.getDurationMs(),
          "parse images=" + images.size(),
          ai.getRawModelResponse() == null ? "" : ai.getRawModelResponse());
      return entity;
    } catch (BusinessException e) {
      // W-1（2026-11-12 chandler 确认）：AI 调用失败同样留痕 AI_CALL（success=false+错误码），
      // 运行日志页「AI 调用+失败」可追溯；参数校验 40000 在 Controller 层拦截，不进入此路径
      businessLogService.recordAiFailure(
          "", clock.millis() - start, "parse images=" + images.size(), e.getCode(), e.getMessage());
      throw e;
    }
  }

  /** 调内部契约（单次；重试收敛于 AI 服务 OpenAiVisionAdapter）。 */
  private AiParseResponse callAi(List<ImageRef> images) {
    List<AiParseRequest.ImageRef> refs = new ArrayList<>();
    for (ImageRef image : images) {
      refs.add(new AiParseRequest.ImageRef(image.getUrl(), image.getRole()));
    }
    ApiResponse<AiParseResponse> response;
    try {
      response = aiServiceClient.parse(AiParseRequest.builder().images(refs).build());
    } catch (Exception e) {
      // TC-P-03：任何传输/解码异常 → 42002，message 不携带底层细节
      log.warn("ai internal call failed: {}", e.getMessage());
      throw new BusinessException(ErrorCode.AI_UNAVAILABLE);
    }
    if (response == null) {
      throw new BusinessException(ErrorCode.AI_PARSE_FAILED);
    }
    if (response.getCode() != 0) {
      // AI 业务错误透传：42002/40000 保留语义（W-2：图片 url 失效等参数错误带明细直达前端），其余（含未知码）归并 42001
      if (response.getCode() == ErrorCode.AI_UNAVAILABLE.getCode()) {
        throw new BusinessException(ErrorCode.AI_UNAVAILABLE, response.getMessage());
      }
      if (response.getCode() == ErrorCode.PARAM_INVALID.getCode()) {
        throw new BusinessException(ErrorCode.PARAM_INVALID, response.getMessage());
      }
      throw new BusinessException(ErrorCode.AI_PARSE_FAILED, response.getMessage());
    }
    AiParseResponse data = response.getData();
    if (data == null || data.getDraft() == null) {
      // 防腐：成功码但 data/draft 缺失 → AI 输出无效
      throw new BusinessException(ErrorCode.AI_PARSE_FAILED, response.getMessage());
    }
    return data;
  }

  private WrongQuestion toPendingDraft(WrongQuestionDraftDto draft, List<ImageRef> images) {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId(idGenerator.newId());
    q.setSubject(draft.getSubject());
    q.setGradeLevel(draft.getGradeLevel());
    q.setQuestionType(draft.getQuestionType());
    q.setQuestionStem(draft.getQuestionStem());
    q.setImages(images); // 请求图原样入库（题目图在前，TC-P-04 由 AI 服务保证）
    q.setSourceName(draft.getSourceName());
    q.setQuestionNumber(draft.getQuestionNumber());
    q.setAnswers(draft.getStandardAnswer());
    q.setPoints(draft.getKnowledgePoints());
    q.setTagList(draft.getTags()); // BR-11：AI 恒为空数组，透传不改写
    q.setKnowledgeAnalysis(draft.getKnowledgeAnalysis());
    q.setWrongDate(LocalDate.now(clock)); // 解析日即错题日期默认值，确认时可改
    q.setStatus(WrongQuestion.STATUS_PENDING);
    LocalDateTime now = LocalDateTime.now(clock);
    q.setCreatedAt(now);
    q.setUpdatedAt(now);
    return q;
  }
}
