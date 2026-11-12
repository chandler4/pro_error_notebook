package com.errnote.log.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.errnote.common.ApiResponse;
import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.log.controller.LogQueryController;
import com.errnote.log.entity.BusinessLog;
import com.errnote.log.service.BusinessLogService;
import java.lang.reflect.Method;
import java.time.Clock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** 切面日志域级单测（TC-G-01/02/04 + 防递归；不启动容器）。 */
class OperationLogAspectTest {

  private BusinessLogService service;
  private OperationLogAspect aspect;
  private ProceedingJoinPoint pjp;

  @BeforeEach
  void setUp() throws Throwable {
    service = mock(BusinessLogService.class);
    aspect = new OperationLogAspect(service, Clock.systemDefaultZone());
    pjp = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    Method method = SampleController.class.getDeclaredMethod("confirm", String.class);
    when(signature.getMethod()).thenReturn(method);
    when(pjp.getSignature()).thenReturn(signature);
    when(pjp.getTarget()).thenReturn(new SampleController());
    when(pjp.getArgs()).thenReturn(new Object[] {"id-1"});
  }

  // TC-G-01 规约5：切面记录成功请求（module=类#方法、业务返回全文、success=true、errorCode=0、耗时>0）
  @Test
  void successRequestIsRecordedWithFullResponse() throws Throwable {
    when(pjp.proceed()).thenReturn(ApiResponse.ok("已确认"));

    Object result = aspect.around(pjp);

    assertThat(result).isEqualTo(ApiResponse.ok("已确认"));
    ArgumentCaptor<BusinessLog> captor = ArgumentCaptor.forClass(BusinessLog.class);
    verify(service, times(1)).save(captor.capture());
    BusinessLog saved = captor.getValue();
    assertThat(saved.getLogType()).isEqualTo("OPERATION");
    assertThat(saved.getModule()).isEqualTo("SampleController#confirm");
    assertThat(saved.getResponseContent()).contains("已确认");
    assertThat(saved.getSuccess()).isTrue();
    assertThat(saved.getErrorCode()).isEqualTo(0);
    assertThat(saved.getDurationMs()).isGreaterThanOrEqualTo(0);
  }

  // TC-G-02 规约5：业务异常路径 success=false + 对应错误码入库，且异常继续抛出
  @Test
  void businessFailureIsRecordedAndRethrown() throws Throwable {
    when(pjp.proceed()).thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "错题不存在"));

    assertThatThrownBy(() -> aspect.around(pjp)).isInstanceOf(BusinessException.class);

    ArgumentCaptor<BusinessLog> captor = ArgumentCaptor.forClass(BusinessLog.class);
    verify(service).save(captor.capture());
    assertThat(captor.getValue().getSuccess()).isFalse();
    assertThat(captor.getValue().getErrorCode()).isEqualTo(40400);
  }

  // TC-G-04 规约5：日志入库失败不影响业务主流程（save 抛异常 → 业务方法正常返回）
  @Test
  void persistFailureDoesNotAffectBusiness() throws Throwable {
    when(pjp.proceed()).thenReturn(ApiResponse.ok("ok"));
    org.mockito.Mockito.doThrow(new RuntimeException("db down"))
        .when(service)
        .save(any(BusinessLog.class));

    Object result = aspect.around(pjp);

    assertThat(((ApiResponse<?>) result).getCode()).isEqualTo(0);
    verify(service, times(1)).save(any(BusinessLog.class));
  }

  // api-contract 3.12：日志查询接口自身不产生业务日志（防递归）
  @Test
  void logQueryControllerIsSkipped() throws Throwable {
    MethodSignature signature = mock(MethodSignature.class);
    Method method = LogQueryController.class.getDeclaredMethods()[0];
    when(signature.getMethod()).thenReturn(method);
    when(pjp.getSignature()).thenReturn(signature);
    when(pjp.getTarget()).thenReturn(new LogQueryController(null));
    when(pjp.proceed()).thenReturn(ApiResponse.ok(null));

    aspect.around(pjp);

    verify(service, never()).save(any(BusinessLog.class));
  }

  /** 测试用目标控制器（模拟被拦截的 RestController）。 */
  static class SampleController {
    public ApiResponse<String> confirm(String id) {
      return ApiResponse.ok("已确认");
    }
  }
}
