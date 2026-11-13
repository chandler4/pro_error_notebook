package com.errnote.export;

import com.errnote.integration.storage.FileStorageService;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * 导出图片 base64 内嵌（W-9，2026-11-13 chandler 确认）：读存储图片转 data URL——导出的 Markdown/PDF
 * 单文件离线自包含，脱离服务仍显示原题图片。
 *
 * <p>失败兜底：文件缺失/读取异常/未知扩展名返回 null，导出回退 /files/ 服务相对路径（不阻断导出主流程）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportImageEmbedder {

  private final FileStorageService fileStorageService;

  /** @return data:image/xxx;base64,...；无法内嵌时 null（调用方回退） */
  public String toDataUrl(String relativeUrl) {
    String mime = mimeOf(relativeUrl);
    if (mime == null) {
      log.warn("export image skip embed (unsupported extension): {}", relativeUrl);
      return null;
    }
    try {
      Resource resource = fileStorageService.read(relativeUrl);
      byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
      return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      log.warn("export image fallback to /files/ ({}): {}", e.getMessage(), relativeUrl);
      return null;
    }
  }

  private static String mimeOf(String url) {
    String lower = url == null ? "" : url.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    return null;
  }
}
