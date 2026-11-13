package com.errnote.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** PDF 生成冒烟（TC-E-02 渲染链路；视觉人工确认归 T9-3）。 */
class PdfExporterTest {

  private final PdfExporter exporter = new PdfExporter();

  @Test
  void rendersPdfMagicBytesWithCjkContent() {
    String md = "# 错题本\n\n## 1. 串联电路实验\n\n中文内容渲染链路。\n\n### 答案\n\n1. 12\n";

    byte[] pdf = exporter.renderPdf(md);

    assertThat(pdf).isNotNull().isNotEmpty();
    assertThat(pdf.length).isGreaterThan(4);
    assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
  }

  @Test
  void emptyMarkdownStillProducesValidPdf() {
    byte[] pdf = exporter.renderPdf("# 错题本\n\n（当前筛选条件下无错题记录）\n");

    assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
  }

  // W-9：MD 内嵌 base64 data URL 图片（1x1 PNG）——openhtmltopdf 渲染不报错仍出有效 PDF
  @Test
  void dataUrlImageRendersIntoPdf() {
    String png1x1 =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
    String md = "# 错题本\n\n![QUESTION](data:image/png;base64," + png1x1 + ")\n";

    byte[] pdf = exporter.renderPdf(md);

    assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
  }
}
