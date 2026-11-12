package com.errnote.integration.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.errnote.common.BusinessException;
import com.errnote.common.IdGenerator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 本地存储单测（⭐测试先行：TC-U-05 魔数 / TC-U-06 大小 / TC-U-07 命名 / TC-S-01 穿越防护）。 */
class LocalFileStorageServiceTest {

  @TempDir Path tempDir;

  private LocalFileStorageService service;
  private Path secretOutsideImages;

  private static final byte[] JPG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
  private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  @BeforeEach
  void setUp() throws IOException {
    // 固定时间 2026-11-12 → 目录 yyyyMM=202611；固定 ID 断言文件名（TC-U-07）
    Clock fixed = Clock.fixed(Instant.parse("2026-11-12T00:00:00Z"), ZoneOffset.UTC);
    IdGenerator fixedId = () -> "fixed-uuid";
    service = new LocalFileStorageService(tempDir.toString(), fixedId, fixed);
    secretOutsideImages = tempDir.resolve("secret.txt");
    Files.write(secretOutsideImages, "top secret".getBytes());
  }

  private static InputStream jpgOfSize(int size) {
    byte[] data = new byte[size];
    System.arraycopy(JPG_MAGIC, 0, data, 0, JPG_MAGIC.length);
    return new ByteArrayInputStream(data);
  }

  // TC-U-05 规约1：jpg/png 魔数通过（不信任扩展名）
  @Test
  void magicNumberJpgAndPngAccepted() throws IOException {
    String jpg = service.save(jpgOfSize(100), "photo.JPG");
    String png = service.save(new ByteArrayInputStream(PNG_MAGIC), "photo.png");

    assertThat(jpg).isEqualTo("images/202611/fixed-uuid.jpg");
    assertThat(png).isEqualTo("images/202611/fixed-uuid.jpg".replace(".jpg", ".png"));
    assertThat(Files.exists(tempDir.resolve("images/202611"))).isTrue();
  }

  // TC-U-05 规约1：改扩展名的文本文件 → 41001
  @Test
  void textFileWithJpgExtensionRejected() {
    InputStream fake = new ByteArrayInputStream("this is plain text, not an image".getBytes());

    assertThatThrownBy(() -> service.save(fake, "fake.jpg"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(41001);
  }

  // TC-U-06 规约1：单张 >10MB → 41002
  @Test
  void oversizedImageRejected() throws IOException {
    InputStream big = jpgOfSize((int) LocalFileStorageService.MAX_BYTES + 1);

    assertThatThrownBy(() -> service.save(big, "big.jpg"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(41002);
    // 超限文件不残留
    assertThat(listSavedFiles()).isEmpty();
  }

  // TC-U-07 规约1：服务端 UUID 命名，原始文件名不落盘
  @Test
  void savedFileNameIsServerGeneratedUuid() throws IOException {
    service.save(jpgOfSize(50), "我的错题截图 2026期末.JPG");

    for (Path file : listSavedFiles()) {
      String name = file.getFileName().toString();
      assertThat(name).startsWith("fixed-uuid");
      assertThat(name).doesNotContain("我的错题");
    }
  }

  // TC-S-01 安全：路径穿越输入被拒绝（读取抛 40000、删除返回 false，界外文件未动）
  @Test
  void pathTraversalIsRejected() {
    assertThatThrownBy(() -> service.read("images/../../secret.txt"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo(40000);

    assertThat(service.delete("../secret.txt")).isFalse();
    assertThat(service.delete("images/../../secret.txt")).isFalse();
    assertThat(Files.exists(secretOutsideImages)).isTrue(); // 界外文件未被删除
  }

  @Test
  void deleteRemovesStoredImageAndUrlMapsCorrectly() throws IOException {
    String path = service.save(jpgOfSize(50), "a.jpg");

    assertThat(service.getUrl(path)).isEqualTo("/files/" + path);
    assertThat(service.delete(path)).isTrue();
    assertThat(Files.exists(tempDir.resolve(path))).isFalse();
    assertThat(service.delete(path)).isFalse(); // 幂等
  }

  private Iterable<Path> listSavedFiles() throws IOException {
    Path imagesDir = tempDir.resolve("images");
    if (!Files.exists(imagesDir)) {
      return java.util.Collections.emptyList();
    }
    return Files.newDirectoryStream(imagesDir.resolve("202611"), p -> !Files.isDirectory(p));
  }

  @SuppressWarnings("unused")
  private static byte[] padded(byte[] head, int size) {
    byte[] data = new byte[size];
    System.arraycopy(head, 0, data, 0, Math.min(head.length, size));
    Arrays.fill(data, head.length, size, (byte) 0x11);
    return data;
  }

  @SuppressWarnings("unused")
  private static InputStream streamOf(byte[] data) {
    return new ByteArrayInputStream(data);
  }

  @SuppressWarnings("unused")
  private static Path pathOf(Path base, String relative) {
    return Paths.get(base.toString(), relative);
  }
}
