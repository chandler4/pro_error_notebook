package com.errnote.ai.prompt;

import org.springframework.stereotype.Component;

/** 默认系统提示词（A-3 占位：内置严格 JSON 指令；A-4 替换为 yml 外置模板渲染）。 */
@Component
public class DefaultPromptProvider implements PromptProvider {

  @Override
  public String systemPrompt() {
    return "你是错题识别助手。仅输出严格 JSON（无解释、无 markdown 围栏），字段结构与示例见后续配置模板。";
  }
}
