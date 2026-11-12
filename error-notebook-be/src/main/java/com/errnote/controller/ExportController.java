package com.errnote.controller;

import com.errnote.dto.QuestionDetail;
import com.errnote.dto.QuestionQuery;
import com.errnote.export.MarkdownExporter;
import com.errnote.export.PdfExporter;
import com.errnote.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 导出（api-contract 3.9）：format=markdown|pdf + rework（PDF 重做模式）+ 3.5 筛选参数。 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "export", description = "Markdown / PDF 导出")
public class ExportController {

  private final QuestionService questionService;
  private final MarkdownExporter markdownExporter;
  private final PdfExporter pdfExporter;

  @GetMapping("/export")
  @Operation(summary = "按筛选条件导出 Markdown 或 PDF 文件流")
  public ResponseEntity<byte[]> export(
      @RequestParam(defaultValue = "markdown") String format,
      @RequestParam(defaultValue = "false") boolean rework,
      QuestionQuery query)
      throws java.io.UnsupportedEncodingException {
    List<QuestionDetail> questions = questionService.listAll(query);
    MarkdownExporter.Mode mode = rework ? MarkdownExporter.Mode.REWORK : MarkdownExporter.Mode.FULL;
    String md = markdownExporter.render(questions, mode);
    byte[] content;
    String filename;
    MediaType contentType;
    if ("pdf".equalsIgnoreCase(format)) {
      content = pdfExporter.renderPdf(md);
      filename = "errnote.pdf";
      contentType = MediaType.APPLICATION_PDF;
    } else {
      content = md.getBytes(StandardCharsets.UTF_8);
      filename = "errnote.md";
      contentType = new MediaType("text", "markdown", StandardCharsets.UTF_8);
    }
    // TC-E-03：附件流；filename ASCII 兜底 + RFC 5987 UTF-8 扩展名
    String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replace("+", "%20");
    return ResponseEntity.ok()
        .contentType(contentType)
        .header(
            "Content-Disposition",
            "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
        .body(content);
  }
}
