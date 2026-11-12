package com.errnote.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.integration.storage.FileStorageService;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

/** 上传接口测试（TC-U-05/06 接口层：错误码 41001/41002/41003 与相对路径响应）。 */
@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private FileStorageService fileStorageService;

  private static MockMultipartFile jpgFile(String name) {
    return new MockMultipartFile(
        "files", name, "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
  }

  @Test
  void uploadWithinLimitReturnsRelativePaths() throws Exception {
    given(fileStorageService.save(Mockito.any(InputStream.class), Mockito.any()))
        .willReturn("images/202611/a1b2.jpg");

    mockMvc
        .perform(multipart("/api/v1/questions/parse-image").file(jpgFile("q1.jpg")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data[0]").value("images/202611/a1b2.jpg"));
  }

  // TC-U-06：单次 >6 张 → 41003
  @Test
  void moreThanSixFilesRejected() throws Exception {
    MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/questions/parse-image");
    for (int i = 0; i < 7; i++) {
      request.file(jpgFile("f" + i + ".jpg"));
    }

    mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(41003));
  }

  // TC-U-06：空上传 → 41003
  @Test
  void emptyUploadRejected() throws Exception {
    mockMvc
        .perform(multipart("/api/v1/questions/parse-image"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(41003));
  }

  // TC-U-05：魔数失败透传 41001（service 抛 → 统一响应）
  @Test
  void formatErrorFromStoragePropagates() throws Exception {
    given(fileStorageService.save(Mockito.any(InputStream.class), Mockito.any()))
        .willThrow(new BusinessException(ErrorCode.IMAGE_FORMAT_UNSUPPORTED));

    mockMvc
        .perform(multipart("/api/v1/questions/parse-image").file(jpgFile("fake.jpg")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(41001));
  }
}
