package com.errnote.controller;

import com.errnote.backup.BackupService;
import com.errnote.backup.ImportResult;
import com.errnote.common.ApiResponse;
import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 备份迁移（api-contract 3.10）：全量 JSON 导出 + 幂等导入（skip/overwrite）。 */
@RestController
@RequestMapping("/api/v1/backup")
@RequiredArgsConstructor
@Tag(name = "backup", description = "备份导出与导入")
public class BackupController {

  private final BackupService backupService;

  @GetMapping("/export")
  @Operation(summary = "全量 CONFIRMED 数据导出规约 JSON 数组文件")
  public ResponseEntity<byte[]> export() throws java.io.UnsupportedEncodingException {
    byte[] content = backupService.exportAll();
    String filename = "errnote-backup.json";
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(
            "Content-Disposition",
            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
        .body(content);
  }

  @PostMapping("/import")
  @Operation(summary = "导入 JSON 备份：mode=skip|overwrite，按 errorQuestionId 幂等")
  public ApiResponse<ImportResult> importBackup(
      @RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "skip") String mode)
      throws java.io.IOException {
    if (!"skip".equals(mode) && !"overwrite".equals(mode)) {
      throw new BusinessException(ErrorCode.PARAM_INVALID, "mode 仅支持 skip / overwrite");
    }
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.IMPORT_FORMAT_ERROR, "导入文件为空");
    }
    ImportResult result = backupService.importData(file.getBytes(), mode);
    if (!result.getErrors().isEmpty()) { // TC-E-06：43002 携带行级明细
      return new ApiResponse<>(
          ErrorCode.IMPORT_DATA_INVALID.getCode(),
          "导入数据校验失败：" + result.getErrors().size() + " 行错误",
          result);
    }
    return ApiResponse.ok(result);
  }
}
