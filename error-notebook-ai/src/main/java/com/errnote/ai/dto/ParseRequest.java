package com.errnote.ai.dto;

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
public class ParseRequest {

  @NotEmpty @Valid private List<ImageRef> images;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImageRef {

    @NotEmpty private String url;

    /** QUESTION / ANSWER_SHEET（BR-06）。 */
    @NotEmpty private String role;
  }
}
