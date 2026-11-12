package com.errnote.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.errnote.dto.ImageRef;
import com.errnote.entity.WrongQuestion;
import com.errnote.log.entity.BusinessLog;
import com.errnote.log.mapper.BusinessLogMapper;
import com.errnote.mapper.WrongQuestionMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 列表/草稿接口集成测试（H2 内存库，真 SQL 语义）：TC-L-01~05、TC-T-02、3.8 草稿、TC-P-07 零 AI 留痕。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionListIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private WrongQuestionMapper wrongQuestionMapper;
  @Autowired private BusinessLogMapper businessLogMapper;

  private WrongQuestion insertConfirmed(
      String id, String subject, String stem, String type, LocalDate wrongDate, String tagsJson) {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId(id);
    q.setSubject(subject);
    q.setGradeLevel("grade9");
    q.setQuestionType(type);
    q.setQuestionStem(stem);
    q.setImages(Arrays.asList(new ImageRef("images/202611/" + id + ".jpg", "QUESTION")));
    q.setAnswers(Arrays.asList("12"));
    q.setPoints(Arrays.asList("电压表读数", "串联电路分压规律"));
    q.setTagList(Arrays.asList(tagsJson.split(",")));
    q.setWrongDate(wrongDate);
    q.setStatus(WrongQuestion.STATUS_CONFIRMED);
    LocalDateTime now = LocalDateTime.of(2026, 11, 12, 8, 0);
    q.setCreatedAt(now);
    q.setUpdatedAt(now);
    wrongQuestionMapper.insert(q);
    return q;
  }

  // TC-L-01：科目筛选仅返回该科目 CONFIRMED（草稿不出现）
  @Test
  void subjectFilterReturnsOnlyConfirmedOfSubject() throws Exception {
    insertConfirmed(
        "q-physics", "physics", "串联电路实验", "fillBlank", LocalDate.of(2026, 11, 10), "二刷");
    insertConfirmed("q-math", "math", "二次函数图像", "choice", LocalDate.of(2026, 11, 11), "易错");
    insertPending("q-draft", "physics", "草稿题干");

    mockMvc
        .perform(get("/api/v1/questions").param("subject", "physics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.list[0].errorQuestionId").value("q-physics"));
  }

  // TC-L-02：题干模糊 命中/不命中/空关键词
  @Test
  void keywordFuzzySearchHitMissAndEmpty() throws Exception {
    insertConfirmed("q-1", "physics", "串联电路电压规律实验", "fillBlank", LocalDate.of(2026, 11, 10), "二刷");
    insertConfirmed("q-2", "physics", "平面镜成像探究", "experiment", LocalDate.of(2026, 11, 9), "易错");

    mockMvc
        .perform(get("/api/v1/questions").param("keyword", "串联电路"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.list[0].errorQuestionId").value("q-1"));
    mockMvc
        .perform(get("/api/v1/questions").param("keyword", "化学反应"))
        .andExpect(jsonPath("$.data.total").value(0));
    mockMvc.perform(get("/api/v1/questions")).andExpect(jsonPath("$.data.total").value(2));
  }

  // TC-L-03 / TC-T-02：知识点、标签 LIKE 命中任一元素
  @Test
  void knowledgePointAndTagLikeHitAnyElement() throws Exception {
    insertConfirmed("q-1", "physics", "题干A", "fillBlank", LocalDate.of(2026, 11, 10), "二刷,易错");
    insertConfirmed("q-2", "physics", "题干B", "fillBlank", LocalDate.of(2026, 11, 9), "高频");

    mockMvc
        .perform(get("/api/v1/questions").param("knowledgePoint", "分压规律"))
        .andExpect(jsonPath("$.data.total").value(2)); // 两条 seeded 知识点相同
    mockMvc
        .perform(get("/api/v1/questions").param("tag", "易错"))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.list[0].errorQuestionId").value("q-1"));
    mockMvc
        .perform(get("/api/v1/questions").param("tag", ""))
        .andExpect(jsonPath("$.data.total").value(2)); // 空 tag 不过滤
  }

  // TC-L-04：分页边界（超尾页空列表、size>50 收敛）
  @Test
  void pagingBoundsConverge() throws Exception {
    insertConfirmed("q-1", "physics", "题干A", "fillBlank", LocalDate.of(2026, 11, 10), "二刷");
    insertConfirmed("q-2", "physics", "题干B", "fillBlank", LocalDate.of(2026, 11, 9), "二刷");
    insertConfirmed("q-3", "physics", "题干C", "fillBlank", LocalDate.of(2026, 11, 8), "二刷");

    mockMvc
        .perform(get("/api/v1/questions").param("page", "1").param("size", "2"))
        .andExpect(jsonPath("$.data.total").value(3))
        .andExpect(jsonPath("$.data.list.length()").value(2))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.size").value(2));
    mockMvc
        .perform(get("/api/v1/questions").param("page", "99"))
        .andExpect(jsonPath("$.data.total").value(3))
        .andExpect(jsonPath("$.data.list.length()").value(0));
    mockMvc
        .perform(get("/api/v1/questions").param("size", "999"))
        .andExpect(jsonPath("$.data.size").value(50)); // 收敛 50
  }

  // TC-L-05：科目+题型+日期区间组合；排序 wrongDate 倒序
  @Test
  void combinedFilterAndOrdering() throws Exception {
    insertConfirmed(
        "q-old", "physics", "题干A", "fillBlank", LocalDate.of(2026, 10, 30), "二刷"); // 日期出界
    insertConfirmed("q-mid", "physics", "题干B", "fillBlank", LocalDate.of(2026, 11, 5), "二刷");
    insertConfirmed("q-new", "physics", "题干C", "fillBlank", LocalDate.of(2026, 11, 11), "二刷");
    insertConfirmed(
        "q-choice", "physics", "题干D", "choice", LocalDate.of(2026, 11, 10), "二刷"); // 题型出界
    insertConfirmed("q-math", "math", "题干E", "fillBlank", LocalDate.of(2026, 11, 10), "二刷"); // 科目出界

    mockMvc
        .perform(
            get("/api/v1/questions")
                .param("subject", "physics")
                .param("questionType", "fillBlank")
                .param("dateFrom", "2026-11-01")
                .param("dateTo", "2026-11-30"))
        .andExpect(jsonPath("$.data.total").value(2))
        .andExpect(jsonPath("$.data.list[0].errorQuestionId").value("q-new"))
        .andExpect(jsonPath("$.data.list[1].errorQuestionId").value("q-mid"));
  }

  // 3.8：空草稿创建/列表/删除；TC-P-07 零 AI 留痕（无 AI_CALL 业务日志）
  @Test
  void draftLifecycleWithoutAnyAiCall() throws Exception {
    insertPending("q-draft", "physics", "既有草稿");

    String content =
        mockMvc
            .perform(post("/api/v1/questions/drafts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"))
            .andExpect(jsonPath("$.data.questionImages.length()").value(0))
            .andExpect(jsonPath("$.data.tags.length()").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String newId = JsonPath.read(content, "$.data.errorQuestionId");

    mockMvc
        .perform(get("/api/v1/questions/drafts"))
        .andExpect(jsonPath("$.data.total").value(2)) // 既有 + 新建（创建倒序）
        .andExpect(jsonPath("$.data.list[0].status").value("PENDING_CONFIRM"));

    mockMvc
        .perform(delete("/api/v1/questions/drafts/" + newId))
        .andExpect(jsonPath("$.code").value(0));

    int aiCalls =
        businessLogMapper
            .selectCount(
                new LambdaQueryWrapper<BusinessLog>().eq(BusinessLog::getLogType, "AI_CALL"))
            .intValue();
    assertThat(aiCalls).as("TC-P-07：转手动填写零 AI 调用").isZero();
  }

  private void insertPending(String id, String subject, String stem) {
    WrongQuestion q = new WrongQuestion();
    q.setErrorQuestionId(id);
    q.setSubject(subject);
    q.setQuestionStem(stem);
    q.setImages(Arrays.asList());
    q.setTagList(Arrays.asList());
    q.setStatus(WrongQuestion.STATUS_PENDING);
    LocalDateTime now = LocalDateTime.of(2026, 11, 12, 7, 0);
    q.setCreatedAt(now);
    q.setUpdatedAt(now);
    wrongQuestionMapper.insert(q);
  }
}
