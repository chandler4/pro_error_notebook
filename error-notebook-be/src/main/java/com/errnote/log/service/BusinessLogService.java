package com.errnote.log.service;

import com.errnote.dto.PageResult;
import com.errnote.log.entity.BusinessLog;

/** 业务日志服务（BR-09/10：切面日志持久化 + 运行日志查询）。 */
public interface BusinessLogService {

  /** 持久化一条日志；入库失败不抛出（TC-G-04：日志失败不影响业务）。 */
  void save(BusinessLog log);

  /** AI 调用记录（FR-C2/TC-G-03）：模型名、耗时、请求摘要、模型返回原文全文。 */
  void recordAi(String model, long durationMs, String requestSummary, String rawModelResponse);

  /**
   * AI 调用失败记录（2026-11-12 chandler 确认，W-1 修复）：success=false + 契约错误码 + 失败原因， 模型无返回故 raw 置空；供运行日志页「AI
   * 调用+失败」筛选追溯解析失败（原型 logs 失败态样例意图）。
   */
  void recordAiFailure(
      String model, long durationMs, String requestSummary, int errorCode, String errorMessage);

  /** 运行日志分页查询（FR-C3/TC-G-05）：类型/模块模糊/成败/时间区间。 */
  PageResult<BusinessLog> query(
      String logType,
      String module,
      Boolean success,
      String dateFrom,
      String dateTo,
      long page,
      long size);
}
