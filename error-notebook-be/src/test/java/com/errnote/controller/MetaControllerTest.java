package com.errnote.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.config.EnumProperties;
import com.errnote.entity.KnowledgePoint;
import com.errnote.service.KnowledgeService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** 元数据接口测试：enums 与 yml 一致（T0-2）；knowledge-points 联想候选（B-10 / 契约 3.11）。 */
@WebMvcTest(MetaController.class)
class MetaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private EnumProperties enumProperties;
  @MockBean private KnowledgeService knowledgeService;

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

  // 3.11：[{name, useCount}] camelCase
  @Test
  void knowledgePointsReturnsNameAndUseCount() throws Exception {
    KnowledgePoint kp1 = new KnowledgePoint();
    kp1.setName("电压表读数");
    kp1.setUseCount(3);
    KnowledgePoint kp2 = new KnowledgePoint();
    kp2.setName("串联电路分压规律");
    kp2.setUseCount(1);
    given(knowledgeService.candidateEntities("电压", 10)).willReturn(Arrays.asList(kp1, kp2));

    mockMvc
        .perform(get("/api/v1/meta/knowledge-points").param("keyword", "电压"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0].name").value("电压表读数"))
        .andExpect(jsonPath("$.data[0].useCount").value(3))
        .andExpect(jsonPath("$.data[1].useCount").value(1));
  }

  // 3.11：空匹配返回 []
  @Test
  void knowledgePointsEmptyMatchReturnsEmptyArray() throws Exception {
    given(knowledgeService.candidateEntities("不存在", 10))
        .willReturn(Collections.<KnowledgePoint>emptyList());

    mockMvc
        .perform(get("/api/v1/meta/knowledge-points").param("keyword", "不存在"))
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.length()").value(0));
  }

  // limit 默认 10，非正值归一 10
  @Test
  void knowledgePointsNormalizesNonPositiveLimit() throws Exception {
    given(knowledgeService.candidateEntities(null, 10))
        .willReturn(Collections.<KnowledgePoint>emptyList());

    mockMvc
        .perform(get("/api/v1/meta/knowledge-points").param("limit", "0"))
        .andExpect(jsonPath("$.code").value(0));
  }
}
