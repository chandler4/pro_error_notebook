package com.errnote.log.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 业务日志标注（FR-C1）：可选覆盖 module/action 描述；未标注时切面默认 module=类#方法、 action=方法名。 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogRecord {

  /** 操作描述（如「确认错题入库」）。 */
  String action() default "";
}
