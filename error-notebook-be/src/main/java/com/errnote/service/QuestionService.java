package com.errnote.service;

import com.errnote.dto.PageResult;
import com.errnote.dto.QuestionDetail;
import com.errnote.dto.QuestionQuery;
import com.errnote.dto.QuestionSaveRequest;
import com.errnote.entity.WrongQuestion;
import java.util.List;

/** 错题详情/确认/编辑/删除/列表/草稿管理（api-contract 3.3~3.8）。 */
public interface QuestionService {

  /** 详情（错题/草稿共用，3.4）；不存在抛 40400。 */
  WrongQuestion get(String id);

  /** 确认入库（3.3）：必填校验（控制器层 40000）→ 转 CONFIRMED → 知识点归并（BR-03）。 */
  WrongQuestion confirm(String id, QuestionSaveRequest request);

  /** 全字段编辑（3.6）：更新 + 知识点归并；状态不变。 */
  WrongQuestion update(String id, QuestionSaveRequest request);

  /** 硬删除（3.7）：删记录 + 联动删图（BR-04）+ 知识点减计；不存在抛 40400。 */
  void delete(String id);

  /** 分页筛选（3.5）：仅 CONFIRMED；错题日期倒序、同日创建倒序；size 上限 50。 */
  PageResult<QuestionDetail> list(QuestionQuery query);

  /** 导出用全量列表（FR-B4）：同 3.5 筛选语义，内部按 50/页翻页取全（仅 CONFIRMED）。 */
  List<QuestionDetail> listAll(QuestionQuery query);

  /** 创建空草稿（3.8 / FR-A4 转手动填写）：零 AI 调用（TC-P-07），PENDING_CONFIRM。 */
  WrongQuestion createEmptyDraft();

  /** 草稿列表（3.8）：仅 PENDING_CONFIRM，创建倒序。 */
  PageResult<QuestionDetail> listDrafts(int page, int size);

  /** 手动删除草稿（3.8）：不删图片；非草稿或不存在抛 40400。 */
  void deleteDraft(String id);
}
