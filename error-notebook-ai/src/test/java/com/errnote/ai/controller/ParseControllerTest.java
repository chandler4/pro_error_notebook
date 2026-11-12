package com.errnote.ai.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/** T0-2：POST /internal/parse 返回 mock 草稿（统一结构 + 规约示例字段 + tags 恒空）。 */
@WebMvcTest(ParseController.class)
class ParseControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void parseReturnsMockDraftWithUnifiedEnvelope() throws Exception {
    mockMvc
        .perform(
            post("/internal/parse")
                .contentType(APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"images/demo/q.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.model").value("glm-5.3-flash"))
        .andExpect(jsonPath("$.data.durationMs").value(42))
        .andExpect(jsonPath("$.data.draft.subject").value("physics"))
        .andExpect(jsonPath("$.data.draft.gradeLevel").value("grade9"))
        .andExpect(jsonPath("$.data.draft.questionType").value("fillBlank"))
        .andExpect(jsonPath("$.data.draft.standardAnswer.length()").value(2))
        .andExpect(jsonPath("$.data.draft.knowledgePoints.length()").value(2))
        .andExpect(jsonPath("$.data.rawModelResponse").exists());
  }

  @Test
  void parseRejectsEmptyImages() throws Exception {
    mockMvc
        .perform(post("/internal/parse").contentType(APPLICATION_JSON).content("{\"images\":[]}"))
        .andExpect(status().isBadRequest());
  }
}
