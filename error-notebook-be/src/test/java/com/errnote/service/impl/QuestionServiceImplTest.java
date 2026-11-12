package com.errnote.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.errnote.common.BusinessException;
import com.errnote.dto.ImageRef;
import com.errnote.dto.QuestionSaveRequest;
import com.errnote.entity.WrongQuestion;
import com.errnote.integration.storage.FileStorageService;
import com.errnote.mapper.WrongQuestionMapper;
import com.errnote.service.KnowledgeService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 错题域单测（⭐TDD：TC-C-02 确认转态 / TC-T-01 标签去重 / TC-C-05 硬删联动删图+减计 / 40400）。 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

  @Mock private WrongQuestionMapper wrongQuestionMapper;
  @Mock private KnowledgeService knowledgeService;
  @Mock private FileStorageService fileStorageService;

  private QuestionServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new QuestionServiceImpl(
            wrongQuestionMapper,
            knowledgeService,
            fileStorageService,
            Clock.fixed(Instant.parse("2026-11-12T08:00:00Z"), ZoneOffset.UTC));
  }

  private static WrongQuestion pendingWithImages() {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId("id-1");
    q.setSubject("physics");
    q.setQuestionStem("旧题干");
    q.setImages(
        Arrays.asList(
            new ImageRef("images/202611/a1b2.jpg", "QUESTION"),
            new ImageRef("images/202611/c3d4.jpg", "ANSWER_SHEET")));
    q.setPoints(Arrays.asList("电压表读数"));
    q.setTagList(Arrays.asList());
    q.setWrongDate(LocalDate.of(2026, 11, 10));
    q.setStatus(WrongQuestion.STATUS_PENDING);
    return q;
  }

  private static QuestionSaveRequest saveRequest() {
    QuestionSaveRequest r = new QuestionSaveRequest();
    r.setSubject("physics");
    r.setGradeLevel("grade9");
    r.setQuestionType("fillBlank");
    r.setQuestionStem("确认后的题干");
    r.setSourceName("期末调研");
    r.setQuestionNumber("14");
    r.setStandardAnswer(Arrays.asList("12", "1:5"));
    r.setKnowledgePoints(Arrays.asList("电压表读数", "串联电路分压规律"));
    r.setTags(Arrays.asList("二刷", "易错", "二刷"));
    r.setKnowledgeAnalysis("**步骤1**");
    return r;
  }

  // TC-C-02：confirm 转 CONFIRMED，errorQuestionId 不变，知识点归并
  @Test
  void confirmTransitionsToConfirmedAndAccumulatesKnowledge() {
    when(wrongQuestionMapper.selectById("id-1")).thenReturn(pendingWithImages());

    WrongQuestion confirmed = service.confirm("id-1", saveRequest());

    assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");
    assertThat(confirmed.getErrorQuestionId()).isEqualTo("id-1");
    assertThat(confirmed.getQuestionStem()).isEqualTo("确认后的题干");
    assertThat(confirmed.getWrongDate()).isEqualTo(LocalDate.of(2026, 11, 10)); // 未传沿用原值
    verify(wrongQuestionMapper).updateById(confirmed);
    verify(knowledgeService).accumulate(Arrays.asList("电压表读数", "串联电路分压规律"));
  }

  // TC-T-01：tags JSON 持久化 + 重复去重
  @Test
  void confirmPersistsDedupedTags() {
    when(wrongQuestionMapper.selectById("id-1")).thenReturn(pendingWithImages());

    WrongQuestion confirmed = service.confirm("id-1", saveRequest());

    assertThat(confirmed.tagList()).containsExactly("二刷", "易错");
    assertThat(confirmed.getTags()).isEqualTo("[\"二刷\",\"易错\"]"); // JSON 数组形态落库
  }

  // 3.6：编辑不改状态（CONFIRMED 保持），全字段覆盖
  @Test
  void updateKeepsStatusAndReplacesFields() {
    WrongQuestion existing = pendingWithImages();
    existing.setStatus(WrongQuestion.STATUS_CONFIRMED);
    when(wrongQuestionMapper.selectById("id-1")).thenReturn(existing);
    QuestionSaveRequest req = saveRequest();
    req.setWrongDate(LocalDate.of(2026, 11, 1)); // 显式改日期

    WrongQuestion updated = service.update("id-1", req);

    assertThat(updated.getStatus()).isEqualTo("CONFIRMED");
    assertThat(updated.getWrongDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    assertThat(updated.answers()).containsExactly("12", "1:5");
    verify(knowledgeService).accumulate(any(List.class));
  }

  // TC-C-05 / BR-04：硬删除 → 删记录 + 每张图删文件 + 知识点减计
  @Test
  void deleteRemovesRecordImagesAndDecrementsKnowledge() {
    WrongQuestion q = pendingWithImages();
    when(wrongQuestionMapper.selectById("id-1")).thenReturn(q);

    service.delete("id-1");

    verify(wrongQuestionMapper).deleteById("id-1");
    verify(fileStorageService).delete("images/202611/a1b2.jpg");
    verify(fileStorageService).delete("images/202611/c3d4.jpg");
    verify(knowledgeService).decrement(Arrays.asList("电压表读数"));
  }

  // 删图失败（delete 返回 false）不阻断删除事务（数据生命周期 §5）
  @Test
  void deleteContinuesWhenImageDeleteFails() {
    when(wrongQuestionMapper.selectById("id-1")).thenReturn(pendingWithImages());
    when(fileStorageService.delete(any(String.class))).thenReturn(false);

    service.delete("id-1");

    verify(wrongQuestionMapper).deleteById("id-1");
    verify(knowledgeService).decrement(any(List.class));
  }

  // null 集合安全：confirm 空 images/answers/tags 不抛 NPE
  @Test
  void confirmToleratesNullCollections() {
    when(wrongQuestionMapper.selectById("id-1")).thenReturn(pendingWithImages());
    QuestionSaveRequest req = saveRequest();
    req.setQuestionImages(null);
    req.setStandardAnswer(null);
    req.setTags(null);
    req.setKnowledgePoints(null);

    WrongQuestion confirmed = service.confirm("id-1", req);

    assertThat(confirmed.images()).isEmpty();
    assertThat(confirmed.answers()).isEmpty();
    assertThat(confirmed.tagList()).isEmpty();
    verify(knowledgeService).accumulate(Collections.<String>emptyList());
  }

  // 40400：不存在 → 详情/删除均抛 NOT_FOUND，且无写操作
  @Test
  void missingIdThrows40400WithoutWrites() {
    when(wrongQuestionMapper.selectById("missing")).thenReturn(null);

    assertThatThrownBy(() -> service.get("missing"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(40400);
    assertThatThrownBy(() -> service.delete("missing"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(40400);
    verify(wrongQuestionMapper, never()).deleteById(any(String.class));
  }
}
