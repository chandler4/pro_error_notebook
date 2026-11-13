package com.errnote.common;

import lombok.Getter;

/**
 * 业务异常：携带契约错误码，由全局异常处理映射为统一响应（HTTP 恒 200）。
 *
 * <p>model 为可选日志留痕上下文（W-4，2026-11-13 chandler 确认）：AI 已响应但业务失败时，携带内部契约错误信封 data.model 供 business_log
 * 失败条目记录；传输层失败（AI 整体不可达）无从获知，保持 null。
 */
@Getter
public class BusinessException extends RuntimeException {

  private final int code;
  private final String model;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.code = errorCode.getCode();
    this.model = null;
  }

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.code = errorCode.getCode();
    this.model = null;
  }

  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
    this.model = null;
  }

  public BusinessException(ErrorCode errorCode, String message, String model) {
    super(message);
    this.code = errorCode.getCode();
    this.model = model;
  }
}
