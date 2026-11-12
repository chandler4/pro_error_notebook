package com.errnote.service;

import com.errnote.entity.KnowledgePoint;
import java.util.List;

/** 知识点字典服务（BR-03：自由录入 + 保存归并累积；BR-11 语义独立于标签）。 */
public interface KnowledgeService {

  /** 保存时归并累积（TC-C-03）：新名入字典 use_count=1；已存在 use_count+1；同名去重不重复计。 */
  void accumulate(List<String> knowledgePoints);

  /** 错题删除时减计（architecture-data §5：use_count 随保存/删除增减，下限 0，不删字典行）。 */
  void decrement(List<String> knowledgePoints);

  /** 归并候选（TC-C-04）：keyword 包含匹配 + use_count 降序 + limit；空 keyword 返回热度前 limit。 */
  List<String> candidates(String keyword, int limit);

  /** 归并候选实体（B-10 / 契约 3.11：meta.knowledge-points 返回 name + useCount）。 */
  List<KnowledgePoint> candidateEntities(String keyword, int limit);
}
