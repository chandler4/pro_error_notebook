package com.errnote.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.config.EnumProperties;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** T0-2：GET /api/v1/meta/enums 返回 yml 枚举（统一结构 + 与配置一致）。 */
@WebMvcTest(MetaController.class)
class MetaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private EnumProperties enumProperties;

  @Test
  void enumsReturnsUnifiedResponseWithYmlValues() throws Exception {
    given(enumProperties.getSubjects()).willReturn(Arrays.asList("math", "physics"));
    given(enumProperties.getGradeLevels()).willReturn(Arrays.asList("grade9"));
    given(enumProperties.getQuestionTypes()).willReturn(Arrays.asList("fillBlank"));
    given(enumProperties.getSubjectTabs())
        .willReturn(Arrays.asList("math", "physics", "chemistry"));

    mockMvc
        .perform(get("/api/v1/meta/enums"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.subjects[0]").value("math"))
        .andExpect(jsonPath("$.data.subjectTabs.length()").value(3))
        .andExpect(jsonPath("$.data.gradeLevels[0]").value("grade9"))
        .andExpect(jsonPath("$.data.questionTypes[0]").value("fillBlank"));
  }
}
