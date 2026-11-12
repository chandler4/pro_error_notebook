# 错题整理应用 · 接口契约

| 项 | 内容 |
|---|---|
| 文档版本 | v1.4 |
| 日期 | 2026-11-12 |
| 作者 | chandler（决策评审）、AI 助手（起草） |
| 状态 | ✅ 已评审通过（评审人：chandler，2026-11-11） |
| 关联文档 | [architecture.md](architecture.md)、[architecture-data.md](architecture-data.md)、[security.md](security.md)、[communication-log.md](communication-log.md) |

**本层确认决策（2026-11-11 15:45，chandler）**：统一响应采用业务码包装 `{code, message, data}`；路径带 `/api/v1` 版本前缀；接口文档采用 springdoc-openapi + Swagger UI（新依赖，评审通过）；解析草稿永久保留+手动删除（数据架构遗留项同步关闭）。

**v1.1 扩展（2026-11-11 16:13，chandler）**：新增 3.12 业务日志查询接口（规约 5 切面日志，「运行日志」页数据源）。

**v1.2 扩展（2026-11-11 17:05，chandler）**：标签（FR-B6/B7）——详情/列表响应增加 `tags` 字段；confirm/PUT 请求接受 `tags`（自由文本数组，AI 解析草稿恒为空数组，人工填写）；列表查询增加 `tag` 筛选参数；备份导出含 `tags` 扩展字段。

**v1.3 扩展（2026-11-11 17:24，chandler）**：架构变更 v3.0（AI 独立服务）——新增**第 7 章内部契约**（BE↔AI 业务语义接口）。

**v1.4 扩展（2026-11-12，chandler）**：review 补缺——① 3.8 新增 `POST /api/v1/questions/drafts` 创建空草稿（FR-A4「转手动填写」入口，零 AI 调用，关闭 TC-P-07 契约缺口）；② 3.3 补编辑模式重新解析语义（确认成功后前端联动删除旧记录）。

---

## 1. 通用规范

| 项 | 规范 |
|---|---|
| Base Path | `/api/v1` |
| 协议 | HTTP/1.1（本机部署，JSON over UTF-8） |
| 统一响应 | `{code, message, data}`；**HTTP 状态恒 200**，业务结果由 code 表达 |
| 字段命名 | JSON camelCase，与规约 JSON 字段一致 |
| 时间格式 | `yyyy-MM-dd`（日期）/ ISO-8601（时间戳） |
| 分页参数 | `page`（1 起）、`size`（默认 10，最大 50） |
| 分页响应 | `data: {list, total, page, size}` |
| 上传限制 | 单次 ≤ 6 张；单张 ≤ 10MB；类型 jpg/png（服务端按魔数校验） |
| 长请求 | `/questions/parse` 同步等待，前端超时 120s |

**统一响应示例**

```json
// 成功
{ "code": 0, "message": "OK", "data": { ... } }
// 失败
{ "code": 42001, "message": "AI 解析失败：模型返回超时（已重试 1 次）", "data": null }
```

## 2. 错误码表

| code | 含义 | 场景 |
|---|---|---|
| 0 | 成功 | — |
| 40000 | 参数校验失败 | message 含字段级明细 |
| 40400 | 资源不存在 | 错题/草稿 id 无效 |
| 41001 | 图片格式不支持 | 非 jpg/png（魔数校验） |
| 41002 | 图片超过大小限制 | >10MB |
| 41003 | 图片数量超限 | 单次 >6 张 |
| 42001 | AI 解析失败 | 模型输出无效/超时，重试后仍失败 |
| 42002 | AI 服务不可达 | 网络错误/配置缺失 |
| 43001 | 导入文件格式错误 | 非 JSON / 结构不符 |
| 43002 | 导入数据校验失败 | data 携带行级错误明细 |
| 50000 | 系统内部错误 | 未预期异常（日志留痕） |

## 3. 端点契约

### 3.1 POST `/api/v1/questions/parse-image` — 上传处理图

- 请求：`multipart/form-data`，字段 `files`（多文件，前端已压缩+裁剪）
- 响应：`data: ["images/202611/a1b2....jpg", "images/202611/c3d4....jpg"]`（相对路径）
- 错误：41001 / 41002 / 41003

### 3.2 POST `/api/v1/questions/parse` — AI 解析（同步）

- 请求：

```json
{ "images": [ { "url": "images/202611/a1b2.jpg", "role": "QUESTION" },
              { "url": "images/202611/c3d4.jpg", "role": "ANSWER_SHEET" } ] }
```

