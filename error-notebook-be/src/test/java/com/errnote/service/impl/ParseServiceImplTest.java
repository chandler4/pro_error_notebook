package com.errnote.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.errnote.common.ApiResponse;
import com.errnote.common.BusinessException;
import com.errnote.common.IdGenerator;
import com.errnote.dto.ImageRef;
import com.errnote.entity.WrongQuestion;
import com.errnote.integration.ai.AiServiceClient;
import com.errnote.integration.ai.dto.AiParseRequest;
import com.errnote.integration.ai.dto.AiParseResponse;
import com.errnote.integration.ai.dto.WrongQuestionDraftDto;
import com.errnote.log.service.BusinessLogService;
import com.errnote.mapper.WrongQuestionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 解析编排单测（⭐TDD：TC-P-01 草稿映射 / TC-P-05 PENDING_CONFIRM 入库 / TC-P-03 网络异常→42002； TC-G-03 raw 全文落日志）。
 *
 * <p>v3.0 注记：重试收敛于 AI 服务防腐层，BE 对内部契约为单次调用（test-strategy §3.2 修订）。
 */
@ExtendWith(MockitoExtension.class)
class ParseServiceImplTest {

  @Mock private AiServiceClient aiServiceClient;
  @Mock private WrongQuestionMapper wrongQuestionMapper;
  @Mock private BusinessLogService businessLogService;

  private ParseServiceImpl service;
  private List<ImageRef> images;

  private static final String RAW = "{\"id\":\"resp-1\",\"choices\":[{\"message\":...}]}";

  @BeforeEach
  void setUp() {
    Clock fixed = Clock.fixed(Instant.parse("2026-11-12T08:00:00Z"), ZoneOffset.UTC);
    IdGenerator fixedId = () -> "fixed-uuid";
    service =
        new ParseServiceImpl(
            aiServiceClient, wrongQuestionMapper, businessLogService, fixedId, fixed);
    images =
        Arrays.asList(
            new ImageRef("images/202611/a1b2.jpg", "QUESTION"),
            new ImageRef("images/202611/c3d4.jpg", "ANSWER_SHEET"));
  }

  private static WrongQuestionDraftDto fixtureDraft() {
    // notebook.md 规约 4 标准夹具（物理填空题）
    return WrongQuestionDraftDto.builder()
        .subject("physics")
        .gradeLevel("grade9")
        .questionType("fillBlank")
        .questionStem("在探究\"串联电路电压规律\"的实验中……")
        .sourceName("2026—2027 学年度上学期九年级物理学科素养调研")
        .questionNumber("14")
        .standardAnswer(Arrays.asList("12", "1:5"))
        .knowledgePoints(Arrays.asList("电压表读数", "串联电路分压规律"))
        .knowledgeAnalysis("**步骤1** ……")
        .tags(Arrays.asList())
        .build();
  }

  private void mockAiOk(WrongQuestionDraftDto draft) {
    AiParseResponse data =
        AiParseResponse.builder()
            .draft(draft)
            .rawModelResponse(RAW)
            .model("glm-5.3-flash")
            .durationMs(1234L)
            .build();
    when(aiServiceClient.parse(any(AiParseRequest.class))).thenReturn(ApiResponse.ok(data));
  }

