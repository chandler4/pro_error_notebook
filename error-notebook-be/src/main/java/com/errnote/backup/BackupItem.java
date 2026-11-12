package com.errnote.backup;

import com.errnote.dto.ImageRef;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 备份迁移条目（api-contract 3.10 / BR-08）：规约 JSON 字段 + questionImage 兼容（首个 QUESTION 角色图， TC-D-02）+
 * questionImages 扩展。
 */
@Data
public class BackupItem {

  private String errorQuestionId;
  private String subject;
  private String gradeLevel;
  private String questionType;
  private String questionStem;

  /** 兼容字段：单图场景（规约数据结构 questionImage）。 */
  private String questionImage;

  /** 扩展字段：多图 + 角色（现行模型）。 */
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
}