- 行为：调用 AI → 校验 → **草稿入库（PENDING_CONFIRM，生成 errorQuestionId）** → 返回草稿
- 响应：`data` = 错题草稿全字段（结构同 3.4 详情，`status: "PENDING_CONFIRM"`）
- 错误：42001（可重试或转手动填写）/ 42002 / 40000

### 3.3 POST `/api/v1/questions/{id}/confirm` — 确认入库

- 请求：确认后的**完整草稿字段**（人工修正结果，必填：subject、questionStem）
- 行为：校验 → 状态转 `CONFIRMED` → 知识点归并累积（BR-03）
- **编辑模式重新解析语义（2026-11-12 chandler 确认）**：换图重新解析经 3.2 生成**新草稿/新 errorQuestionId**，confirm 新记录成功后由**前端联动 DELETE 原 CONFIRMED 记录**（3.7），保持编辑语义不产生重复
- 响应：`data` = 确认后的错题
- 错误：40400（草稿不存在）/ 40000

### 3.4 GET `/api/v1/questions/{id}` — 错题/草稿详情

- 响应 data（与规约 JSON 对齐 + 管理字段）：

```json
{
  "errorQuestionId": "3f8a2c1e-6b7d-4e29-9c1f-8d2e7a4b5c60",
  "subject": "physics", "gradeLevel": "grade9", "questionType": "fillBlank",
  "questionStem": "在探究\"串联电路电压规律\"的实验中……",
  "questionImages": [ { "url": "images/202611/a1b2.jpg", "role": "QUESTION" } ],
  "sourceName": "2026—2027 学年度上学期九年级物理学科素养调研",
  "questionNumber": "14",
  "standardAnswer": ["12", "1:5"],
  "knowledgePoints": ["电压表读数", "串联电路分压规律"],
  "tags": ["二刷", "易错"],
  "knowledgeAnalysis": "**步骤1** …… $U = U_1 + U_2 = 12\\,\\mathrm{V}$ ……",
  "wrongDate": "2026-11-10",
  "status": "CONFIRMED", "createdAt": "...", "updatedAt": "..."
}
```

### 3.5 GET `/api/v1/questions` — 分页筛选（仅 CONFIRMED）

- 查询参数：`subject`、`keyword`（题干模糊）、`knowledgePoint`、`tag`（标签模糊，FR-B7）、`questionType`、`dateFrom`、`dateTo`、`page`、`size`
- 响应：`data: { list: [详情结构…], total, page, size }`

### 3.6 PUT `/api/v1/questions/{id}` — 编辑（全字段）

- 请求：完整字段（同 confirm）；行为：更新 + 知识点归并累积
- 错误：40400 / 40000

### 3.7 DELETE `/api/v1/questions/{id}` — 硬删除

- 行为：删除记录 + **联动删除图片文件**（BR-04），二次确认由前端交互承担
- 错误：40400

### 3.8 草稿管理

| 方法路径 | 说明 |
|---|---|
| POST `/api/v1/questions/drafts` | **创建空草稿**（FR-A4「转手动填写」，2026-11-12 确认）：零 AI 调用（TC-P-07），生成 PENDING_CONFIRM 空草稿（`questionImages: []`、`tags: []`），返回 `data` = 空草稿全字段（结构同 3.4，含 errorQuestionId），后续 confirm/待确认区/续编复用现有契约 |
| GET `/api/v1/questions/drafts?page=&size=` | 待确认草稿列表（PENDING_CONFIRM，按创建倒序） |
| DELETE `/api/v1/questions/drafts/{id}` | 手动删除草稿（**永久保留+手动删**，2026-11-11 15:45 确认；草稿删除不删图片，图片随下一次同 URL 复用或人工清理） |

### 3.9 GET `/api/v1/questions/export` — 导出

- 查询参数：`format=markdown|pdf`、`rework=true|false`（PDF 重做模式）+ 3.5 全部筛选参数
- 响应：文件流（`Content-Type: text/markdown` / `application/pdf`，`Content-Disposition` 附件）

### 3.10 备份迁移

| 方法路径 | 说明 |
|---|---|
| GET `/api/v1/backup/export` | 全量 CONFIRMED 数据 → 规约 JSON 数组文件（含 questionImage 兼容字段与 questionImages 扩展） |
| POST `/api/v1/backup/import?mode=skip\|overwrite` | multipart JSON 文件；按 errorQuestionId 幂等：skip 跳过已存在 / overwrite 覆盖更新；data 返回 `{total, inserted, skipped, overwritten, errors:[{line, reason}]}` |

