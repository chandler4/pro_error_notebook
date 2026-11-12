package com.errnote.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** 全局异常处理单测：错误码映射与字段明细（B-1 DoD；TC-C-01 前置的 40000 行为）。 */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void businessExceptionMapsToItsCodeAndMessage() {
    ApiResponse<Void> resp =
        handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND, "错题不存在"));
    assertThat(resp.getCode()).isEqualTo(40400);
    assertThat(resp.getMessage()).isEqualTo("错题不存在");
    assertThat(resp.getData()).isNull();
  }

  @Test
  void validationMapsTo40000WithFieldDetail() throws NoSuchMethodException {
    BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "req");
    binding.addError(new FieldError("req", "questionStem", "不能为空"));
    binding.addError(new FieldError("req", "subject", "不能为空"));
    MethodArgumentNotValidException e =
        new MethodArgumentNotValidException(
            new MethodParameter(Object.class.getDeclaredMethod("toString"), -1), binding);

    ApiResponse<Void> resp = handler.handleValidation(e);
    assertThat(resp.getCode()).isEqualTo(40000);
    assertThat(resp.getMessage()).contains("questionStem").contains("subject");
  }

  @Test
  void unexpectedMapsTo50000GenericWithoutStackTraceLeak() {
    ApiResponse<Void> resp = handler.handleUnexpected(new RuntimeException("db password is xyz"));
    assertThat(resp.getCode()).isEqualTo(50000);
    assertThat(resp.getMessage()).isEqualTo("系统内部错误");
    assertThat(resp.getMessage()).doesNotContain("xyz");
  }

  @Test
  void errorCodesCoverContractTable() {
    // api-contract 第2章 全表核对（防枚举漏项）
    Integer[] expected = {0, 40000, 40400, 41001, 41002, 41003, 42001, 42002, 43001, 43002, 50000};
    Integer[] actual =
        Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).toArray(Integer[]::new);
    assertThat(actual).containsExactlyInAnyOrder(expected);
  }
}
