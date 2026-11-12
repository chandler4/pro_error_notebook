package com.errnote.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.errnote.common.IdGenerator;
import com.errnote.dto.PageResult;
import com.errnote.log.entity.BusinessLog;
import com.errnote.log.mapper.BusinessLogMapper;
import com.errnote.log.service.BusinessLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 业务日志服务实现：入库失败仅 logback warn（TC-G-04）；永久保留无清理任务（BR-09）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLogServiceImpl implements BusinessLogService {

  private final BusinessLogMapper mapper;
  private final IdGenerator idGenerator;

  @Override
  public void save(BusinessLog businessLog) {
    try {
      if (businessLog.getCreatedAt() == null) {
        businessLog.setCreatedAt(LocalDateTime.now());
      }
      if (businessLog.getTraceId() == null) {
        businessLog.setTraceId(idGenerator.newId());
      }
      mapper.insert(businessLog);
    } catch (Exception e) {
      // TC-G-04：日志入库失败不影响业务主流程，仅系统运行日志告警
      log.warn("business_log persist failed: {}", e.getMessage());
    }
  }

  @Override
  public void recordAi(
      String model, long durationMs, String requestSummary, String rawModelResponse) {
    BusinessLog businessLog = new BusinessLog();
    businessLog.setLogType(BusinessLog.TYPE_AI_CALL);
    businessLog.setModule("OpenAiVisionAdapter#parse"); // api-contract 3.12 示例对齐
    businessLog.setAction("AI解析 model=" + model);
    businessLog.setRequestContent(requestSummary);
    businessLog.setResponseContent(rawModelResponse); // 模型返回原文全文（不得截断，TC-G-03）
    businessLog.setSuccess(true);
    businessLog.setErrorCode(0);
    businessLog.setDurationMs(durationMs);
    save(businessLog);
  }

  @Override
  public PageResult<BusinessLog> query(
      String logType,
      String module,
      Boolean success,
      String dateFrom,
      String dateTo,
      long page,
      long size) {
    LambdaQueryWrapper<BusinessLog> wrapper = new LambdaQueryWrapper<>();
    if (logType != null && !logType.isEmpty()) {
      wrapper.eq(BusinessLog::getLogType, logType);
    }
    if (module != null && !module.isEmpty()) {
      wrapper.like(BusinessLog::getModule, module);
    }
    if (success != null) {
      wrapper.eq(BusinessLog::getSuccess, success);
    }
    if (dateFrom != null && !dateFrom.isEmpty()) {
      wrapper.ge(BusinessLog::getCreatedAt, LocalDateTime.parse(dateFrom + "T00:00:00"));
    }
    if (dateTo != null && !dateTo.isEmpty()) {
      wrapper.le(BusinessLog::getCreatedAt, LocalDateTime.parse(dateTo + "T23:59:59"));
    }
    wrapper.orderByDesc(BusinessLog::getCreatedAt);

    long safeSize = Math.min(Math.max(size, 1), 50);
    long safePage = Math.max(page, 1);
    Page<BusinessLog> result = mapper.selectPage(new Page<>(safePage, safeSize), wrapper);
    return PageResult.of(result.getRecords(), result.getTotal(), safePage, safeSize);
  }
}
