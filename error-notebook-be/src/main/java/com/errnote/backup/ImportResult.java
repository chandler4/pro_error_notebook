package com.errnote.backup;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 导入结果报告（api-contract 3.10）：{total, inserted, skipped, overwritten, errors:[{line, reason}]}。 */
@Data
public class ImportResult {

  private int total;
  private int inserted;
  private int skipped;
  private int overwritten;
  private List<RowError> errors = new ArrayList<>();

  /** 行级错误（line = JSON 数组内 1 基序号）。 */
  @Data
  public static class RowError {
    private final int line;
    private final String reason;

    public RowError(int line, String reason) {
      this.line = line;
      this.reason = reason;
    }
  }
}
