package com.errnote.backup;

import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.common.IdGenerator;
import com.errnote.common.JsonUtil;
import com.errnote.dto.ImageRef;
import com.errnote.dto.QuestionDetail;
import com.errnote.dto.QuestionQuery;
import com.errnote.entity.WrongQuestion;
import com.errnote.mapper.WrongQuestionMapper;
import com.errnote.service.KnowledgeService;
import com.errnote.service.QuestionService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 备份迁移（FR-B5 / BR-08）：全量 CONFIRMED 导出规约 JSON 数组；导入按 errorQuestionId 幂等（skip / overwrite），
 * 行级错误不中断整批（api-contract 3.10）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

  static final int MAX_IMPORT_BYTES = 20 * 1024 * 1024; // TC-S-04

  private final QuestionService questionService;
  private final WrongQuestionMapper wrongQuestionMapper;
  private final KnowledgeService knowledgeService;
  private final IdGenerator idGenerator;
  private final Clock clock;

  /** 全量导出（仅 CONFIRMED）：含 questionImage 兼容字段与 questionImages 扩展。 */
  public byte[] exportAll() {
    List<QuestionDetail> all = questionService.listAll(new QuestionQuery());
    List<BackupItem> items = new ArrayList<>();
    for (QuestionDetail q : all) {
      items.add(toItem(q));
    }
    return JsonUtil.toJson(items).getBytes(StandardCharsets.UTF_8);
  }

  /** 导入（幂等）：skip=已存在跳过；overwrite=覆盖更新。行级校验失败记 errors 不中断；文件级错误（非 JSON/超限） 抛 43001。 */
  @Transactional
  public ImportResult importData(byte[] content, String mode) {
    if (content == null || content.length == 0) {
      throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR, "导入文件为空");
    }
    if (content.length > MAX_IMPORT_BYTES) { // TC-S-04：>20MB 拒绝
      throw new BusinessException(
          ErrorCode.IMPORT_FORMAT_ERROR, "导入文件超过 20MB 限制: " + content.length + " bytes");
    }
    boolean overwrite = "overwrite".equals(mode);
    List<BackupItem> items = parse(content);
    ImportResult result = new ImportResult();
    result.setTotal(items.size());
    for (int i = 0; i < items.size(); i++) {
      String reason = validate(items.get(i));
      if (reason != null) {
        result.getErrors().add(new ImportResult.RowError(i + 1, reason)); // 43002 行级明细
        continue;
      }
      apply(items.get(i), overwrite, result);
    }
    return result;
  }

  private static List<BackupItem> parse(byte[] content) {
    try {
      String json = new String(content, StandardCharsets.UTF_8);
      List<BackupItem> items =
          JsonUtil.convert(
              json, new com.fasterxml.jackson.core.type.TypeReference<List<BackupItem>>() {});
      if (items == null) {
        throw new IllegalArgumentException("null");
      }
      return items;
    } catch (Exception e) { // TC-E-06：非 JSON / 结构不符 → 43001
      throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR, "导入文件非 JSON 或结构不符");
    }
  }

  /** 行级校验（TC-E-06 → 43002 明细）：必填 id/科目/题干。 */
  private static String validate(BackupItem item) {
    if (item.getErrorQuestionId() == null || item.getErrorQuestionId().trim().isEmpty()) {
      return "缺少 errorQuestionId";
    }
    if (item.getSubject() == null || item.getSubject().trim().isEmpty()) {
      return "缺少 subject";
    }
    if (item.getQuestionStem() == null || item.getQuestionStem().trim().isEmpty()) {
      return "缺少 questionStem";
    }
    return null;
  }

  private void apply(BackupItem item, boolean overwrite, ImportResult result) {
    WrongQuestion existing = wrongQuestionMapper.selectById(item.getErrorQuestionId());
    if (existing == null) {
      wrongQuestionMapper.insert(toEntity(item));
      knowledgeService.accumulate(points(item));
      result.setInserted(result.getInserted() + 1);
      return;
    }
    if (!overwrite) { // skip：幂等跳过
      result.setSkipped(result.getSkipped() + 1);
      return;
    }
    knowledgeService.decrement(existing.points()); // 覆盖前旧知识点减计，重导不重复计数
    wrongQuestionMapper.updateById(toEntity(item));
    knowledgeService.accumulate(points(item));
    result.setOverwritten(result.getOverwritten() + 1);
  }

  private WrongQuestion toEntity(BackupItem item) {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId(item.getErrorQuestionId());
    q.setSubject(item.getSubject());
    q.setGradeLevel(item.getGradeLevel());
    q.setQuestionType(item.getQuestionType());
    q.setQuestionStem(item.getQuestionStem());
    List<ImageRef> images = item.getQuestionImages();
    if (images == null || images.isEmpty()) {
      // 兼容：仅 questionImage 单图（TC-D-02 反向）
      images =
          item.getQuestionImage() == null
              ? Collections.<ImageRef>emptyList()
              : Collections.singletonList(new ImageRef(item.getQuestionImage(), "QUESTION"));
    }
    q.setImages(images);
    q.setSourceName(item.getSourceName());
    q.setQuestionNumber(item.getQuestionNumber());
    q.setAnswers(
        item.getStandardAnswer() == null
            ? Collections.<String>emptyList()
            : item.getStandardAnswer());
    q.setPoints(points(item));
    q.setTagList(item.getTags() == null ? Collections.<String>emptyList() : item.getTags());
    q.setKnowledgeAnalysis(item.getKnowledgeAnalysis());
    q.setWrongDate(item.getWrongDate());
    q.setStatus(WrongQuestion.STATUS_CONFIRMED);
    LocalDateTime now = LocalDateTime.now(clock);
    q.setCreatedAt(item.getCreatedAt() == null ? now : item.getCreatedAt());
    q.setUpdatedAt(now);
    return q;
  }

  private static List<String> points(BackupItem item) {
    return item.getKnowledgePoints() == null
        ? Collections.<String>emptyList()
        : item.getKnowledgePoints();
  }

  /** 详情 → 备份条目：questionImage = 首个 QUESTION 角色图（TC-D-02）。 */
  private static BackupItem toItem(QuestionDetail q) {
    BackupItem item = new BackupItem();
    item.setErrorQuestionId(q.getErrorQuestionId());
    item.setSubject(q.getSubject());
    item.setGradeLevel(q.getGradeLevel());
    item.setQuestionType(q.getQuestionType());
    item.setQuestionStem(q.getQuestionStem());
    item.setQuestionImages(q.getQuestionImages());
    String firstQuestion = null;
    if (q.getQuestionImages() != null) {
      for (ImageRef image : q.getQuestionImages()) {
        if ("QUESTION".equals(image.getRole())) {
          firstQuestion = image.getUrl();
          break;
        }
      }
    }
    item.setQuestionImage(firstQuestion);
    item.setSourceName(q.getSourceName());
    item.setQuestionNumber(q.getQuestionNumber());
    item.setStandardAnswer(q.getStandardAnswer());
    item.setKnowledgePoints(q.getKnowledgePoints());
    item.setTags(q.getTags());
    item.setKnowledgeAnalysis(q.getKnowledgeAnalysis());
    item.setWrongDate(q.getWrongDate());
    item.setStatus(q.getStatus());
    item.setCreatedAt(q.getCreatedAt());
    item.setUpdatedAt(q.getUpdatedAt());
    return item;
  }
}