### 3.11 元数据

| 方法路径 | 说明 |
|---|---|
| GET `/api/v1/meta/enums` | `{subjects, gradeLevels, questionTypes, subjectTabs}`（yml 配置化） |
| GET `/api/v1/meta/knowledge-points?keyword=&limit=10` | 归并候选（字典表，use_count 降序，keyword 前缀/包含匹配） |

### 3.12 GET `/api/v1/logs` — 业务日志查询（「运行日志」页数据源，2026-11-11 追加）

- 查询参数：`logType=OPERATION|AI_CALL`、`module`（模糊）、`success=true|false`、`dateFrom`、`dateTo`、`page`、`size`
- 响应 data：

```json
{ "list": [ { "id": 1, "traceId": "…", "logType": "AI_CALL", "module": "OpenAiVisionAdapter#parse",
    "action": "AI解析 model=glm-4v", "requestContent": "…请求摘要…",
    "responseContent": "…AI 返回原文全文…", "success": true, "errorCode": 0,
    "durationMs": 18342, "createdAt": "2026-11-11T16:30:00" } ],
  "total": 1, "page": 1, "size": 10 }
```

- 说明：`responseContent` 为入库原文（AI 返回与业务返回全文，质量查看记录）；**本接口自身不产生业务日志**（切面跳过日志查询控制器，防递归）。

## 4. 接口文档（springdoc-openapi + Swagger UI）

- 依赖：`springdoc-openapi-ui 1.7.x`（Spring Boot 2.7 兼容线；**新引入依赖，2026-11-11 15:45 评审通过**）。
- UI：`/swagger-ui.html`；OpenAPI JSON：`/v3/api-docs`。
- 生产开关：`springdoc.api-docs.enabled=false` 可关闭（默认开发开启，部署配置见 security.md 的暴露面章节）。
- 契约一致性：代码注解（`@Operation/@Schema`）为准，本文档为契约基线；端点/字段变更需评审并同步更新本文档（记入沟通记录）。

## 5. 前端 request 封装契约（fetch）

```ts
request<T>(method, url, { json?, form?, timeout? = 30000 }): Promise<T>
```
- 统一解析 `{code, message, data}`；`code !== 0` 抛 `ApiError(code, message)` 由调用方/全局 toast 处理。
- `parse` 等长请求调用处覆写 `timeout = 120000`。
- 文件上传用 `FormData`；下载（导出/备份）走 `fetch` blob + `<a download>` 触发保存。

## 6. 变更管理

- 本契约为前后端并行开发的唯一接口基线；任何端点、字段、错误码变更需：提出 → 交互确认（沟通记录留痕）→ 同步本文档与 Swagger 注解 → 前后端联调验证。

## 7. 内部契约：BE ↔ AI 服务（v1.3 追加，2026-11-11 17:24）

> 仅供 `error-notebook-be` ↔ `error-notebook-ai` 内部调用；AI 服务仅监听 `127.0.0.1:8081`（回环，无鉴权，security.md 边界内）；不对外暴露、不进入前端调用。

### 7.1 POST `/internal/parse` — 错题解析（业务语义接口）

- 请求：

```json
{ "images": [ { "url": "images/202611/a1b2.jpg", "role": "QUESTION" },
              { "url": "images/202611/c3d4.jpg", "role": "ANSWER_SHEET" } ] }
```

- 响应 data：

```json
{
  "draft": { "subject": "physics", "gradeLevel": "grade9", "questionType": "fillBlank",
             "questionStem": "…", "sourceName": "…", "questionNumber": "14",
             "standardAnswer": ["12", "1:5"], "knowledgePoints": ["…"], "knowledgeAnalysis": "…",
             "tags": [] },
  "rawModelResponse": "{…模型返回原文全文…}",
  "model": "glm-4v",
  "durationMs": 18342
}
```

- 错误码：42001（输出非法，已重试 1 次）/ 42002（模型不可达/超时）；错误响应同统一结构 `{code, message, data}`。
- 约定：`draft.tags` 恒为空数组（BR-11：标签纯手工标注）；`rawModelResponse` 供 BE 落 `business_log`（BR-09/TC-G-03），AI 服务自身**不持久化**任何数据；图片由 BE 侧文件存储提供访问（AI 服务按 URL 拉取或由 BE 传内容，实现取其一并在 A-2 任务固化——默认 AI 服务回环直接读 BE 静态 `/files/**`）。
- 变更：内部契约变更同样走第 6 章流程，并同步 [architecture.md](architecture.md) §5.2。
