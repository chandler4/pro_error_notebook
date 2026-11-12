package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.dto.ParseRequest;
import com.errnote.dto.QuestionDetail;
import com.errnote.entity.WrongQuestion;
import com.errnote.service.ParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 解析（api-contract 3.2：POST /api/v1/questions/parse，同步等待）。 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "parse", description = "AI 解析与草稿入库")
public class QuestionParseController {

  private final ParseService parseService;

  @PostMapping("/parse")
  @Operation(summary = "多图解析，草稿 PENDING_CONFIRM 入库并返回全字段")
  public ApiResponse<QuestionDetail> parse(@Valid @RequestBody ParseRequest request) {
    WrongQuestion draft = parseService.parse(request.getImages());
    return ApiResponse.ok(QuestionDetail.from(draft));
  }
}
