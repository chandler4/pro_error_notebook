package com.errnote.service.impl;

import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.dto.ImageRef;
import com.errnote.dto.QuestionSaveRequest;
import com.errnote.entity.WrongQuestion;
import com.errnote.integration.storage.FileStorageService;
import com.errnote.mapper.WrongQuestionMapper;
import com.errnote.service.KnowledgeService;
import com.errnote.service.QuestionService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 错题域服务实现（FR-A6/B3、BR-03/04）。
 *
 * <p>confirm/update 共用全字段覆盖；tags/知识点去重保序（TC-T-01）；删除联动删图（TC-C-05，删图失败不影响删除 ——FileStorageService
 * 内部吞异常）并知识点减计。
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

  private final WrongQuestionMapper wrongQuestionMapper;
  private final KnowledgeService knowledgeService;
  private final FileStorageService fileStorageService;
  private final Clock clock;

  @Override
  public WrongQuestion get(String id) {
    WrongQuestion q = wrongQuestionMapper.selectById(id);
    if (q == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "错题不存在: " + id);
    }
    return q;
  }

  @Override
  @Transactional
  public WrongQuestion confirm(String id, QuestionSaveRequest request) {
    WrongQuestion q = loadAndApply(id, request);
    q.setStatus(WrongQuestion.STATUS_CONFIRMED); // TC-C-02：转 CONFIRMED，id 不变
    persist(q, request);
    return q;
  }

  @Override
  @Transactional
  public WrongQuestion update(String id, QuestionSaveRequest request) {
    WrongQuestion q = loadAndApply(id, request); // 3.6：编辑不改状态
    persist(q, request);
    return q;
  }

  @Override
  @Transactional
  public void delete(String id) {
    WrongQuestion q = get(id);
    wrongQuestionMapper.deleteById(id);
    // TC-C-05 / BR-04：联动删除图片文件（失败仅日志，不影响删除事务）
    for (ImageRef image : q.images()) {
      fileStorageService.delete(image.getUrl());
    }
    knowledgeService.decrement(q.points());
  }

  private WrongQuestion loadAndApply(String id, QuestionSaveRequest request) {
    WrongQuestion q = get(id);
    q.setSubject(request.getSubject());
    q.setGradeLevel(request.getGradeLevel());
    q.setQuestionType(request.getQuestionType());
    q.setQuestionStem(request.getQuestionStem());
    q.setImages(
        request.getQuestionImages() == null
            ? Collections.<ImageRef>emptyList()
            : request.getQuestionImages());
    q.setSourceName(request.getSourceName());
    q.setQuestionNumber(request.getQuestionNumber());
    q.setAnswers(
        request.getStandardAnswer() == null
            ? Collections.<String>emptyList()
            : request.getStandardAnswer());
    q.setPoints(dedupe(request.getKnowledgePoints()));
    q.setTagList(dedupe(request.getTags()));
    q.setKnowledgeAnalysis(request.getKnowledgeAnalysis());
    if (request.getWrongDate() != null) {
      q.setWrongDate(request.getWrongDate());
    }
    return q;
  }

  private void persist(WrongQuestion q, QuestionSaveRequest request) {
    q.setUpdatedAt(LocalDateTime.now(clock));
    wrongQuestionMapper.updateById(q);
    knowledgeService.accumulate(dedupe(request.getKnowledgePoints()));
  }

  private static List<String> dedupe(List<String> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }
    return new java.util.ArrayList<>(new LinkedHashSet<>(values));
  }
}
