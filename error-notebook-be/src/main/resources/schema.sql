-- 错题整理应用建表脚本（幂等：CREATE TABLE IF NOT EXISTS；architecture-data.md §2.2 唯一基线）
-- B-1 落地（2026-11-12）：wrong_question / knowledge_point / business_log 三表 + 索引
-- 变更规约：表结构变更 = 修改本文件 + 文件头注记（日期/变更人/内容）；存量迁移一次性脚本归档 db-changelog.md

CREATE TABLE IF NOT EXISTS wrong_question (
  error_question_id   CHAR(36)     NOT NULL,
  subject             VARCHAR(20)  NOT NULL,
  grade_level         VARCHAR(20),
  question_stem       TEXT         NOT NULL,
  question_images     TEXT         NOT NULL,   -- JSON: [{"url":"images/202611/xxx.jpg","role":"QUESTION"}]
  question_type       VARCHAR(20),
  source_name         VARCHAR(255),
  question_number     VARCHAR(20),
  standard_answer     TEXT,                    -- JSON: ["12","1:5"]
  knowledge_points    TEXT,                    -- JSON: ["电压表读数","串联电路分压规律"]
  tags                TEXT,                    -- JSON: ["二刷","易错"]（自由文本标签，BR-11）
  knowledge_analysis  TEXT,
  wrong_date          DATE,
  status              VARCHAR(16)  NOT NULL,   -- PENDING_CONFIRM / CONFIRMED
  created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (error_question_id)
);
CREATE INDEX IF NOT EXISTS IDX_WQ_SUBJECT_STATUS ON wrong_question (subject, status);
CREATE INDEX IF NOT EXISTS IDX_WQ_WRONG_DATE ON wrong_question (wrong_date);

CREATE TABLE IF NOT EXISTS knowledge_point (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  name       VARCHAR(100) NOT NULL,
  use_count  INT          NOT NULL DEFAULT 1,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_kp_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS business_log (          -- 规约5：切面日志持久化（2026-11-11）
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  trace_id         VARCHAR(36),
  log_type         VARCHAR(16)  NOT NULL,          -- OPERATION / AI_CALL
  module           VARCHAR(100),
  action           VARCHAR(100),
  request_content  TEXT,
  response_content TEXT,                           -- AI返回 / 业务返回全文
  success          TINYINT      NOT NULL,
  error_code       INT,
  duration_ms      BIGINT,
  created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS IDX_BL_TYPE_TIME ON business_log (log_type, created_at);
