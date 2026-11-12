package com.errnote.dto;

import com.errnote.entity.WrongQuestion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 详情响应结构（api-contract 3.4，错题/草稿共用）：实体 JSON TEXT 列在此展开为规约数组形态（TC-D-03 camelCase 与规约一致）。 */
@Data
@Builder
public class QuestionDetail {

  private String errorQuestionId;
  private String subject;
  private String gradeLevel;
  private String questionType;
  private String questionStem;
  private List<ImageRef> questionImages;
  private String sourceName;
  private String questionNumber;
  private List<String> standardAnswer;
  private List<String> knowledgePoints;
  private List<String> tags;
  private String knowledgeAnalysis;
  private LocalDate wrongDate;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** 实体 → 响应 DTO（JSON 列经领域访问器展开）。 */
  public static QuestionDetail from(WrongQuestion q) {
    return QuestionDetail.builder()
        .errorQuestionId(q.getErrorQuestionId())
        .subject(q.getSubject())
        .gradeLevel(q.getGradeLevel())
        .questionType(q.getQuestionType())
        .questionStem(q.getQuestionStem())
        .questionImages(q.images())
        .sourceName(q.getSourceName())
        .questionNumber(q.getQuestionNumber())
        .standardAnswer(q.answers())
        .knowledgePoints(q.points())
        .tags(q.tagList())
        .knowledgeAnalysis(q.getKnowledgeAnalysis())
        .wrongDate(q.getWrongDate())
        .status(q.getStatus())
        .createdAt(q.getCreatedAt())
        .updatedAt(q.getUpdatedAt())
        .build();
  }
}
