package com.errnote.ai.prompt;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Prompt 模板配置（app.ai.prompt.*；模板与 few-shot 外置于 yml，占位符 {subjects} 注入学科枚举）。 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai.prompt")
public class PromptProperties {

  /** 系统指令模板：字段抽取规则 + 严格 JSON 输出约束；{subjects} 占位符运行时替换。 */
  private String system;

  /** few-shot 示例（规约物理填空题），拼接在 system 之后。 */
  private String fewshot;

  /** 学科枚举（architecture-data §3，与 BE /api/v1/meta/enums 同源）。 */
  private List<String> subjects = new ArrayList<>();
}
