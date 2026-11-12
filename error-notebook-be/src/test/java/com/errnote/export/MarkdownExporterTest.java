package com.errnote.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.errnote.common.JsonUtil;
import com.errnote.dto.QuestionDetail;
import com.errnote.export.MarkdownExporter.Mode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** Markdown 模板测试（⭐TDD：TC-E-01 golden 比对 / TC-E-02 重做模式 / TC-D-02 多图兼容）。 */
class MarkdownExporterTest {

  private final MarkdownExporter exporter = new MarkdownExporter();

  private static QuestionDetail sample() throws Exception {
    try (InputStream in =
        MarkdownExporterTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/notebook-sample.json")) {
      return JsonUtil.convert(
          new String(readAll(in), StandardCharsets.UTF_8),
          new com.fasterxml.jackson.core.type.TypeReference<QuestionDetail>() {});
    }
  }

  private static byte[] readAll(InputStream in) throws Exception {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int n;
    while ((n = in.read(buffer)) != -1) {
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  private static String golden(String name) throws Exception {
    try (InputStream in = MarkdownExporterTest.class.getClassLoader().getResourceAsStream(name)) {
      return new String(readAll(in), StandardCharsets.UTF_8);
    }
  }

  // TC-E-01：与 golden 文件逐字一致（规约 4 示例夹具）
  @Test
  void fullModeMatchesGoldenFile() throws Exception {
    String md = exporter.render(Collections.singletonList(sample()), Mode.FULL);

    assertThat(md).isEqualTo(golden("fixtures/notebook-sample.md"));
  }

  // TC-E-02：重做模式正文无答案/解析；答案页置尾且完整
  @Test
  void reworkModeMovesAnswersToTailPage() throws Exception {
    String md = exporter.render(Collections.singletonList(sample()), Mode.REWORK);

    int answerPage = md.indexOf("# 答案页");
    assertThat(answerPage).isGreaterThan(0);
    String body = md.substring(0, answerPage);
    assertThat(body).doesNotContain("1:5"); // 正文不泄漏答案
    assertThat(body).doesNotContain("### 答案");
    assertThat(body).doesNotContain("### 解析");
    assertThat(body).doesNotContain("偏转角相同时"); // 解析内容不在正文

    String tail = md.substring(answerPage);
    assertThat(tail).contains("12");
    assertThat(tail).contains("1:5");
    assertThat(tail).contains("偏转角相同时"); // 解析在答案页
  }

  // TC-D-02：多图兼容——题目图与解答图全部输出，URL 带 /files/ 前缀
  @Test
  void multiImageExportIncludesAllRoles() throws Exception {
    String md = exporter.render(Collections.singletonList(sample()), Mode.FULL);

    assertThat(md).contains("![QUESTION](/files/images/202611/a1b2c3d4.jpg)");
    assertThat(md).contains("![ANSWER_SHEET](/files/images/202611/e5f6a7b8.jpg)");
  }

  // 空结果导出提示（边界）
  @Test
  void emptyListRendersPlaceholder() {
    String md = exporter.render(Collections.<QuestionDetail>emptyList(), Mode.FULL);

    assertThat(md).contains("无错题记录");
  }

  // 空字段安全（null stem/无图草稿化数据）
  @Test
  void nullFieldsRenderDashesWithoutNpe() {
    QuestionDetail q = QuestionDetail.builder().build();
    String md = exporter.render(Collections.singletonList(q), Mode.FULL);

    assertThat(md).contains("（无题干）");
    assertThat(md).contains("科目：-");
  }
}
