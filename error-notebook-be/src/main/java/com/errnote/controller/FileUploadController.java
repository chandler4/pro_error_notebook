package com.errnote.controller;

import com.errnote.common.ApiResponse;
import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.integration.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 图片上传（api-contract 3.1：POST /api/v1/questions/parse-image，multipart files 多文件）。 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "upload", description = "错题图片上传")
public class FileUploadController {

  static final int MAX_COUNT = 6;

  private final FileStorageService fileStorageService;

  @PostMapping("/parse-image")
  @Operation(summary = "上传处理图（前端已压缩+裁剪），返回相对路径数组")
  public ApiResponse<List<String>> upload(
      @RequestParam(value = "files", required = false) List<MultipartFile> files)
      throws IOException {
    if (files == null || files.isEmpty() || files.size() > MAX_COUNT) {
      // TC-U-06：单次 >6 张 → 41003
      throw new BusinessException(ErrorCode.IMAGE_TOO_MANY);
    }
    List<String> paths = new ArrayList<>();
    for (MultipartFile file : files) {
      try (InputStream in = file.getInputStream()) {
        paths.add(fileStorageService.save(in, file.getOriginalFilename()));
      }
    }
    return ApiResponse.ok(paths);
  }
}
