package com.errnote.ai.prompt;

/** 系统提示词供给口（A-4 模板化实现；防腐层只依赖本接口——依赖倒置）。 */
@FunctionalInterface
public interface PromptProvider {

  String systemPrompt();
}
