package com.errnote.log.aspect;

import com.errnote.common.ApiResponse;
import com.errnote.common.BusinessException;
import com.errnote.common.JsonUtil;
import com.errnote.log.controller.LogQueryController;
import com.errnote.log.entity.BusinessLog;
import com.errnote.log.service.BusinessLogService;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 切面日志（FR-C1，BR-09，架构 §5.6）：拦截全部 @RestController 方法，记录请求参数、
 * 业务返回内容全文（ApiResponse.data）、成功状态、错误码与耗时，持久化 business_log。
 *
 * <p>防递归（api-contract 3.12）：日志查询控制器自身跳过；入库失败仅 warn（TC-G-04）。
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

  private static final int MAX_CONTENT_LENGTH = 4000;

  private final BusinessLogService businessLogService;
  private final Clock clock;

  public OperationLogAspect(BusinessLogService businessLogService, Clock clock) {
    this.businessLogService = businessLogService;
    this.clock = clock;
  }

  @Around("@within(org.springframework.web.bind.annotation.RestController)")
  public Object around(ProceedingJoinPoint pjp) throws Throwable {
    if (pjp.getTarget().getClass() == LogQueryController.class) {
      return pjp.proceed(); // 防递归：日志查询不产生日志（TC-G-05）
    }
    long start = clock.millis();
    try {
      Object result = pjp.proceed();
      record(pjp, result, null, start);
      return result;
    } catch (Throwable t) {
      record(pjp, null, t, start);
      throw t;
    }
  }

  private void record(ProceedingJoinPoint pjp, Object result, Throwable error, long start) {
    try {
      BusinessLog entry = new BusinessLog();
      entry.setLogType(BusinessLog.TYPE_OPERATION);
      entry.setModule(moduleOf(pjp));
      entry.setAction(actionOf(pjp));
      entry.setRequestContent(truncate(JsonUtil.toJson(pjp.getArgs())));
      entry.setResponseContent(responseOf(result, error));
      entry.setDurationMs(clock.millis() - start);

      if (error != null) {
        entry.setSuccess(false);
        entry.setErrorCode(
            error instanceof BusinessException ? ((BusinessException) error).getCode() : 50000);
      } else if (result instanceof ApiResponse) {
        int code = ((ApiResponse<?>) result).getCode();
        entry.setSuccess(code == 0);
        entry.setErrorCode(code);
      } else {
        entry.setSuccess(true);
        entry.setErrorCode(0);
      }
      businessLogService.save(entry);
    } catch (Exception e) {
      // TC-G-04：日志链路任何异常不得影响业务主流程
      log.warn("operation log recording failed: {}", e.getMessage());
    }
  }

  private String moduleOf(ProceedingJoinPoint pjp) {
    String simpleName = pjp.getTarget().getClass().getSimpleName();
    String method = ((MethodSignature) pjp.getSignature()).getMethod().getName();
    return simpleName + "#" + method;
  }

  private String actionOf(ProceedingJoinPoint pjp) {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    com.errnote.log.annotation.LogRecord record =
        signature.getMethod().getAnnotation(com.errnote.log.annotation.LogRecord.class);
    if (record == null) {
      record = pjp.getTarget().getClass().getAnnotation(com.errnote.log.annotation.LogRecord.class);
    }
    String action = record != null ? record.action() : "";
    return action.isEmpty() ? signature.getMethod().getName() : action;
  }

  /** 业务返回内容全文（架构 §5.6：ApiResponse.data 全文）。 */
  private String responseOf(Object result, Throwable error) {
    if (error != null) {
      return truncate(error.getClass().getSimpleName() + ": " + error.getMessage());
    }
    if (result instanceof ApiResponse) {
      Object data = ((ApiResponse<?>) result).getData();
      return truncate(JsonUtil.toJson(data));
    }
    return truncate(JsonUtil.toJson(result));
  }

  private String truncate(String content) {
    if (content == null) {
      return null;
    }
    return content.length() <= MAX_CONTENT_LENGTH
        ? content
        : content.substring(0, MAX_CONTENT_LENGTH);
  }
}
