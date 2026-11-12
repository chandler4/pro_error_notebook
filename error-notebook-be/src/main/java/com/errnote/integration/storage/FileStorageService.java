package com.errnote.integration.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

/** 文件存储抽象（架构 §5.4：当前本地目录实现，可替换 MinIO，业务层零改动）。 */
public interface FileStorageService {

  /** 存储（含魔数/大小校验），返回相对路径 images/yyyyMM/{uuid}.jpg。 */
  String save(InputStream in, String originalFilename);

  /** 相对路径 → 访问 URL（/files/images/...）。 */
  String getUrl(String relativePath);

  /** 读取（导出用）；路径穿越防护（TC-S-01）。 */
  Resource read(String relativePath);

  /** 删除（随错题硬删联动，BR-04）；非法路径或删除失败返回 false 不抛出。 */
  boolean delete(String relativePath);
}
