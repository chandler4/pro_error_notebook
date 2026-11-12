package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.dto.QuestionDetail;
import com.errnote.dto.QuestionSaveRequest;
import com.errnote.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 错题详情/确认/编辑/删除（api-contract 3.3~3.7）。 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "question", description = "错题详情与生命周期")
public class QuestionController {

  private final QuestionService questionService;

  @GetMapping("/{id}")
  @Operation(summary = "错题/草稿详情（3.4）")
  public ApiResponse<QuestionDetail> get(@PathVariable String id) {
    return ApiResponse.ok(QuestionDetail.from(questionService.get(id)));
  }

  @PostMapping("/{id}/confirm")
  @Operation(summary = "确认入库：转 CONFIRMED + 知识点归并（3.3）")
  public ApiResponse<QuestionDetail> confirm(
      @PathVariable String id, @Valid @RequestBody QuestionSaveRequest request) {
    return ApiResponse.ok(QuestionDetail.from(questionService.confirm(id, request)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "全字段编辑 + 知识点归并（3.6）")
  public ApiResponse<QuestionDetail> update(
      @PathVariable String id, @Valid @RequestBody QuestionSaveRequest request) {
    return ApiResponse.ok(QuestionDetail.from(questionService.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "硬删除 + 联动删图（3.7）")
  public ApiResponse<Void> delete(@PathVariable String id) {
    questionService.delete(id);
    return ApiResponse.ok(null);
  }
}
