package com.errnote.ai.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.ai.acl.OpenAiVisionAdapter;
import com.errnote.ai.common.BusinessException;
import com.errnote.ai.config.AiModelProperties;
import com.errnote.ai.dto.ParseResponse;
import com.errnote.ai.dto.WrongQuestionDraft;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A-5 接口组装：POST /internal/parse 三态（api-contract §7.1 + §2 统一结构：HTTP 恒 200，业务结果由 code 表达）。
 *
 * <p>TC-T-04 tags 恒空穿透；TC-P-02/TC-P-03 业务码经全局异常映射为统一信封。 W-4（2026-11-13 chandler 确认）：错误信封 data 恒为
 * {"model": 配置模型名} 供 BE 失败留痕，其余 data 字段错误路径不携带。
 */
@WebMvcTest(ParseController.class)
@Import(AiModelProperties.class)
class ParseControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OpenAiVisionAdapter visionAdapter;

  @Test
  @DisplayName("成功：防腐层结果经统一信封返回，tags 恒空")
  void parseReturnsAdapterDraftWithUnifiedEnvelope() throws Exception {
    given(visionAdapter.parse(any())).willReturn(sampleResponse());

    mockMvc
        .perform(
            post("/internal/parse")
                .contentType(APPLICATION_JSON)
                .content(
                    "{\"images\":[{\"url\":\"images/202611/a1b2.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.model").value("glm-5.3-flash"))
        .andExpect(jsonPath("$.data.draft.subject").value("physics"))
        .andExpect(jsonPath("$.data.draft.standardAnswer.length()").value(2))
        .andExpect(jsonPath("$.data.draft.tags.length()").value(0))
        .andExpect(jsonPath("$.data.rawModelResponse").value("{\"subject\":\"physics\"}"))
        .andExpect(jsonPath("$.data.durationMs").value(18342));
  }

  @Test
  @DisplayName("TC-P-02 端到端：模型输出无效映射 42001，HTTP 恒 200；W-4 错误信封携带 model")
  void parseMapsInvalidModelOutputTo42001() throws Exception {
    given(visionAdapter.parse(any()))
        .willThrow(new BusinessException(42001, "AI 解析失败：模型输出无效（已重试 1 次）"));

    mockMvc
        .perform(
            post("/internal/parse")
                .contentType(APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"images/x.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(42001))
        .andExpect(jsonPath("$.message", containsString("已重试 1 次")))
        .andExpect(jsonPath("$.data.model").value("glm-5.3-flash"))
        .andExpect(jsonPath("$.data.draft").doesNotExist())
        .andExpect(jsonPath("$.data.rawModelResponse").doesNotExist());
  }

  @Test
  @DisplayName("TC-P-03 端到端：模型不可达映射 42002，不泄露网络细节；W-4 model 仍携带")
  void parseMapsUnreachableModelTo42002() throws Exception {
    given(visionAdapter.parse(any())).willThrow(new BusinessException(42002, "AI 服务不可达（网络错误或超时）"));

    mockMvc
        .perform(
            post("/internal/parse")
                .contentType(APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"images/x.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(42002))
        .andExpect(jsonPath("$.message", containsString("AI 服务不可达")))
        .andExpect(jsonPath("$.message").value(containsString("AI 服务不可达（网络错误或超时）")))
        .andExpect(jsonPath("$.data.model").value("glm-5.3-flash"));
  }

  @Test
  @DisplayName("空 images：参数校验失败映射 40000，message 含字段明细；W-4 model 携带")
  void parseRejectsEmptyImagesAs40000() throws Exception {
    mockMvc
        .perform(post("/internal/parse").contentType(APPLICATION_JSON).content("{\"images\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(40000))
        .andExpect(jsonPath("$.message", containsString("images")))
        .andExpect(jsonPath("$.data.model").value("glm-5.3-flash"));
  }

  @Test
  @DisplayName("W-4 兜底：未预期异常映射 50000，错误信封同样携带 model")
  void parseUnexpectedErrorMapsTo50000WithModel() throws Exception {
    given(visionAdapter.parse(any())).willThrow(new RuntimeException("boom"));

    mockMvc
        .perform(
            post("/internal/parse")
                .contentType(APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"images/x.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(50000))
        .andExpect(jsonPath("$.message").value("系统内部错误"))
        .andExpect(jsonPath("$.data.model").value("glm-5.3-flash"));
  }

  /** 规约示例（api-contract §7.1 响应 data · 物理题）。 */
  private ParseResponse sampleResponse() {
    return ParseResponse.builder()
        .draft(
            WrongQuestionDraft.builder()
                .subject("physics")
                .gradeLevel("grade9")
                .questionType("fillBlank")
                .questionStem("在探究\"串联电路电压规律\"的实验中……")
                .sourceName("2026—2027 学年度上学期九年级物理学科素养调研")
                .questionNumber("14")
                .standardAnswer(Arrays.asList("12", "1:5"))
                .knowledgePoints(Arrays.asList("电压表读数", "串联电路分压规律"))
                .knowledgeAnalysis("**步骤1** ……")
                .tags(Collections.emptyList())
                .build())
        .rawModelResponse("{\"subject\":\"physics\"}")
        .model("glm-5.3-flash")
        .durationMs(18342L)
        .build();
  }
}
