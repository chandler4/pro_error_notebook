package com.errnote.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.common.ApiResponse;
import com.errnote.integration.ai.AiServiceClient;
import com.errnote.integration.ai.dto.AiParseResponse;
import com.errnote.integration.ai.dto.WrongQuestionDraftDto;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** T0-2：POST /api/v1/demo/parse 透传 AI 服务内部契约响应（FE→BE→AI(mock) 切片）。 */
@WebMvcTest(DemoController.class)
class DemoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AiServiceClient aiServiceClient;

  @Test
  void parseForwardsAiMockResponse() throws Exception {
    AiParseResponse aiResp =
        AiParseResponse.builder()
            .draft(
                WrongQuestionDraftDto.builder()
                    .subject("physics")
                    .tags(Collections.emptyList())
                    .build())
            .rawModelResponse("{\"mock\":true}")
            .model("glm-4v")
            .durationMs(42L)
            .build();
    given(aiServiceClient.parse(any())).willReturn(ApiResponse.ok(aiResp));

    mockMvc
        .perform(
            post("/api/v1/demo/parse")
                .contentType(APPLICATION_JSON)
                .content("{\"images\":[{\"url\":\"images/demo/q.jpg\",\"role\":\"QUESTION\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.model").value("glm-4v"))
        .andExpect(jsonPath("$.data.draft.subject").value("physics"))
        .andExpect(jsonPath("$.data.rawModelResponse").exists());
  }
}
