package com.errnote.integration.ai.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 解析草稿（规约数据结构，notebook.md「数据结构」）。
 *
 * <p>tags 恒为空数组（BR-11：标签纯手工标注，AI 不自动生成——TC-T-04）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongQuestionDraftDto {

  private String subject;
  private String gradeLevel;
  private String questionType;
  private String questionStem;
  private String sourceName;
  private String questionNumber;
  private List<String> standardAnswer;
  private List<String> knowledgePoints;
  private String knowledgeAnalysis;
  private List<String> tags;
}
