package com.errnote.ai.controller;

import com.errnote.ai.common.ApiResponse;
import com.errnote.ai.dto.ParseRequest;
import com.errnote.ai.dto.ParseResponse;
import com.errnote.ai.dto.WrongQuestionDraft;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collections;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部契约端点（api-contract §7.1）：POST /internal/parse。
 *
 * <p>T0-2 为 mock 返回（走通切片：固定返回规约示例物理题草稿）；正式实现（Controller → 防腐层，含重试与 42001/42002 异常映射）随 A-3/A-5 落地替换本
 * mock。
 */
@RestController
@RequestMapping("/internal")
@Tag(name = "internal", description = "BE↔AI 内部契约（仅回环 127.0.0.1:8081，无鉴权）")
public class ParseController {

  @PostMapping("/parse")
  @Operation(summary = "错题解析（T0-2 mock；A-5 接入防腐层真实调用）")
  public ApiResponse<ParseResponse> parse(@Valid @RequestBody ParseRequest request) {
    return ApiResponse.ok(mockResponse());
  }

  private ParseResponse mockResponse() {
    return ParseResponse.builder()
        .draft(mockDraft())
        .rawModelResponse("{\"mock\":true,\"note\":\"T0-2 固定 mock，A-5 替换为真实模型调用\"}")
        .model("glm-5.3-flash")
        .durationMs(42L)
        .build();
  }

  /** 规约示例（notebook.md 数据结构 · 物理题），与 fixtures/notebook-sample.json 同源。 */
  private WrongQuestionDraft mockDraft() {
    return WrongQuestionDraft.builder()
        .subject("physics")
        .gradeLevel("grade9")
        .questionType("fillBlank")
        .questionStem(
            "在探究\"串联电路电压规律\"的实验中，R₁、R₂串联，V₁测R₁、V₂测R₂，闭合开关后两电压表"
                + "指针偏转角度相同（如图）。则电源电压为 ______ V；R₁与R₂的阻值之比为 ______ 。")
        .sourceName("2026—2027 学年度上学期九年级物理学科素养调研")
        .questionNumber("14")
        .standardAnswer(Arrays.asList("12", "1:5"))
        .knowledgePoints(Arrays.asList("电压表读数", "串联电路分压规律"))
        .knowledgeAnalysis("**步骤1** 偏转角相同时，大量程读数是小量程的5倍……（T0-2 mock，完整解析随 A-4 模板）")
        .tags(Collections.emptyList())
        .build();
  }
}
