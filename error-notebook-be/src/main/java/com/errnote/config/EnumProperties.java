package com.errnote.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 枚举字典（architecture-data.md §3）：科目/年级/题型/分科 Tab 全部 yml 配置化，改配置重启生效。
 *
 * <p>subjectTabs 是前端展示分组，与 subjects 全集区分（规约场景：数学/物理/化学，可扩充）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "enums")
public class EnumProperties {

  private List<String> subjects = new ArrayList<>();

  private List<String> gradeLevels = new ArrayList<>();

  private List<String> questionTypes = new ArrayList<>();

  private List<String> subjectTabs = new ArrayList<>();
}
