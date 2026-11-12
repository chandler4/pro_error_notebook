package com.errnote.common;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理（架构 §5.6）：统一映射错误码；HTTP 恒 200；50000 通用文案不泄露堆栈（security.md §8）。 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 业务异常：透传错误码与文案（api-contract §2）。 */
  @ExceptionHandler(BusinessException.class)
  public ApiResponse<Void> handleBusiness(BusinessException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
  }

  /** JSR-303 参数校验失败：40000 + 字段级明细（api-contract §2：message 含字段级明细）。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(), detail);
  }

  /** 请求体不可读（JSON 格式/日期非法等）：40000，不暴露底层解析细节。 */
  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ApiResponse<Void> handleUnreadable(
      org.springframework.http.converter.HttpMessageNotReadableException e) {
    return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(), "请求体不可读或字段格式错误");
  }

  /** 未预期异常：50000 通用文案；堆栈只进系统运行日志（logback），不外泄。 */
  @ExceptionHandler(Exception.class)
  public ApiResponse<Void> handleUnexpected(Exception e) {
    log.error("unhandled exception", e);
    return ApiResponse.error(
        ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getDefaultMessage());
  }

  /** multipart 请求体超限（spring.servlet.multipart 上限触发）：映射 41003。 */
  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ApiResponse<Void> handleMaxUpload(
      org.springframework.web.multipart.MaxUploadSizeExceededException e) {
    return ApiResponse.error(
        ErrorCode.IMAGE_TOO_MANY.getCode(), ErrorCode.IMAGE_TOO_MANY.getDefaultMessage());
  }
}
