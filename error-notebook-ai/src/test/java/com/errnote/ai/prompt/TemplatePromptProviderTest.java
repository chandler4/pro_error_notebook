package com.errnote.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A-4 Prompt 模板外置（TDD）。
 *
 * <p>规约草稿字段全集 + 「仅输出严格 JSON」指令 + {subjects} 枚举替换（api-contract §7.1、architecture-data §3、BR-11）。
 */
@SpringBootTest
class TemplatePromptProviderTest {

  @Autowired private PromptProvider promptProvider;

  @Test
  @DisplayName("占位符替换：{subjects} 换为枚举列表，system 与 few-shot 拼接")
  void substitutesSubjectsVariableAndJoinsFewshot() {
    PromptProperties properties = new PromptProperties();
    properties.setSystem("学科枚举取值 {subjects}。仅输出严格 JSON。");
    properties.setFewshot("示例：{\"subject\":\"physics\"}");
    properties.setSubjects(Arrays.asList("chinese", "math", "physics"));

    String prompt = new TemplatePromptProvider(properties).systemPrompt();

    assertThat(prompt)
        .isEqualTo("学科枚举取值 chinese, math, physics。仅输出严格 JSON。\n\n示例：{\"subject\":\"physics\"}");
  }

  @Test
  @DisplayName("yml 模板覆盖规约草稿全字段并含严格 JSON 指令")
  void yamlTemplateCoversAllSpecFields() {
    String prompt = promptProvider.systemPrompt();

    assertThat(prompt)
        .contains("subject")
        .contains("gradeLevel")
        .contains("questionType")
        .contains("questionStem")
        .contains("sourceName")
        .contains("questionNumber")
        .contains("standardAnswer")
        .contains("knowledgePoints")
        .contains("knowledgeAnalysis")
        .contains("tags") // BR-11：标签人工标注，模型不得生成
        .contains("仅输出严格 JSON")
        // 2026-11-13 chandler 确认（推翻当日晨间「维持提取定位」）：图中优先、无则 AI 解题生成
        .contains("自行解题")
        .contains("不得留空");
  }

  @Test
  @DisplayName("yml 学科枚举注入：占位符已替换、无残留")
  void subjectsEnumInjectedFromYaml() {
    String prompt = promptProvider.systemPrompt();

    assertThat(prompt).contains("chinese, math, english, physics, chemistry, biology");
    assertThat(prompt).doesNotContain("{subjects}");
  }

  @Test
  @DisplayName("few-shot 物理示例在 system 指令之后拼入")
  void fewshotPhysicsExampleAppendedAfterSystemDirective() {
    String prompt = promptProvider.systemPrompt();

    assertThat(prompt.indexOf("仅输出严格 JSON"))
        .isLessThan(prompt.indexOf("\"questionType\":\"fillBlank\""));
    assertThat(prompt).contains("\"standardAnswer\":[\"12\",\"1:5\"]");
  }
}
