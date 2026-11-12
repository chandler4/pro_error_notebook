package com.errnote.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** JSON 工具：实体 JSON TEXT 列与领域集合的统一转换（architecture-data §2.2 映射）。 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtil {

  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  /** 序列化；null 安全（返回 null 交由调用方处理可空列）。 */
  public static String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("JSON serialize failed", e);
    }
  }

  /** 反序列化；空串/null 返回 null。 */
  public static <T> T fromJson(String json, Class<T> type) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalStateException("JSON deserialize failed: " + type.getSimpleName(), e);
    }
  }

  /** 反序列化集合；空串/null 返回空列表（业务侧免判空）。 */
  public static <T> List<T> listFromJson(String json, Class<T> elementType) {
    if (json == null || json.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return MAPPER.readValue(
          json, MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
    } catch (Exception e) {
      throw new IllegalStateException(
          "JSON deserialize list failed: " + elementType.getSimpleName(), e);
    }
  }

  /** 供需要复杂类型时复用同一 ObjectMapper（保持配置一致）。 */
  public static <T> T convert(String json, TypeReference<T> typeRef) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.readValue(json, typeRef);
    } catch (Exception e) {
      throw new IllegalStateException("JSON deserialize failed", e);
    }
  }
}
