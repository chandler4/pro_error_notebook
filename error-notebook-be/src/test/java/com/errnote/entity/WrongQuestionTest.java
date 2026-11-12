package com.errnote.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.errnote.dto.ImageRef;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 实体 JSON 列映射 round-trip（TC-D-01：表字段与规约 JSON 一一映射）。 */
class WrongQuestionTest {

  // TC-D-01 规约4：多图/答案/知识点/标签集合字段 round-trip（标准夹具子集）
  @Test
  void jsonColumnsRoundTripWithDomainAccessors() {
    WrongQuestion question = new WrongQuestion();
    List<ImageRef> images =
        Arrays.asList(
            new ImageRef("images/202611/a1b2.jpg", "QUESTION"),
            new ImageRef("images/202611/c3d4.jpg", "ANSWER_SHEET"));
    question.setImages(images);
    question.setAnswers(Arrays.asList("12", "1:5"));
    question.setPoints(Arrays.asList("电压表读数", "串联电路分压规律"));
    question.setTagList(Arrays.asList("二刷", "易错"));

    assertThat(question.getQuestionImages()).contains("a1b2").contains("ANSWER_SHEET");
    assertThat(question.images()).containsExactlyElementsOf(images);
    assertThat(question.answers()).containsExactly("12", "1:5");
    assertThat(question.points()).containsExactly("电压表读数", "串联电路分压规律");
    assertThat(question.tagList()).containsExactly("二刷", "易错");
  }

  // TC-D-01：null/空 JSON 列的领域访问器为空集合（草稿空字段安全）
  @Test
  void emptyColumnsYieldEmptyCollections() {
    WrongQuestion question = new WrongQuestion();
    assertThat(question.images()).isEmpty();
    assertThat(question.answers()).isEmpty();
    assertThat(question.points()).isEmpty();
    assertThat(question.tagList()).isEmpty();
  }

  // TC-D-02 前置：questionImage 兼容字段取首个 QUESTION 角色图
  @Test
  void firstQuestionImageIsDerivable() {
    WrongQuestion question = new WrongQuestion();
    question.setImages(
        Arrays.asList(
            new ImageRef("images/202611/answer.jpg", "ANSWER_SHEET"),
            new ImageRef("images/202611/question.jpg", "QUESTION")));

    String questionImage =
        question.images().stream()
            .filter(img -> "QUESTION".equals(img.getRole()))
            .map(ImageRef::getUrl)
            .findFirst()
            .orElse(null);

    assertThat(questionImage).isEqualTo("images/202611/question.jpg");
  }
}
