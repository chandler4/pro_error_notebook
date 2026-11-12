package com.errnote.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** JsonUtil 单测：JSON TEXT 列与领域集合转换（B-1 DoD；实体 JSON 字段的唯一通道）。 */
class JsonUtilTest {

  // TC-D-01 前置：集合字段序列化/反序列化 round-trip
  @Test
  void listRoundTripPreservesElements() {
    List<String> tags = Arrays.asList("二刷", "易错", "期中考");
    String json = JsonUtil.toJson(tags);
    assertThat(JsonUtil.listFromJson(json, String.class)).containsExactlyElementsOf(tags);
  }

  @Test
  void nullAndEmptyInputsAreSafe() {
    assertThat(JsonUtil.toJson(null)).isNull();
    assertThat(JsonUtil.fromJson(null, String.class)).isNull();
    assertThat(JsonUtil.fromJson("", String.class)).isNull();
    assertThat(JsonUtil.listFromJson(null, String.class)).isEmpty();
    assertThat(JsonUtil.listFromJson("", String.class)).isEmpty();
  }

  @Test
  void objectRoundTripWithUnknownTolerance() {
    Sample src = new Sample();
    src.setName("物理");
    Sample out = JsonUtil.fromJson(JsonUtil.toJson(src), Sample.class);
    assertThat(out.getName()).isEqualTo("物理");
    // 未知字段容忍（向前兼容：导入旧导出文件不炸）
    Sample extra = JsonUtil.fromJson("{\"name\":\"a\",\"futureField\":1}", Sample.class);
    assertThat(extra.getName()).isEqualTo("a");
  }

  @Test
  void malformedJsonThrowsIllegalState() {
    assertThatThrownBy(() -> JsonUtil.fromJson("{bad", Sample.class))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> JsonUtil.listFromJson("[{bad]", Sample.class))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void typeReferenceConvertWorks() {
    List<String> out = JsonUtil.convert("[\"x\",\"y\"]", new TypeReference<List<String>>() {});
    assertThat(out).containsExactly("x", "y");
  }

  static class Sample {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
