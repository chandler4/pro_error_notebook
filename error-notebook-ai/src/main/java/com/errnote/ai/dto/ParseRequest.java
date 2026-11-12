package com.errnote.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 内部契约请求（api-contract §7.1）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "错题解析请求（BE 传入多图与角色）")
public class ParseRequest {

  @NotEmpty
  @Valid
  @Schema(description = "图片列表（至少 1 张）", required = true)
  private List<ImageRef> images;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "图片引用：相对路径 + 角色")
  public static class ImageRef {

    @NotEmpty
    @Schema(description = "BE 文件存储相对路径，如 images/202611/a1b2.jpg", required = true)
    private String url;

    /** QUESTION / ANSWER_SHEET（BR-06）。 */
    @NotEmpty
    @Schema(
        description = "图片角色：QUESTION=题目图 / ANSWER_SHEET=解答图",
        allowableValues = {"QUESTION", "ANSWER_SHEET"},
        required = true)
    private String role;
  }
}
