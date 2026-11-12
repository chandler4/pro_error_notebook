package com.errnote.log.controller;

import com.errnote.common.ApiResponse;
import com.errnote.dto.PageResult;
import com.errnote.log.entity.BusinessLog;
import com.errnote.log.service.BusinessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 运行日志查询（api-contract 3.12，「运行日志」页数据源；本接口自身不产生业务日志防递归）。 */
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Tag(name = "logs", description = "业务日志查询（业务操作/AI 调用）")
public class LogQueryController {

  private final BusinessLogService businessLogService;

  @GetMapping
  @Operation(summary = "分页筛选查询业务日志")
  public ApiResponse<PageResult<BusinessLog>> query(
      @RequestParam(required = false) String logType,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) Boolean success,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "10") long size) {
    return ApiResponse.ok(
        businessLogService.query(logType, module, success, dateFrom, dateTo, page, size));
  }
}
