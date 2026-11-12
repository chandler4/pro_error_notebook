package com.errnote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.errnote.common.JsonUtil;
import com.errnote.dto.ImageRef;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * 错题实体（wrong_question 表）。四个 JSON TEXT 列经 JsonUtil 与领域集合互转（TC-D-01： 表字段与规约 JSON 一一映射）；MyBatis-Plus
 * 仅映射同名属性，领域访问器（无 get 前缀）不参与。
 */
@Data
@TableName("wrong_question")
public class WrongQuestion {

  public static final String STATUS_PENDING = "PENDING_CONFIRM";
  public static final String STATUS_CONFIRMED = "CONFIRMED";

  @TableId(type = IdType.INPUT)
  private String errorQuestionId;

  private String subject;

  private String gradeLevel;

  private String questionStem;

  /** JSON: [{url, role}]。 */
  private String questionImages;

  private String questionType;

  private String sourceName;

  private String questionNumber;

  /** JSON: ["12","1:5"]。 */
  private String standardAnswer;

  /** JSON: ["电压表读数", ...]。 */
  private String knowledgePoints;

  /** JSON: ["二刷","易错"]（BR-11 自由文本标签）。 */
  private String tags;

  private String knowledgeAnalysis;

  private LocalDate wrongDate;

  /** PENDING_CONFIRM / CONFIRMED。 */
  private String status;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  // ---- 领域访问器（JSON 列转换，null 安全） ----

  public List<ImageRef> images() {
    if (questionImages == null || questionImages.isEmpty()) {
      return Collections.emptyList();
    }
    return JsonUtil.convert(questionImages, new TypeReference<List<ImageRef>>() {});
  }

  public List<String> answers() {
    return JsonUtil.listFromJson(standardAnswer, String.class);
  }

  public List<String> points() {
    return JsonUtil.listFromJson(knowledgePoints, String.class);
  }

  public List<String> tagList() {
    return JsonUtil.listFromJson(tags, String.class);
  }

  public void setImages(List<ImageRef> images) {
    this.questionImages = JsonUtil.toJson(images);
  }

  public void setAnswers(List<String> answers) {
    this.standardAnswer = JsonUtil.toJson(answers);
  }

  public void setPoints(List<String> points) {
    this.knowledgePoints = JsonUtil.toJson(points);
  }

  public void setTagList(List<String> tags) {
    this.tags = JsonUtil.toJson(tags);
  }
}
