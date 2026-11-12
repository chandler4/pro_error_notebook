package com.errnote.dto;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 多图引用（规约：questionImages: [{url, role}]，role=QUESTION / ANSWER_SHEET，BR-06）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageRef {

  /** 相对路径（images/202611/{uuid}.jpg）。 */
  @NotBlank private String url;

  /** QUESTION / ANSWER_SHEET。 */
  @NotBlank private String role;
}
