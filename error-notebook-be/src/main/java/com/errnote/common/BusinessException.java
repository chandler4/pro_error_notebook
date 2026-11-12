package com.errnote.common;

import lombok.Getter;

/** 业务异常：携带契约错误码，由全局异常处理映射为统一响应（HTTP 恒 200）。 */
@Getter
public class BusinessException extends RuntimeException {

  private final int code;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.code = errorCode.getCode();
  }

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.code = errorCode.getCode();
  }

  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }
}
