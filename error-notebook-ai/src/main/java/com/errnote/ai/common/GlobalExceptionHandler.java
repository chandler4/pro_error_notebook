package com.errnote.ai.common;

import com.errnote.ai.config.AiModelProperties;
import com.errnote.ai.dto.ParseResponse;
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
 *
 * <p>W-4（2026-11-13 chandler 确认）：错误信封 data 恒为 {"model": 配置模型名}，供 BE business_log 失败留痕； 仅此一字段，其余
 * data 内容（draft/raw/duration）错误路径不携带。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private final AiModelProperties aiModelProperties;

  public GlobalExceptionHandler(AiModelProperties aiModelProperties) {
    this.aiModelProperties = aiModelProperties;
  }

  @ExceptionHandler(BusinessException.class)
  public ApiResponse<ParseResponse> handleBusiness(BusinessException e) {
    log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
    return ApiResponse.error(e.getCode(), e.getMessage(), modelOnly());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<ParseResponse> handleValidation(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
    log.warn("参数校验失败 {}", detail);
    return ApiResponse.error(40000, "参数校验失败：" + detail, modelOnly());
  }

  @ExceptionHandler(Exception.class)
  public ApiResponse<ParseResponse> handleUnexpected(Exception e) {
    log.error("未预期异常", e);
    return ApiResponse.error(50000, "系统内部错误", modelOnly());
  }

  private ParseResponse modelOnly() {
    String model = aiModelProperties.getModel();
    return ParseResponse.builder().model(model == null ? "" : model).build();
  }
}
