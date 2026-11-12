package com.errnote.common;

/** ID 生成接口：测试可注入固定值替身（test-strategy §2 可测试性原则 4）。 */
public interface IdGenerator {

  String newId();
}
