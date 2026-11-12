package com.errnote.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.errnote.common.BusinessException;
import com.errnote.common.JsonUtil;
import com.errnote.entity.WrongQuestion;
import com.errnote.mapper.WrongQuestionMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 备份迁移集成测试（H2 内存库，⭐TDD）：TC-E-04 round-trip / TC-E-05 幂等 skip+overwrite / TC-E-06 行级错误与 43001 /
 * TC-S-04 20MB 上限。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BackupServiceIntegrationTest {

  @Autowired private BackupService backupService;
  @Autowired private WrongQuestionMapper wrongQuestionMapper;

  private static byte[] fixture() throws Exception {
    try (InputStream in =
        BackupServiceIntegrationTest.class
            .getClassLoader()
            .getResourceAsStream("fixtures/backup-set.json")) {
      java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int n;
      while ((n = in.read(buffer)) != -1) {
        out.write(buffer, 0, n);
      }
      return out.toByteArray();
    }
  }

  // TC-E-04：夹具导入 5 条 → 导出 → 清库 → 再导入 → 数据等价（SQL 语义验证）
  @Test
  void exportImportRoundTripKeepsDataEquivalent() throws Exception {
    ImportResult first = backupService.importData(fixture(), "skip");
    assertThat(first.getInserted()).isEqualTo(5);
    assertThat(first.getErrors()).isEmpty();

    byte[] exported = backupService.exportAll();
    for (WrongQuestion q : wrongQuestionMapper.selectList(null)) {
      wrongQuestionMapper.deleteById(q.getErrorQuestionId());
    }
    assertThat(wrongQuestionMapper.selectCount(null)).isZero();

    ImportResult second = backupService.importData(exported, "skip");
    assertThat(second.getInserted()).isEqualTo(5);

    List<WrongQuestion> restored = wrongQuestionMapper.selectList(null);
    assertThat(restored).hasSize(5);
    WrongQuestion bk1 = wrongQuestionMapper.selectById("bk-1");
    assertThat(bk1.getSubject()).isEqualTo("physics");
    assertThat(bk1.images()).hasSize(2); // 多图扩展保留
    assertThat(bk1.images().get(1).getRole()).isEqualTo("ANSWER_SHEET");
    assertThat(bk1.answers()).containsExactly("12", "1:5"); // 多答案
    assertThat(bk1.points()).containsExactly("电压表读数", "串联电路分压规律");
    assertThat(bk1.tagList()).containsExactly("二刷", "易错");
    assertThat(bk1.getWrongDate().toString()).isEqualTo("2026-11-10");
    // 单图兼容：bk-2 仅 questionImage → 导入归一为 QUESTION 角色图
    assertThat(wrongQuestionMapper.selectById("bk-2").images()).hasSize(1);
  }

  // TC-E-05：skip 幂等（重复导入全部跳过）；overwrite 重复导入全部覆盖且内容不变
  @Test
  void importIsIdempotentByErrorQuestionId() throws Exception {
    backupService.importData(fixture(), "skip");
    ImportResult again = backupService.importData(fixture(), "skip");
    assertThat(again.getInserted()).isZero();
    assertThat(again.getSkipped()).isEqualTo(5);

    ImportResult overwrite = backupService.importData(fixture(), "overwrite");
    assertThat(overwrite.getOverwritten()).isEqualTo(5);
    assertThat(overwrite.getInserted()).isZero();
    assertThat(wrongQuestionMapper.selectById("bk-3").getQuestionStem()).contains("表达效果");
  }

  // TC-E-06：非 JSON → 43001；行级校验失败 → 行号+原因明细（有效行仍入库）
  @Test
  void rejectsInvalidFileAndReportsRowLevelErrors() throws Exception {
    assertThatThrownBy(
            () -> backupService.importData("not-json{".getBytes(StandardCharsets.UTF_8), "skip"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(43001);

    String mixed =
        "[{\"errorQuestionId\":\"ok-1\",\"subject\":\"math\",\"questionStem\":\"有效行\"},"
            + "{\"subject\":\"math\",\"questionStem\":\"缺 id\"},"
            + "{\"errorQuestionId\":\"ok-2\",\"questionStem\":\"缺 subject\"},"
            + "{\"errorQuestionId\":\"ok-3\",\"subject\":\"math\"}]";
    ImportResult result = backupService.importData(mixed.getBytes(StandardCharsets.UTF_8), "skip");

    assertThat(result.getTotal()).isEqualTo(4);
    assertThat(result.getInserted()).isEqualTo(1); // 有效行不中断
    assertThat(result.getErrors()).hasSize(3);
    assertThat(result.getErrors().get(0).getLine()).isEqualTo(2);
    assertThat(result.getErrors().get(0).getReason()).contains("errorQuestionId");
    assertThat(result.getErrors().get(1).getLine()).isEqualTo(3);
    assertThat(result.getErrors().get(1).getReason()).contains("subject");
    assertThat(result.getErrors().get(2).getLine()).isEqualTo(4);
    assertThat(result.getErrors().get(2).getReason()).contains("questionStem");
  }

  // TC-S-04：>20MB 拒绝（43001 文件级）
  @Test
  void rejectsOversizedImport() {
    byte[] big = new byte[BackupService.MAX_IMPORT_BYTES + 1];
    big[0] = '[';

    assertThatThrownBy(() -> backupService.importData(big, "skip"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(43001);
  }

  // 空文件 → 43001
  @Test
  void rejectsEmptyContent() {
    assertThatThrownBy(() -> backupService.importData(new byte[0], "skip"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(43001);
  }

  // 导出含 questionImage 兼容字段（TC-D-02：首个 QUESTION 角色图）
  @Test
  void exportCarriesCompatQuestionImageField() throws Exception {
    backupService.importData(fixture(), "skip");

    String exported = new String(backupService.exportAll(), StandardCharsets.UTF_8);
    List<BackupItem> items =
        JsonUtil.convert(
            exported, new com.fasterxml.jackson.core.type.TypeReference<List<BackupItem>>() {});
    assertThat(items).hasSize(5);
    for (BackupItem item : items) {
      if (!item.getQuestionImages().isEmpty()) {
        assertThat(item.getQuestionImage()).isEqualTo(item.getQuestionImages().get(0).getUrl());
      }
    }
  }
}
