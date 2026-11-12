package com.errnote.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.springframework.stereotype.Component;

/**
 * PDF 导出（FR-B4）：Markdown → commonmark HTML → jsoup 规整 XHTML → openhtmltopdf。
 *
 * <p>中文字体：优先系统 TTF（黑体/等线/仿宋），未找到时降级内置字体并告警（视觉人工确认项，T9-3）。
 */
@Slf4j
@Component
public class PdfExporter {

  private static final String[] CJK_FONTS = {
    "C:/Windows/Fonts/simhei.ttf",
    "C:/Windows/Fonts/deng.ttf",
    "C:/Windows/Fonts/simfang.ttf",
    "C:/Windows/Fonts/simsun.ttf"
  };

  private final Parser parser = Parser.builder().build();
  private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

  public byte[] renderPdf(String markdown) {
    try {
      Node node = parser.parse(markdown);
      String body = htmlRenderer.render(node);
      String html =
          "<html><head><style>"
              + "body{font-family:'cjk',sans-serif;font-size:12pt;line-height:1.6;}"
              + "img{max-width:100%;}"
              + "h1{font-size:18pt;border-bottom:1pt solid #333;}"
              + "h2{font-size:14pt;margin-top:16pt;}"
              + "h3{font-size:12pt;}"
              + "</style></head><body>"
              + body
              + "</body></html>";

      Document jdoc = Jsoup.parse(html);
      jdoc.outputSettings(
          new Document.OutputSettings()
              .syntax(Document.OutputSettings.Syntax.xml)
              .escapeMode(Entities.EscapeMode.xhtml)
              .charset(StandardCharsets.UTF_8));

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      File cjk = findCjkFont();
      if (cjk != null) {
        builder.useFont(cjk, "cjk");
      } else {
        log.warn("no CJK ttf font found in C:/Windows/Fonts; PDF chinese text may be blank");
      }
      builder.withHtmlContent(jdoc.outerHtml(), null);
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("PDF export failed", e);
    }
  }

  private static File findCjkFont() {
    for (String path : CJK_FONTS) {
      File f = new File(path);
      if (f.exists()) {
        return f;
      }
    }
    return null;
  }
}
