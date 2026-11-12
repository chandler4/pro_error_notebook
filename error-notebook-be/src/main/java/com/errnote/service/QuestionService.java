package com.errnote.service;

import com.errnote.dto.QuestionSaveRequest;
import com.errnote.entity.WrongQuestion;

/** 错题详情/确认/编辑/删除（api-contract 3.3~3.7）。 */
public interface QuestionService {

  /** 详情（错题/草稿共用，3.4）；不存在抛 40400。 */
  WrongQuestion get(String id);

  /** 确认入库（3.3）：必填校验（控制器层 40000）→ 转 CONFIRMED → 知识点归并（BR-03）。 */
  WrongQuestion confirm(String id, QuestionSaveRequest request);

  /** 全字段编辑（3.6）：更新 + 知识点归并；状态不变。 */
  WrongQuestion update(String id, QuestionSaveRequest request);

  /** 硬删除（3.7）：删记录 + 联动删图（BR-04）+ 知识点减计；不存在抛 40400。 */
  void delete(String id);
}
