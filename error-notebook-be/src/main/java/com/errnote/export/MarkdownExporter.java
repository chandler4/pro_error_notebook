package com.errnote.export;

import com.errnote.dto.ImageRef;
import com.errnote.dto.QuestionDetail;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Markdown 导出模板（FR-B4 / TC-E-01 golden 基线）。
 *
 * <p>重做模式（TC-E-02）：正文仅题目与元信息，答案与解析整体移至文末「答案页」。
 */
@Component
public class MarkdownExporter {

  /** 导出模式：FULL 常规（答案随题）；REWORK 重做（答案页置尾）。 */
  public enum Mode {
    FULL,
    REWORK
  }

  public String render(List<QuestionDetail> questions, Mode mode) {
    StringBuilder md = new StringBuilder();
    md.append("# 错题本\n\n");
    if (questions.isEmpty()) {
      md.append("（当前筛选条件下无错题记录）\n");
      return md.toString();
    }
    int index = 1;
    for (QuestionDetail q : questions) {
      renderQuestion(md, index++, q, mode == Mode.REWORK);
      md.append("\n---\n\n");
    }
    if (mode == Mode.REWORK) {
      md.append("# 答案页\n\n");
      int answerIndex = 1;
      for (QuestionDetail q : questions) {
        renderAnswerSection(md, answerIndex++, q);
      }
    }
    return md.toString();
  }

  private void renderQuestion(StringBuilder md, int index, QuestionDetail q, boolean rework) {
    md.append("## ")
        .append(index)
        .append(". ")
        .append(firstLine(q.getQuestionStem()))
        .append("\n\n");
    md.append("- 科目：").append(orDash(q.getSubject()));
    md.append(" ｜ 年级：").append(orDash(q.getGradeLevel()));
    md.append(" ｜ 题型：").append(orDash(q.getQuestionType())).append("\n");
    md.append("- 来源：").append(orDash(q.getSourceName()));
    md.append(" ｜ 题号：").append(orDash(q.getQuestionNumber())).append("\n");
    md.append("- 错题日期：")
        .append(q.getWrongDate() == null ? "-" : q.getWrongDate().toString())
        .append("\n");
    md.append("- 知识点：").append(join(q.getKnowledgePoints())).append("\n");
    md.append("- 标签：").append(join(q.getTags())).append("\n\n");
    if (q.getQuestionImages() != null && !q.getQuestionImages().isEmpty()) {
      for (ImageRef image : q.getQuestionImages()) {
        md.append("![")
            .append(image.getRole())
            .append("](")
            .append("/files/")
            .append(image.getUrl())
            .append(")\n");
      }
      md.append("\n");
    }
    md.append("### 题目\n\n").append(orEmpty(q.getQuestionStem())).append("\n\n");
    if (!rework) {
      renderAnswerSection(md, index, q);
    }
  }

  private void renderAnswerSection(StringBuilder md, int index, QuestionDetail q) {
    md.append("### 答案\n\n");
    List<String> answers = q.getStandardAnswer();
    if (answers == null || answers.isEmpty()) {
      md.append("-\n\n");
    } else {
      for (int i = 0; i < answers.size(); i++) {
        md.append(i + 1).append(". ").append(answers.get(i)).append("\n");
      }
      md.append("\n");
    }
    md.append("### 解析\n\n").append(orEmpty(q.getKnowledgeAnalysis())).append("\n\n");
  }

  private static String firstLine(String text) {
    if (text == null || text.isEmpty()) {
      return "（无题干）";
    }
    int cut = text.indexOf('\n');
    return cut > 0 ? text.substring(0, cut) : text;
  }

  private static String join(List<String> values) {
    if (values == null || values.isEmpty()) {
      return "-";
    }
    StringBuilder sb = new StringBuilder();
    for (String v : values) {
      if (sb.length() > 0) {
        sb.append("、");
      }
      sb.append(v);
    }
    return sb.toString();
  }

  private static String orDash(String value) {
    return value == null || value.isEmpty() ? "-" : value;
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }
}
