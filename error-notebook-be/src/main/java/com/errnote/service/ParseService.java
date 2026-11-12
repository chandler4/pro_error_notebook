package com.errnote.service;

import com.errnote.dto.ImageRef;
import com.errnote.entity.WrongQuestion;
import java.util.List;

/**
 * 解析编排（api-contract 3.2）：调内部 parse（单次，重试在 AI 服务防腐层）→ 草稿入库 PENDING_CONFIRM（BR-02）→ recordAi
 * 留痕（TC-G-03）。
 */
public interface ParseService {

  /** @return 已入库的草稿实体（status=PENDING_CONFIRM，含 errorQuestionId） */
  WrongQuestion parse(List<ImageRef> images);
}
