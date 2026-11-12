package com.errnote.ai.prompt;

import org.springframework.stereotype.Component;

/** 模板化系统提示词：{subjects} 占位符替换 + few-shot 拼接（A-4，替代 A-3 占位实现）。 */
@Component
public class TemplatePromptProvider implements PromptProvider {

  static final String SUBJECTS_PLACEHOLDER = "{subjects}";

  private final PromptProperties properties;

  public TemplatePromptProvider(PromptProperties properties) {
    this.properties = properties;
  }

  @Override
  public String systemPrompt() {
    String system =
        properties
            .getSystem()
            .replace(SUBJECTS_PLACEHOLDER, String.join(", ", properties.getSubjects()));
    return system + "\n\n" + properties.getFewshot();
  }
}
