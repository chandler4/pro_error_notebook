package com.errnote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 错题整理应用 · 后端入口。
 *
 * <p>仅本机监听 127.0.0.1:8080（security.md §2）；单用户免登录；AI 能力经内部契约调用 error-notebook-ai (8081)，本工程不直连模型（架构
 * v3.0）。
 */
@SpringBootApplication
@EnableFeignClients
public class NotebookApplication {

  public static void main(String[] args) {
    SpringApplication.run(NotebookApplication.class, args);
  }
}
