package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.dto.PageResult;
import com.errnote.dto.QuestionDetail;
import com.errnote.dto.QuestionQuery;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 错题详情/确认/编辑/删除（3.3~3.7）+ 列表筛选（3.5）+ 草稿管理（3.8）。 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "question", description = "错题列表、详情、生命周期与草稿")
public class QuestionController {

  private final QuestionService questionService;

  @GetMapping({"", "/"})
  @Operation(summary = "分页筛选列表，仅 CONFIRMED（3.5）")
  public ApiResponse<PageResult<QuestionDetail>> list(QuestionQuery query) {
    return ApiResponse.ok(questionService.list(query));
  }

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

  @PostMapping("/drafts")
  @Operation(summary = "创建空草稿（转手动填写，零 AI 调用，3.8）")
  public ApiResponse<QuestionDetail> createDraft() {
    return ApiResponse.ok(QuestionDetail.from(questionService.createEmptyDraft()));
  }

  @GetMapping("/drafts")
  @Operation(summary = "待确认草稿列表，创建倒序（3.8）")
  public ApiResponse<PageResult<QuestionDetail>> listDrafts(
      @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
    return ApiResponse.ok(
        questionService.listDrafts(page == null ? 1 : page, size == null ? 10 : size));
  }

  @DeleteMapping("/drafts/{id}")
  @Operation(summary = "手动删除草稿，不删图片（3.8）")
  public ApiResponse<Void> deleteDraft(@PathVariable String id) {
    questionService.deleteDraft(id);
    return ApiResponse.ok(null);
  }
}
