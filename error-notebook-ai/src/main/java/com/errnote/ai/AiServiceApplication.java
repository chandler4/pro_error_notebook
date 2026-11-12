package com.errnote.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 错题整理应用 · AI 解析服务入口。
 *
 * <p>仅回环监听 127.0.0.1:8081、无鉴权、无数据库、无状态（api-contract 第 7 章）；职责＝模型调用 + Prompt + 防腐层，raw 原文随响应回传 BE 落
 * business_log；AI 服务自身不持久化任何数据。
 */
@SpringBootApplication
@EnableFeignClients
public class AiServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AiServiceApplication.class, args);
  }
}
