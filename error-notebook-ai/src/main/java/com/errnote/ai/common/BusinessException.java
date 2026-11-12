package com.errnote.ai.common;

import lombok.Getter;

/** 业务异常（AI 服务内部错误码：42001 输出非法已重试 / 42002 服务不可达——api-contract §7.1）。 */
@Getter
public class BusinessException extends RuntimeException {

  private final int code;

  public BusinessException(int code, String message) {
    super(message);
    this.code = code;
  }
}
