package com.errnote.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 统一分页响应（api-contract 第 1 章：data: {list, total, page, size}）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

  private List<T> list;
  private long total;
  private long page;
  private long size;

  public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
    return new PageResult<>(list, total, page, size);
  }
}
