package com.errnote.ai.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应包装（api-contract §7.1：错误响应同统一结构 {code, message, data}）。
 *
 * <p>错误码：42001（输出非法，已重试 1 次）/ 42002（模型不可达/超时），A-3 防腐层映射。
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
