package com.errnote.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.backup.BackupService;
import com.errnote.backup.ImportResult;
import com.errnote.backup.ImportResult.RowError;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 备份接口测试（3.10：文件流 + 幂等报告 + 43002 行级明细）。 */
@WebMvcTest(BackupController.class)
class BackupControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private BackupService backupService;

  @Test
  void exportStreamsJsonAttachment() throws Exception {
    given(backupService.exportAll()).willReturn("[]".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(get("/api/v1/backup/export"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString("filename=\"errnote-backup.json\"")));
  }

  @Test
  void importReturnsCountersOnSuccess() throws Exception {
    ImportResult result = new ImportResult();
    result.setTotal(3);
    result.setInserted(2);
    result.setSkipped(1);
    given(backupService.importData(any(byte[].class), eq("skip"))).willReturn(result);

    mockMvc
        .perform(
            multipart("/api/v1/backup/import")
                .file(
                    new MockMultipartFile(
                        "file",
                        "backup.json",
                        "application/json",
                        "[]".getBytes(StandardCharsets.UTF_8))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.total").value(3))
        .andExpect(jsonPath("$.data.inserted").value(2))
        .andExpect(jsonPath("$.data.skipped").value(1));
  }

  // TC-E-06：行级错误 → 43002 + data 携带明细
  @Test
  void importWithRowErrorsReturns43002WithDetails() throws Exception {
    ImportResult result = new ImportResult();
    result.setTotal(2);
    result.setInserted(1);
    result.setErrors(Arrays.asList(new RowError(2, "缺少 subject")));
    given(backupService.importData(any(byte[].class), eq("skip"))).willReturn(result);

    mockMvc
        .perform(
            multipart("/api/v1/backup/import")
                .file(
                    new MockMultipartFile(
                        "file",
                        "backup.json",
                        "application/json",
                        "[]".getBytes(StandardCharsets.UTF_8))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(43002))
        .andExpect(jsonPath("$.data.errors[0].line").value(2))
        .andExpect(jsonPath("$.data.errors[0].reason").value("缺少 subject"));
  }

  // 非法 mode → 40000
  @Test
  void invalidModeRejected() throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                multipart("/api/v1/backup/import")
                    .file(
                        new MockMultipartFile(
                            "file",
                            "b.json",
                            "application/json",
                            "[]".getBytes(StandardCharsets.UTF_8)))
                    .param("mode", "merge"))
            .andExpect(jsonPath("$.code").value(40000))
            .andReturn();
    assertThat(r).isNotNull();
  }
}
