# claude_standard.md · 项目标准技术栈与规约模板

| 项 | 内容 |
|---|---|
| 版本 | v1.1（2026-11-11 同步至错题项目全部现行决策） |
| 来源 | 错题整理应用架构评审确认结果（评审人：chandler，记录见 [spec/communication-log.md](spec/communication-log.md)） |
| 用途 | **后续新项目的初始基线模板**：复制至项目根目录按实际调整；调整/新增项按第 4 章评审规约重新确认后方可集成 |

---

## 1. 技术栈基线（已评审通过）

### 1.1 前端（Web）

| 技术 | 版本 | 用途 | 备注 |
|---|---|---|---|
| Vue | 3.x | 前端框架 | |
| Vite | 5.x | 开发/构建 | |
| Element Plus | 2.x | UI 组件库 | |
| Vue Router + Pinia | 4.x / 2.x | 路由/状态 | |
| 原生 fetch + request 轻封装 | 内置 | HTTP 调用 | **不引入 axios** |
| 原生 canvas 图片压缩 | 内置 | 上传前压缩 | 长边 ≤2000px |
| cropperjs | 1.x | 图片裁剪 | 按需 |
| markdown-it + DOMPurify + KaTeX | 最新稳定 | Markdown/LaTeX 渲染 + XSS 消毒 | v-html 仅限消毒后 HTML |

### 1.2 后端

| 技术 | 版本 | 用途 | 备注 |
|---|---|---|---|
| OpenJDK | **8** | 运行时基线 | Boot 3.x 需 JDK 17，不可用 |
| Spring Boot | **2.7.x** | 应用框架 | EOL 风险已知（仅本机监听缓解） |
| MyBatis-Plus | 3.5.x | 持久层 | 标准 SQL 方言，可迁移 MySQL/PG |
| **OpenFeign + 防腐层 ACL** | spring-cloud 2021.0.x | 外部 AI 服务声明式调用 | Feign 用服务端同构 DTO，ACL 转换内部领域模型（替代 RestTemplate） |
| spring-boot-starter-aop | 随 Boot | **AOP 切面业务日志** | 见第 3 章日志基线 |
| Lombok | 1.18.x | 样板代码精简 | provided scope |
| Jackson | 随 Boot | JSON | |

### 1.3 数据与存储

| 项 | 基线 | 演进路径 |
|---|---|---|
| 数据库 | 嵌入式 H2（file 模式） | SQLite / MySQL / PostgreSQL（标准方言迁移） |
| 文件存储 | `FileStorageService` 接口 + 本地目录实现 | MinIO（新增实现 + SDK 届时评审） |
| DDL | schema.sql/data.sql 启动幂等执行 | Flyway（届时评审） |
| 枚举 | application.yml 配置化（改配置重启生效） | 字典表+界面（如需在线扩充） |

### 1.4 AI 能力

| 项 | 基线 |
|---|---|
| 模型服务 | OpenAI 兼容多模态模型（国产 GLM-VL / Qwen-VL 等），型号/密钥/地址环境变量配置化，不绑定厂商 |
| 集成 | 不引 SDK；Feign 声明式客户端 + **防腐层**（转换/异常映射/重试收敛）；同步调用（读超时 90s） |
| 边界 | AI 调用收敛于单一领域接口（如 `AiParseService`），仅业务必需环节调用 |
| 拆分预留 | 接口可替换为独立 Python 服务 |

### 1.5 导出与文档

| 项 | 方案 |
|---|---|
| Markdown 导出 | 服务端模板拼接（零依赖，结构与数据规约一致可再导入） |
| PDF 导出 | commonmark-java（Markdown→HTML）+ openhtmltopdf + 中文字体 |
| 接口文档 | springdoc-openapi-ui 1.7.x + Swagger UI（生产可关闭） |

## 2. 代码质量保障基线

1. **触发机制：仅 commit 钩子**（husky + lint-staged；项目须先 `git init`；不设 CI 重复检查）
2. 前端门禁：ESLint + Prettier（增量）→ vue-tsc → **Vitest（纯逻辑 + @vue/test-utils 组件测试）**
3. 后端门禁：Spotless（google-java-format **1.7.x**）→ Checkstyle → SpotBugs（中高危阻断）→ JUnit 5 + MockMvc
4. **覆盖率门禁：JaCoCo**——总行 ≥70%、核心业务包 ≥85%（`mvn jacoco:check` 链尾阻断）
5. 深度分析：**IDE SonarLint 插件**（非门禁，不部署 SonarQube 服务端）
6. TDD：核心业务域严格测试先行（红→绿→重构）；测试用例从需求规约提炼并编号映射
7. `--no-verify` 仅限 WIP；正式合入前必须完整通过一次

## 3. 架构模式与日志基线

- 单体分层、单 JAR 交付、零外部中间件起步；**仅本机监听 127.0.0.1 + 免口令**（开放局域网前须升级 EOL 栈并加访问控制）
- 外部能力（AI、文件存储）一律接口抽象，实现可替换
- 数据可迁移：全量 JSON 导入导出与数据规约一致；数据目录整体拷贝即备份
- **双层日志（切面日志规约）**：系统运行日志（logback 文件）+ 业务日志（business_log 表持久化，**记录 AI 返回与业务返回全文**，永久保留）+ 运行日志查询页（类型/模块/成败/时间筛选）；日志失败不影响业务；不记录密钥

## 4. 技术选型评审规约

1. 新引入技术栈/中间件：**明确提示 → 交互式评审确认 → 登记选型评审表 → 方可集成**
2. 评审结果随架构文档版本化；撤销项注明「撤销、未集成」
3. 每阶段结束做一次**一致性巡检**（对照单一事实来源索引核对漂移）

## 5. 规格与沟通规约（SDD 对齐）

- 需求规约采用 **FR 编号 + EARS 句式**（当〈触发〉时，系统应〈响应〉），可验收可追溯
- 建立 **spec/README.md 单一事实来源索引** + 追溯矩阵（FR→业务规则→API→测试用例→UI）
- 接口契约唯一来源 api-contract.md（架构文档中仅放指针，禁止双处维护）
- 沟通记录：`spec/communication-log.md`，格式 `时间 | 提问/事项 | 结论/决策 | 作者 | 归类（需求/架构/设计/编码/测试）`，只追加不删除
- 阶段门禁：需求 → 架构 → 就绪评审 → 原型 → 编码（tasks.md 任务分解）→ 测试，门禁确认留痕

## 6. 新项目启动时的开放确认项（模板占位）

- [ ] 数据库选型（嵌入式 / MySQL / PostgreSQL）
- [ ] 用户体系与访问边界（单机免登录 / 局域网+口令）
- [ ] 导出格式（Markdown / PDF / Excel / CSV）
- [ ] 大模型型号与账号（OpenAI 兼容端点、密钥仅环境变量）
- [ ] 部署形态（本机单 JAR / Docker Compose）
- [ ] 业务日志保留策略（默认永久，按量评估）
- [ ] 项目特有中间件评审（按第 4 章流程）
