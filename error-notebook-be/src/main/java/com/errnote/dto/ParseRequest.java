package com.errnote.dto;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/** 解析请求体（api-contract 3.2）：images 多图 + 角色。 */
@Data
public class ParseRequest {

  @NotEmpty @Valid private List<ImageRef> images;
}
