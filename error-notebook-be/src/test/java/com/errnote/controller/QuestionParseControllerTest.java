package com.errnote.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.dto.ImageRef;
import com.errnote.entity.WrongQuestion;
import com.errnote.service.ParseService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 解析接口测试（TC-P-05 接口层：草稿入库响应 PENDING_CONFIRM + JSON 列展开为数组形态）。 */
@WebMvcTest(QuestionParseController.class)
class QuestionParseControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ParseService parseService;

  private static WrongQuestion pendingDraft() {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId("3f8a2c1e-6b7d-4e29-9c1f-8d2e7a4b5c60");
    q.setSubject("physics");
    q.setGradeLevel("grade9");
    q.setQuestionType("fillBlank");
    q.setQuestionStem("在探究\"串联电路电压规律\"的实验中……");
    q.setImages(
        Arrays.asList(
            new ImageRef("images/202611/a1b2.jpg", "QUESTION"),
            new ImageRef("images/202611/c3d4.jpg", "ANSWER_SHEET")));
    q.setSourceName("2026—2027 学年度上学期九年级物理学科素养调研");
    q.setQuestionNumber("14");
    q.setAnswers(Arrays.asList("12", "1:5"));
    q.setPoints(Arrays.asList("电压表读数", "串联电路分压规律"));
    q.setTagList(Arrays.asList());
    q.setKnowledgeAnalysis("**步骤1** ……");
    q.setWrongDate(LocalDate.of(2026, 11, 12));
    q.setStatus(WrongQuestion.STATUS_PENDING);
    q.setCreatedAt(LocalDateTime.of(2026, 11, 12, 8, 0));
    q.setUpdatedAt(LocalDateTime.of(2026, 11, 12, 8, 0));
    return q;
  }

  @Test
  void parseReturnsPendingConfirmDraftWithArrayFields() throws Exception {
    given(parseService.parse(any())).willReturn(pendingDraft());

    mockMvc
        .perform(
            post("/api/v1/questions/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"images\":[{\"url\":\"images/202611/a1b2.jpg\",\"role\":\"QUESTION\"},"
                        + "{\"url\":\"images/202611/c3d4.jpg\",\"role\":\"ANSWER_SHEET\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"))
        .andExpect(jsonPath("$.data.errorQuestionId").value("3f8a2c1e-6b7d-4e29-9c1f-8d2e7a4b5c60"))
        .andExpect(jsonPath("$.data.questionImages[0].url").value("images/202611/a1b2.jpg"))
        .andExpect(jsonPath("$.data.questionImages[1].role").value("ANSWER_SHEET"))
        .andExpect(jsonPath("$.data.standardAnswer[0]").value("12"))
        .andExpect(jsonPath("$.data.knowledgePoints[1]").value("串联电路分压规律"));
  }

  // TC-P-05 错误分支：空 images → 40000
  @Test
  void emptyImagesRejectedWith40000() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/questions/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"images\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40000));
  }

  // 空白 url → 40000 字段级明细
  @Test
  void blankUrlRejectedWith40000() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/questions/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"  \",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40000))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("url")));
  }

  // 42001 透传（可重试/转手动填写）
  @Test
  void aiParseFailurePassesThrough() throws Exception {
    given(parseService.parse(any()))
        .willThrow(new BusinessException(ErrorCode.AI_PARSE_FAILED, "模型输出无效"));

    mockMvc
        .perform(
            post("/api/v1/questions/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"images\":[{\"url\":\"images/202611/a1b2.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(42001));
  }
}
