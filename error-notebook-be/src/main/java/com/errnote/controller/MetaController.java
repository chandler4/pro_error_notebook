package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.config.EnumProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 元数据接口（api-contract §3.11）。 */
@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
@Tag(name = "meta", description = "枚举字典与知识点归并候选")
public class MetaController {

  private final EnumProperties enumProperties;

  /** GET /api/v1/meta/enums：{subjects, gradeLevels, questionTypes, subjectTabs}（yml 配置化）。 */
  @GetMapping("/enums")
  @Operation(summary = "枚举字典（T0-2 走通切片；knowledge-points 联想随 B-10 落地）")
  public ApiResponse<Map<String, Object>> enums() {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("subjects", enumProperties.getSubjects());
    data.put("gradeLevels", enumProperties.getGradeLevels());
    data.put("questionTypes", enumProperties.getQuestionTypes());
    data.put("subjectTabs", enumProperties.getSubjectTabs());
    return ApiResponse.ok(data);
  }
}
