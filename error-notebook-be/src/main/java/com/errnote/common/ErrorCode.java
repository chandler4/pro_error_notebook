package com.errnote.common;

/** 业务错误码（api-contract.md 第 2 章错误码表，唯一基线）。 */
public enum ErrorCode {
  OK(0, "OK"),
  PARAM_INVALID(40000, "参数校验失败"),
  NOT_FOUND(40400, "资源不存在"),
  IMAGE_FORMAT_UNSUPPORTED(41001, "图片格式不支持"),
  IMAGE_TOO_LARGE(41002, "图片超过大小限制"),
  IMAGE_TOO_MANY(41003, "图片数量超限"),
  AI_PARSE_FAILED(42001, "AI 解析失败"),
  AI_UNAVAILABLE(42002, "AI 服务不可达"),
  IMPORT_FORMAT_ERROR(43001, "导入文件格式错误"),
  IMPORT_DATA_INVALID(43002, "导入数据校验失败"),
  INTERNAL_ERROR(50000, "系统内部错误");

  private final int code;
  private final String defaultMessage;

  ErrorCode(int code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  public int getCode() {
    return code;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
