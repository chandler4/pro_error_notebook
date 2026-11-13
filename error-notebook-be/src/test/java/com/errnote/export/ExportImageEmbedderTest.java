package com.errnote.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.errnote.integration.storage.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

/** W-9 导出图片 base64 内嵌（chandler 确认方案）：读图转 data URL；失败回退 null 不阻断导出。 */
@ExtendWith(MockitoExtension.class)
class ExportImageEmbedderTest {

  @Mock private FileStorageService fileStorageService;

  private ExportImageEmbedder build(FileStorageService storage) {
    return new ExportImageEmbedder(storage);
  }

  @Test
  @DisplayName("jpeg 图片读出并转 base64 data URL")
  void embedsJpegAsDataUrl() {
    ExportImageEmbedder e = build(fileStorageService);
    given(fileStorageService.read("images/202611/a1.jpg"))
        .willReturn(new ByteArrayResource(new byte[] {1, 2, 3}));

    assertThat(e.toDataUrl("images/202611/a1.jpg")).isEqualTo("data:image/jpeg;base64,AQID");
  }

  @Test
  @DisplayName("png 扩展名映射 image/png")
  void embedsPngMime() {
    ExportImageEmbedder e = build(fileStorageService);
    given(fileStorageService.read("images/202611/b.png"))
        .willReturn(new ByteArrayResource(new byte[] {1}));

    assertThat(e.toDataUrl("images/202611/b.png")).isEqualTo("data:image/png;base64,AQ==");
  }

  @Test
  @DisplayName("未知扩展名返回 null（不读存储）")
  void unknownExtensionReturnsNull() {
    ExportImageEmbedder e = build(fileStorageService);

    assertThat(e.toDataUrl("images/202611/c.gif")).isNull();
  }

  @Test
  @DisplayName("存储读取异常返回 null（回退 /files/ 不阻断导出）")
  void storageFailureReturnsNull() {
    ExportImageEmbedder e = build(fileStorageService);
    given(fileStorageService.read("images/202611/missing.jpg"))
        .willThrow(new IllegalStateException("file not found"));

    assertThat(e.toDataUrl("images/202611/missing.jpg")).isNull();
  }
}
