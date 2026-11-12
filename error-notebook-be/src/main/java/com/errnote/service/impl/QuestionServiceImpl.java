package com.errnote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.common.IdGenerator;
import com.errnote.dto.ImageRef;
import com.errnote.dto.PageResult;
import com.errnote.dto.QuestionDetail;
import com.errnote.dto.QuestionQuery;
import com.errnote.dto.QuestionSaveRequest;
import com.errnote.entity.WrongQuestion;
import com.errnote.integration.storage.FileStorageService;
import com.errnote.mapper.WrongQuestionMapper;
import com.errnote.service.KnowledgeService;
import com.errnote.service.QuestionService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * 内部吞异常）并知识点减计。列表（3.5）与草稿（3.8）同域收敛于此。
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

  static final int MAX_PAGE_SIZE = 50;

  private final WrongQuestionMapper wrongQuestionMapper;
  private final KnowledgeService knowledgeService;
  private final FileStorageService fileStorageService;
  private final IdGenerator idGenerator;
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

  @Override
  public PageResult<QuestionDetail> list(QuestionQuery query) {
    LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<>();
    w.eq(WrongQuestion::getStatus, WrongQuestion.STATUS_CONFIRMED); // TC-L-01：草稿不出现
    if (notBlank(query.getSubject())) {
      w.eq(WrongQuestion::getSubject, query.getSubject());
    }
    if (notBlank(query.getKeyword())) {
      w.like(WrongQuestion::getQuestionStem, query.getKeyword()); // TC-L-02
    }
    if (notBlank(query.getKnowledgePoint())) {
      w.like(
          WrongQuestion::getKnowledgePoints, query.getKnowledgePoint()); // TC-L-03：JSON 列 LIKE 命中任一
    }
    if (notBlank(query.getTag())) {
      w.like(WrongQuestion::getTags, query.getTag()); // TC-T-02：命中任一标签
    }
    if (notBlank(query.getQuestionType())) {
      w.eq(WrongQuestion::getQuestionType, query.getQuestionType());
    }
    if (query.getDateFrom() != null) {
      w.ge(WrongQuestion::getWrongDate, query.getDateFrom());
    }
    if (query.getDateTo() != null) {
      w.le(WrongQuestion::getWrongDate, query.getDateTo());
    }
    w.orderByDesc(WrongQuestion::getWrongDate).orderByDesc(WrongQuestion::getCreatedAt);
    Page<WrongQuestion> page =
        wrongQuestionMapper.selectPage(normalizePage(query.getPage(), query.getSize()), w);
    List<QuestionDetail> details = new ArrayList<>();
    for (WrongQuestion q : page.getRecords()) {
      details.add(QuestionDetail.from(q));
    }
    return PageResult.of(details, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
  }

  // TC-P-07：空草稿零 AI 调用——本类无任何 AI 依赖，结构上不可能触发模型调用
  @Override
  public WrongQuestion createEmptyDraft() {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId(idGenerator.newId());
    q.setImages(Collections.<ImageRef>emptyList());
    q.setAnswers(Collections.<String>emptyList());
    q.setPoints(Collections.<String>emptyList());
    q.setTagList(Collections.<String>emptyList());
    q.setStatus(WrongQuestion.STATUS_PENDING);
    LocalDateTime now = LocalDateTime.now(clock);
    q.setCreatedAt(now);
    q.setUpdatedAt(now);
    wrongQuestionMapper.insert(q);
    return q;
  }

  @Override
  public PageResult<QuestionDetail> listDrafts(int page, int size) {
    LambdaQueryWrapper<WrongQuestion> w = new LambdaQueryWrapper<>();
    w.eq(WrongQuestion::getStatus, WrongQuestion.STATUS_PENDING);
    w.orderByDesc(WrongQuestion::getCreatedAt);
    Page<WrongQuestion> result = wrongQuestionMapper.selectPage(normalizePage(page, size), w);
    List<QuestionDetail> details = new ArrayList<>();
    for (WrongQuestion q : result.getRecords()) {
      details.add(QuestionDetail.from(q));
    }
    return PageResult.of(
        details, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
  }

  // 3.8：草稿删除不删图片（图片随复用或人工清理）
  @Override
  @Transactional
  public void deleteDraft(String id) {
    WrongQuestion q = wrongQuestionMapper.selectById(id);
    if (q == null || !WrongQuestion.STATUS_PENDING.equals(q.getStatus())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "草稿不存在: " + id);
    }
    wrongQuestionMapper.deleteById(id);
  }

  /** 分页边界（TC-L-04）：page 收敛 ≥1；size 收敛 [1,50]；缺省 page=1/size=10。 */
  private static Page<WrongQuestion> normalizePage(Integer page, Integer size) {
    int p = page == null ? 1 : Math.max(1, page);
    int s = size == null ? 10 : Math.max(1, Math.min(MAX_PAGE_SIZE, size));
    return new Page<>(p, s);
  }

  private static boolean notBlank(String value) {
    return value != null && !value.trim().isEmpty();
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
    return new ArrayList<>(new LinkedHashSet<>(values));
  }
}
