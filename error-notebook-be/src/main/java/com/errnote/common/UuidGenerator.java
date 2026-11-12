package com.errnote.common;

import java.util.UUID;
import org.springframework.stereotype.Component;

/** UUID 生成实现：errorQuestionId（CHAR(36)）由确认/草稿入库时生成。 */
@Component
public class UuidGenerator implements IdGenerator {

  @Override
  public String newId() {
    return UUID.randomUUID().toString();
  }
}
