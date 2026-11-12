package com.errnote.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 业务日志实体（business_log 表，architecture-data §2.2；BR-09 永久保留）。 */
@Data
@TableName("business_log")
public class BusinessLog {

  public static final String TYPE_OPERATION = "OPERATION";
  public static final String TYPE_AI_CALL = "AI_CALL";

  @TableId(type = IdType.AUTO)
  private Long id;

  private String traceId;

  /** OPERATION / AI_CALL。 */
  private String logType;

  /** 类#方法。 */
  private String module;

  private String action;

  /** 请求参数摘要（JSON）。 */
  private String requestContent;

  /** AI 返回 / 业务返回全文（BR-09）。 */
  private String responseContent;

  private Boolean success;

  private Integer errorCode;

  private Long durationMs;

  private LocalDateTime createdAt;
}
