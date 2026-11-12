package com.errnote.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包装（api-contract §1）：{code, message, data}，HTTP 状态恒 200，业务结果由 code 表达。
 *
 * <p>错误码表见 api-contract §2（B-1 扩展 ErrorCode 枚举与全局异常处理）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

  private int code;
  private String message;
  private T data;

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "OK", data);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}