  // TC-P-01 / TC-P-05：解析成功 → 草稿完整映射入库 PENDING_CONFIRM；日志含 raw 全文
  @Test
  void parseOkMapsDraftPersistsPendingConfirmAndLogsRaw() {
    mockAiOk(fixtureDraft());

    WrongQuestion draft = service.parse(images);

    assertThat(draft.getStatus()).isEqualTo("PENDING_CONFIRM");
    assertThat(draft.getErrorQuestionId()).isEqualTo("fixed-uuid");
    assertThat(draft.getSubject()).isEqualTo("physics");
    assertThat(draft.getWrongDate().toString()).isEqualTo("2026-11-12");
    assertThat(draft.images()).hasSize(2);
    assertThat(draft.images().get(0).getUrl()).isEqualTo("images/202611/a1b2.jpg");
    assertThat(draft.images().get(1).getRole()).isEqualTo("ANSWER_SHEET");
    assertThat(draft.answers()).containsExactly("12", "1:5");
    assertThat(draft.points()).containsExactly("电压表读数", "串联电路分压规律");
    assertThat(draft.tagList()).isEmpty();

    ArgumentCaptor<WrongQuestion> captor = ArgumentCaptor.forClass(WrongQuestion.class);
    verify(wrongQuestionMapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo("PENDING_CONFIRM");

    // TC-G-03：raw 全文逐字落 business_log
    verify(businessLogService)
        .recordAi(
            org.mockito.ArgumentMatchers.eq("glm-5.3-flash"),
            org.mockito.ArgumentMatchers.eq(1234L),
            contains("images=2"),
            org.mockito.ArgumentMatchers.eq(RAW));
  }

  // TC-P-02（BE 侧）：AI 业务失败 42001 原样透传，不入库不落日志
  @Test
  void aiBusinessError42001PassesThroughWithoutPersisting() {
    when(aiServiceClient.parse(any(AiParseRequest.class)))
        .thenReturn(ApiResponse.error(42001, "AI 解析失败：模型输出无效（已重试 1 次）"));

    assertThatThrownBy(() -> service.parse(images))
        .isInstanceOf(BusinessException.class)
        .extracting("code", "message")
        .containsExactly(42001, "AI 解析失败：模型输出无效（已重试 1 次）");
    verifyNoInteractions(wrongQuestionMapper, businessLogService);
  }

  // 未知业务码归并为 42001（契约 3.2 仅暴露 42001/42002/40000）
  @Test
  void unknownAiCodeNormalizedTo42001() {
    when(aiServiceClient.parse(any(AiParseRequest.class)))
        .thenReturn(ApiResponse.error(50000, "内部异常"));

    assertThatThrownBy(() -> service.parse(images))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(42001);
    verifyNoInteractions(wrongQuestionMapper, businessLogService);
  }

  // TC-P-03：网络异常 → 42002，不泄漏底层异常细节
  @Test
  void networkErrorMappedTo42002WithoutLeakingDetails() {
    when(aiServiceClient.parse(any(AiParseRequest.class)))
        .thenThrow(new RuntimeException("Connection refused: /127.0.0.1:8081"));

    assertThatThrownBy(() -> service.parse(images))
        .isInstanceOf(BusinessException.class)
        .extracting("code", "message")
        .containsExactly(42002, "AI 服务不可达");
    verifyNoInteractions(wrongQuestionMapper, businessLogService);
  }

  // 成功码但 data/draft 缺失 → 42001（防腐：不信任 AI 输出完整性）
  @Test
  void missingDataOrDraftRejectedAs42001() {
    when(aiServiceClient.parse(any(AiParseRequest.class)))
        .thenReturn(ApiResponse.ok(null))
        .thenReturn(ApiResponse.ok(AiParseResponse.builder().build()));

    assertThatThrownBy(() -> service.parse(images)).extracting("code").isEqualTo(42001);
    assertThatThrownBy(() -> service.parse(images)).extracting("code").isEqualTo(42001);
    verifyNoInteractions(wrongQuestionMapper, businessLogService);
  }

  // durationMs/model 缺失时日志仍落（空值安全，不抛 NPE）
  @Test
  void recordAiToleratesMissingModelAndDuration() {
    AiParseResponse data =
        AiParseResponse.builder().draft(fixtureDraft()).rawModelResponse(RAW).build();
    when(aiServiceClient.parse(any(AiParseRequest.class))).thenReturn(ApiResponse.ok(data));

    service.parse(images);

    verify(businessLogService)
        .recordAi(anyString(), anyLong(), anyString(), org.mockito.ArgumentMatchers.eq(RAW));
  }
}
