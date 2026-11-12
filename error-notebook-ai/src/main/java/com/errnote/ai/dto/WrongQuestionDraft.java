package com.errnote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI 解析草稿（规约数据结构）。tags 恒为空数组（BR-11：手工标注，AI 不自动生成——TC-T-04）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 解析草稿（与规约 JSON 字段一致）")
public class WrongQuestionDraft {

  @Schema(description = "科目（yml 枚举值，如 physics）")
  private String subject;

  @Schema(description = "年级（如 grade9）")
  private String gradeLevel;

  @Schema(description = "题型（如 fillBlank）")
  private String questionType;

  @Schema(description = "题干（Markdown/LaTeX）")
  private String questionStem;

  @Schema(description = "试卷/作业来源名称")
  private String sourceName;

  @Schema(description = "题号")
  private String questionNumber;

  @Schema(description = "标准答案（多空多答案）")
  private List<String> standardAnswer;

  @Schema(description = "知识点名称列表")
  private List<String> knowledgePoints;

  @Schema(description = "解析（Markdown/LaTeX）")
  private String knowledgeAnalysis;

  @Schema(description = "标签：恒为空数组（BR-11 纯手工标注，AI 不生成）")
  private List<String> tags;
}
