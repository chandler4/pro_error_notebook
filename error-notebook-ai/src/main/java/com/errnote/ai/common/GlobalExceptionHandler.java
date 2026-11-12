package com.errnote.ai.common;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常映射（api-contract §2：HTTP 状态恒 200，业务结果由 code 表达）。
 *
 * <p>42001/42002（BusinessException）原样透出；参数校验失败 40000（message 含字段级明细）；兜底 50000
 * 通用文案——未预期异常堆栈只进日志，不回传调用方（security.md：不泄露内部细节）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ApiResponse<Void> handleBusiness(BusinessException e) {
    log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
    return ApiResponse.error(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
    log.warn("参数校验失败 {}", detail);
    return ApiResponse.error(40000, "参数校验失败：" + detail);
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleUnexpected(Exception e) {
    log.error("未预期异常", e);
    return ApiResponse.error(50000, "系统内部错误");
  }
}
