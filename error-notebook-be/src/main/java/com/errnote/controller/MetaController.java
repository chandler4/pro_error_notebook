package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.config.EnumProperties;
import com.errnote.entity.KnowledgePoint;
import com.errnote.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 元数据接口（api-contract 3.11）。 */
@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
@Tag(name = "meta", description = "枚举字典与知识点归并候选")
public class MetaController {

  private final EnumProperties enumProperties;
  private final KnowledgeService knowledgeService;

  /** GET /api/v1/meta/enums：{subjects, gradeLevels, questionTypes, subjectTabs}（yml 配置化）。 */
  @GetMapping("/enums")
  @Operation(summary = "枚举字典（yml 配置化改配置重启生效）")
  public ApiResponse<Map<String, Object>> enums() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("subjects", enumProperties.getSubjects());
    data.put("gradeLevels", enumProperties.getGradeLevels());
    data.put("questionTypes", enumProperties.getQuestionTypes());
    data.put("subjectTabs", enumProperties.getSubjectTabs());
    return ApiResponse.ok(data);
  }

  /** GET /api/v1/meta/knowledge-points：归并候选 [{name, useCount}]（camelCase，空匹配返回 []）。 */
  @GetMapping("/knowledge-points")
  @Operation(summary = "知识点联想候选（keyword 包含匹配 + use_count 降序 + limit，默认 10）")
  public ApiResponse<List<Map<String, Object>>> knowledgePoints(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "10") int limit) {
    int safeLimit = limit <= 0 ? 10 : limit;
    List<Map<String, Object>> items = new ArrayList<>();
    for (KnowledgePoint kp : knowledgeService.candidateEntities(keyword, safeLimit)) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("name", kp.getName());
      item.put("useCount", kp.getUseCount());
      items.add(item);
    }
    return ApiResponse.ok(items);
  }
}
