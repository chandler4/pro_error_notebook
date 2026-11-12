package com.errnote.dto;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** 列表筛选参数（api-contract 3.5）：query string 按名绑定，全部可选。 */
@Data
public class QuestionQuery {

  /** 科目精确匹配（TC-L-01）。 */
  private String subject;

  /** 题干模糊（TC-L-02）。 */
  private String keyword;

  /** 知识点模糊（JSON 列 LIKE，命中任一即可，TC-L-03）。 */
  private String knowledgePoint;

  /** 标签模糊（JSON 列 LIKE，命中任一标签，FR-B7/TC-T-02）。 */
  private String tag;

  private String questionType;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateFrom;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate dateTo;

  private Integer page;

  private Integer size;
}
