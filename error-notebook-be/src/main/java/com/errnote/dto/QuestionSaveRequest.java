package com.errnote.dto;

import java.time.LocalDate;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.Data;

/** confirm / PUT 编辑请求体（api-contract 3.3/3.6）：确认后的完整字段（人工修正结果）。 */
@Data
public class QuestionSaveRequest {

  /** 必填（TC-C-01）。 */
  @NotBlank private String subject;

  private String gradeLevel;

  private String questionType;

  /** 必填（TC-C-01）。 */
  @NotBlank private String questionStem;

  @Valid private List<ImageRef> questionImages;

  private String sourceName;

  private String questionNumber;

  private List<String> standardAnswer;

  private List<String> knowledgePoints;

  /** BR-11 自由文本标签，重复去重（TC-T-01）。 */
  private List<String> tags;

  private String knowledgeAnalysis;

  /** null 表示沿用原值。 */
  private LocalDate wrongDate;
}
