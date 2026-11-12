package com.errnote.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.errnote.dto.ImageRef;
import com.errnote.dto.QuestionDetail;
import com.errnote.service.QuestionService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 导出接口测试（TC-E-03：Content-Type / Content-Disposition / 文件流字节）。 */
@WebMvcTest(ExportController.class)
@Import({com.errnote.export.MarkdownExporter.class, com.errnote.export.PdfExporter.class})
class ExportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private QuestionService questionService;

  private static QuestionDetail sample() {
    return QuestionDetail.builder()
        .errorQuestionId("id-1")
        .subject("physics")
        .questionStem("串联电路实验题干")
        .questionImages(Arrays.asList(new ImageRef("images/202611/a1b2.jpg", "QUESTION")))
        .standardAnswer(Arrays.asList("12"))
        .knowledgePoints(Arrays.asList("电压表读数"))
        .tags(Collections.<String>emptyList())
        .wrongDate(LocalDate.of(2026, 11, 10))
        .status("CONFIRMED")
        .build();
  }

  @Test
  void markdownExportStreamsTextMarkdownAttachment() throws Exception {
    given(questionService.listAll(any())).willReturn(Collections.singletonList(sample()));

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/questions/export").param("format", "markdown"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string("Content-Type", org.hamcrest.Matchers.containsString("text/markdown")))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).contains("# 错题本");
    assertThat(body).contains("串联电路实验题干");
  }

  @Test
  void pdfExportStreamsApplicationPdfWithMagicBytes() throws Exception {
    given(questionService.listAll(any())).willReturn(Collections.singletonList(sample()));

    MvcResult result =
        mockMvc
            .perform(get("/api/v1/questions/export").param("format", "pdf").param("rework", "true"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        "Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename=\"errnote.pdf\"")))
            .andReturn();

    byte[] body = result.getResponse().getContentAsByteArray();
    assertThat(new String(body, 0, 4, java.nio.charset.StandardCharsets.US_ASCII))
        .isEqualTo("%PDF");
  }

  // 未知 format 归并 markdown（缺省行为一致）
  @Test
  void unknownFormatFallsBackToMarkdown() throws Exception {
    given(questionService.listAll(any())).willReturn(Collections.singletonList(sample()));

    mockMvc
        .perform(get("/api/v1/questions/export").param("format", "docx"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Type", org.hamcrest.Matchers.containsString("text/markdown")));
  }
}
