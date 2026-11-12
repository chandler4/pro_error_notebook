package com.errnote.integration.storage;

import com.errnote.common.BusinessException;
import com.errnote.common.ErrorCode;
import com.errnote.common.IdGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * 本地目录存储（{data.dir}/images/{yyyyMM}/{uuid}.jpg，架构 §5.4）。
 *
 * <p>安全（security.md §3）：魔数校验（不信任扩展名/Content-Type）、UUID 命名（原始文件名不落盘）、 单张 10MB、相对路径规范化校验防穿越（TC-S-01）。
 */
@Slf4j
@Service
public final class LocalFileStorageService implements FileStorageService {

  static final long MAX_BYTES = 10L * 1024 * 1024;
  private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyyMM");

  private final Path baseDir;
  private final Path imagesBaseDir;
  private final IdGenerator idGenerator;
  private final Clock clock;

  public LocalFileStorageService(
      @Value("${data.dir}") String dataDir, IdGenerator idGenerator, Clock clock)
      throws IOException {
    this.baseDir = Paths.get(dataDir).toAbsolutePath().normalize();
    this.imagesBaseDir = baseDir.resolve("images");
    this.idGenerator = idGenerator;
    this.clock = clock;
    Files.createDirectories(imagesBaseDir);
  }

  @Override
  public String save(InputStream in, String originalFilename) {
    PushbackInputStream pushback = new PushbackInputStream(in, 8);
    String extension = extensionByMagicNumber(pushback);
    String month = MONTH_DIR.format(LocalDate.now(clock));
    String relativePath = "images/" + month + "/" + idGenerator.newId() + extension;
    Path target = resolveSafely(relativePath);
    Path parent = target.getParent();
    if (parent == null) {
      // unreachable：relativePath 恒含两层目录；防御式兜底（SpotBugs NP）
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
    }
    try {
      Files.createDirectories(parent);
      Files.copy(pushback, target, StandardCopyOption.REPLACE_EXISTING);
      if (Files.size(target) > MAX_BYTES) { // TC-U-06：大小上限（魔数已读后流式落盘再校验）
        Files.deleteIfExists(target);
        throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
      }
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
    }
    return relativePath;
  }

  @Override
  public String getUrl(String relativePath) {
    return "/files/" + relativePath;
  }

  @Override
  public Resource read(String relativePath) {
    Path path = resolveSafely(relativePath);
    if (!Files.exists(path)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在: " + relativePath);
    }
    return new FileSystemResource(path);
  }

  @Override
  public boolean delete(String relativePath) {
    try {
      Path path = resolveSafely(relativePath);
      return Files.deleteIfExists(path);
    } catch (Exception e) {
      // 数据生命周期 §5：删图失败记日志人工核查，不影响删除事务
      log.warn("image delete failed: {} ({})", relativePath, e.getMessage());
      return false;
    }
  }

  /** 规范化校验（TC-S-01）：normalize 后必须仍位于 images 目录内，拒绝 ../ 类输入。 */
  private Path resolveSafely(String relativePath) {
    if (relativePath == null || relativePath.isEmpty()) {
      throw new BusinessException(ErrorCode.PARAM_INVALID, "非法文件路径");
    }
    Path resolved = baseDir.resolve(relativePath).normalize();
    if (!resolved.startsWith(imagesBaseDir)) {
      throw new BusinessException(ErrorCode.PARAM_INVALID, "非法文件路径: " + relativePath);
    }
    return resolved;
  }

  /** 魔数判定扩展名（TC-U-05）：FFD8FF=jpg、89504E47=png；其余（含改扩展名的文本）拒绝 41001。读过的字节回推，落盘保留完整内容。 */
  private String extensionByMagicNumber(PushbackInputStream in) {
    byte[] head = new byte[8];
    int read;
    try {
      read = in.read(head);
      if (read > 0) {
        in.unread(head, 0, read);
      }
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.IMAGE_FORMAT_UNSUPPORTED);
    }
    if (read >= 3
        && (head[0] & 0xFF) == 0xFF
        && (head[1] & 0xFF) == 0xD8
        && (head[2] & 0xFF) == 0xFF) {
      return ".jpg";
    }
    if (read >= 4
        && (head[0] & 0xFF) == 0x89
        && head[1] == 0x50
        && head[2] == 0x4E
        && head[3] == 0x47) {
      return ".png";
    }
    throw new BusinessException(ErrorCode.IMAGE_FORMAT_UNSUPPORTED);
  }
}
