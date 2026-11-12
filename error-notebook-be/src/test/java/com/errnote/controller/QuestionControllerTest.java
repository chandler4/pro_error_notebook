package com.errnote.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.dto.ImageRef;
import com.errnote.dto.QuestionSaveRequest;
import com.errnote.entity.WrongQuestion;
import com.errnote.service.QuestionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 错题接口测试（TC-C-01 必填 40000 明细 / TC-C-02 接口层 / 40400 / 40000 请求体）。 */
@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private QuestionService questionService;

  private static WrongQuestion confirmed() {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId("id-1");
    q.setSubject("physics");
    q.setGradeLevel("grade9");
    q.setQuestionType("fillBlank");
    q.setQuestionStem("确认后的题干");
    q.setImages(Arrays.asList(new ImageRef("images/202611/a1b2.jpg", "QUESTION")));
    q.setAnswers(Arrays.asList("12", "1:5"));
    q.setPoints(Arrays.asList("电压表读数"));
    q.setTagList(Arrays.asList("二刷", "易错"));
    q.setWrongDate(LocalDate.of(2026, 11, 10));
    q.setStatus(WrongQuestion.STATUS_CONFIRMED);
    q.setCreatedAt(LocalDateTime.of(2026, 11, 10, 9, 0));
    q.setUpdatedAt(LocalDateTime.of(2026, 11, 12, 8, 0));
    return q;
  }

  private static String saveBody(String subject, String stem) {
    return "{\"subject\":\""
        + subject
        + "\",\"questionStem\":\""
        + stem
        + "\","
        + "\"gradeLevel\":\"grade9\",\"questionType\":\"fillBlank\","
        + "\"questionImages\":[{\"url\":\"images/202611/a1b2.jpg\",\"role\":\"QUESTION\"}],"
        + "\"standardAnswer\":[\"12\"],\"knowledgePoints\":[\"电压表读数\"],\"tags\":[\"二刷\"],"
        + "\"wrongDate\":\"2026-11-10\"}";
  }

  // TC-C-01：subject/questionStem 缺失 → 40000 带字段明细
  @Test
  void confirmWithoutRequiredFieldsRejectedWith40000Detail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/questions/id-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"\",\"questionStem\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40000))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("subject")))
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("questionStem")));
  }

  // TC-C-02：确认成功 → CONFIRMED + id 不变 + 数组字段展开
  @Test
  void confirmReturnsConfirmedQuestion() throws Exception {
    given(
            questionService.confirm(
                org.mockito.ArgumentMatchers.any(String.class),
                org.mockito.ArgumentMatchers.any(QuestionSaveRequest.class)))
        .willReturn(confirmed());

    mockMvc
        .perform(
            post("/api/v1/questions/id-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("physics", "确认后的题干")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.data.errorQuestionId").value("id-1"))
        .andExpect(jsonPath("$.data.questionImages[0].role").value("QUESTION"))
        .andExpect(jsonPath("$.data.tags[1]").value("易错"));
  }

  @Test
  void getDetailReturnsCamelCaseFields() throws Exception {
    given(questionService.get("id-1")).willReturn(confirmed());

    mockMvc
        .perform(get("/api/v1/questions/id-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.knowledgePoints[0]").value("电压表读数"))
        .andExpect(jsonPath("$.data.standardAnswer[1]").value("1:5"))
        .andExpect(jsonPath("$.data.wrongDate").value("2026-11-10"));
  }

  @Test
  void getMissingReturns40400() throws Exception {
    given(questionService.get("missing"))
        .willThrow(new BusinessException(ErrorCode.NOT_FOUND, "错题不存在"));

    mockMvc
        .perform(get("/api/v1/questions/missing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40400));
  }

  @Test
  void putUpdatesFullFields() throws Exception {
    given(
            questionService.update(
                org.mockito.ArgumentMatchers.any(String.class),
                org.mockito.ArgumentMatchers.any(QuestionSaveRequest.class)))
        .willReturn(confirmed());

    mockMvc
        .perform(
            put("/api/v1/questions/id-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("physics", "编辑后题干")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
  }

  @Test
  void deleteQuestionReturnsOk() throws Exception {
    mockMvc
        .perform(delete("/api/v1/questions/id-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    willThrow(new BusinessException(ErrorCode.NOT_FOUND)).given(questionService).delete("missing");
    mockMvc
        .perform(delete("/api/v1/questions/missing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40400));
  }

  // 非法 JSON / 日期格式错误 → 40000（不泄漏解析细节）
  @Test
  void malformedBodyRejectedWith40000() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/questions/id-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"x\",\"wrongDate\":\"not-a-date\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40000));
  }
}
