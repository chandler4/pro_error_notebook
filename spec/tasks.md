# 实施任务分解（spec/tasks.md）

| 项 | 内容 |
|---|---|
| 版本 | v1.2 |
| 日期 | 2026-11-12 |
| 依据 | chandler tasks 指令（三工程 error-notebook-ai / be / fe）+ 四项确认（2026-11-11 17:24：Java 独立 AI 服务、业务语义内部接口、本文件单文件组织、骨架优先）；v1.1 = 2026-11-12 需求/任务 review 补缺；v1.2 = 模型选型落档（智谱 GLM Coding Plan · glm-5.3-flash，官方推荐采样参数）——详见沟通记录同日条目 |
| 关联 | [README.md](README.md)（追溯矩阵）、[architecture.md](architecture.md) v3.2、[api-contract.md](api-contract.md) v1.4 第 4 章、[test-strategy.md](test-strategy.md) v1.1 |

## 0. 任务字段与执行约定（SDD 标准）

- **任务字段**：ID · 名称 · 关键文件 · 追溯（FR/BR/TC）· 依赖 · DoD（完成定义）· 状态
- **状态标记**：`☐ 待办` `◐ 进行` `✓ 完成(日期)`——只改本表状态列 + 沟通记录留痕（归类：编码）
- **执行流程**：按依赖取任务 →（⭐ 核心域任务先写失败测试，引用 TC 编号）→ 实现 → `mvn test` / `npm run test` 通过 → git commit（pre-commit 门禁全绿：lint/type/单测/静态分析/JaCoCo）→ 更新状态
- **并行规则**：三个 workstream 对应三个独立工程，可并行；跨工程依赖以「契约」为界（FE↔BE 用 api-contract 第 3 章，BE↔AI 用第 4 章内部契约），**契约变更须评审并同步文档后才能继续**
- **端口约定**：BE `127.0.0.1:8080`；AI 服务 `127.0.0.1:8081`（仅回环，无鉴权）；FE dev `5173`（代理→8080）

## 1. T0 骨架（走通切片，先行）

| ID | 任务 | 关键文件 | 追溯 | 依赖 | DoD | 状态 |
|---|---|---|---|---|---|---|
| T0-1 | 仓库与三工程初始化：git init + husky/lint-staged；error-notebook-ai、error-notebook-be（Maven 骨架，**含 Lombok provided，2026-11-11 16:13 已评审**）、error-notebook-fe（Vite+Vue3+TS+Element Plus）；质量链装配（BE：Spotless/Checkstyle/SpotBugs/JaCoCo；FE：ESLint/Prettier/vue-tsc/Vitest） | `.husky/`、根 `package.json`、三工程 `pom.xml`/`package.json` | 架构 §6 | — | 三工程空壳可启动；任意 commit 触发全量门禁 | ◐ 代码就绪待验证（2026-11-12，Bash 权限阻断 git/mvn/npm） |
| T0-2 | 三服务联通切片：BE 暴露 `GET /api/v1/meta/enums`（yml 枚举）；AI 服务暴露 `POST /internal/parse`（**mock 草稿**+raw）；FE 演示页走通 FE→BE→AI(mock) | BE `MetaController`；AI `ParseController`(mock)；FE `demo` 页 | FR-A3 最小切片 | T0-1 | 浏览器演示页端到端返回 mock 数据 | ◐ 代码就绪待联调（2026-11-12） |

## 2. Workstream A：error-notebook-ai（AI 服务，127.0.0.1:8081）

| ID | 任务 | 关键文件 | 追溯 | 依赖 | DoD | 状态 |
|---|---|---|---|---|---|---|
| A-1 | 内部契约与 DTO：`/internal/parse` 请求/响应（draft + rawModelResponse + model + durationMs）+ springdoc | `dto/`、`ParseController` | 契约 §4 | T0-2 | 契约与 api-contract §4 一致，Swagger 可调试 | ☐ |
| A-2 | ⭐ 模型客户端（Feign·OpenAI 兼容）：chat/completions 多模态组装（图片按角色排序：题目图在前）、超时 5s/90s；**固化图片获取方式（契约 §7.1）**：AI 服务回环 `GET /files/images/**` 从 BE 拉取处理图（架构 §2.1 链路④）；日志仅打印 base-url/模型名不打 api-key | `client/OpenAiModelClient` | FR-A3、TC-P-04、TC-S-03 | A-1 | mock Feign 单测：请求体结构/图片顺序/超时配置/图片拉取 | ☐ |
| A-3 | ⭐ 防腐层：OpenAI DTO↔草稿 DTO 转换、输出严格 JSON 校验、失败重试 1 次、异常映射（42001/42002）；raw 原文透出；草稿 `tags` 恒为空数组 | `acl/OpenAiVisionAdapter` | FR-A3/A4、TC-P-01/02/03、TC-T-04 | A-2 | TC-P-01/02/03 单测绿 | ☐ |
| A-4 | Prompt 模板：system（仅输出严格 JSON）+ 规约结构 few-shot + **官方推荐采样参数（temperature 1 / top_p 0.95，思考型模型 glm-5.3-flash，2026-11-12 确认）**，模板外置可配置 | `prompt/`、`application.yml` | FR-A3 | A-1 | 模板单测（变量替换/字段完整） | ☐ |
| A-5 | 解析接口组装：Controller→Adapter，响应含 raw/model/durationMs（供 BE 落业务日志） | `ParseController`、`service/` | FR-A3、BR-09 | A-3/A-4 | MockMvc：成功/42001/42002 三态 | ☐ |

## 3. Workstream B：error-notebook-be（后端，127.0.0.1:8080）

| ID | 任务 | 关键文件 | 追溯 | 依赖 | DoD | 状态 |
|---|---|---|---|---|---|---|
| B-1 | ⭐ 基础工具层：common（ApiResponse/ErrorCode/BusinessException/全局异常/JsonUtil/IdGenerator）+ Clock 配置 + schema.sql 三表 + application.yml（含 AI 服务地址占位） | `common/`、`config/`、`resources/` | 架构 §5.6/§5.7 | T0-1 | JsonUtil/异常处理单测绿；启动建表成功 | ☐ |
| B-2 | ⭐ 切面日志：LogRecord 注解 + OperationLogAspect（防递归）+ BusinessLog 实体/Mapper/Service + /api/v1/logs 查询 | `log/` | BR-09/10、FR-C1~C4、TC-G-01~05 | B-1 | TC-G 全绿（入库失败不影响业务） | ☐ |
| B-3 | ⭐ 错题域持久层：WrongQuestion 实体/Mapper（多图 JSON、tags）+ KnowledgeService（字典自动累积、**错题删除减计**、归并候选） | `entity/`、`mapper/`、`service/` | BR-03/11、TC-C-03/04、TC-D-01 | B-1 | 归并累积/减计/候选排序表驱动单测绿；表↔规约字段映射 round-trip（TC-D-01） | ☐ |
| B-4 | ⭐ AI 客户端与解析编排：AiServiceClient（Feign→8081）；ParseService 编排（调内部 parse→草稿入库 PENDING_CONFIRM→recordAi 落业务日志含 raw） | `integration/ai/`、`service/ParseService` | FR-A3/A5、TC-P-01/05、TC-G-03 | B-2/B-3、A-5 | mock AI 服务：TC-P-01/05 绿；business_log 含 raw 全文 | ☐ |
| B-5 | 图片上传与存储：FileStorageService + LocalFileStorage（魔数/10MB/6 张校验、UUID 命名、yyyyMM、路径穿越防护）+ **静态资源映射 `GET /files/images/**`**（架构 §5.4/§2.1 链路②′④：FE 图片回显与 AI 服务拉取共用） | `integration/storage/`、`config/`、`controller/` | FR-A1、TC-U-05~07、TC-S-01 | B-1 | 上传校验/穿越攻击用例绿；图片经 `/files/images/**` 可访问（AI 服务与 FE 依赖） | ☐ |
| B-6 | ⭐ 详情/确认与编辑：GET `{id}` 详情（错题/草稿共用，3.4）、confirm（必填校验/转 CONFIRMED/知识点归并）、PUT 编辑、DELETE 硬删联动删图 | `service/`、`controller/` | FR-A6/B3、BR-04、TC-C-01/02/05、TC-T-01 | B-3/B-5 | TC-C-01/02/05 绿（verify 删图调用）；tags 持久化去重（TC-T-01） | ☐ |
| B-7 | 列表与草稿查询：page 多维筛选（subject/keyword/knowledgePoint/**tag**/type/date 区间）+ drafts 列表/手动删除 + **POST drafts 创建空草稿**（转手动填写入口，3.8，零 AI 调用） | `controller/`、`service/` | FR-B1/B2/B7、FR-A4/A5、TC-L-01~05、TC-T-02、TC-P-07 | B-3 | 筛选/分页边界/标签模糊接口测试绿；空草稿入库且零 AI 交互（TC-P-07） | ☐ |
| B-8 | ⭐ 导出：Markdown 模板（规约结构 golden 比对）+ commonmark-java→openhtmltopdf（重做模式：答案页置尾）；多图导出兼容 questionImage/questionImages | `export/` | FR-B4、TC-E-01~03、TC-D-02 | B-7 | golden 文件测试 + PDF 视觉人工确认 | ☐ |
| B-9 | ⭐ 备份迁移：全量导出/导入（skip/overwrite、行级错误、≤20MB、幂等） | `backup/` | FR-B5、TC-E-04~06、TC-S-04 | B-3 | round-trip/幂等/错误明细测试绿 | ☐ |
| B-10 | meta 接口：enums（yml 配置化）+ knowledge-points 联想候选 | `controller/MetaController` | FR-B1、TC-C-04 | B-3 | 与 yml 一致性测试绿 | ☐ |
| B-11 | 接口测试全量：MockMvc + H2 内存 test profile 覆盖全部端点（含响应字段 camelCase 与规约一致性） | `src/test/` | 全部接口层 TC、TC-D-03 | B-4~B-10 | 接口层用例 100% 执行并记录 | ☐ |

## 4. Workstream F：error-notebook-fe（前端，Vite dev 5173）

| ID | 任务 | 关键文件 | 追溯 | 依赖 | DoD | 状态 |
|---|---|---|---|---|---|---|
| F-1 | ⭐ 工程基础：request 封装（fetch/超时 120s 解析/统一 code/错误 toast）+ types（规约 DTO）+ router/pinia + 图片工具（canvas 压缩 ≤2000px、cropperjs 裁剪封装） | `api/request.ts`、`utils/image.ts`、`types/` | FR-A1/A2、TC-U-01 | T0-1 | request/压缩工具单测绿（TC-U-01） | ☐ |
| F-2 | ⭐ 错题记录页：分科 Tab/待确认入口/三条件筛选（含标签）/表格（知识点+标签列）/分页/删除确认/详情抽屉（markdown-it+KaTeX+**DOMPurify**） | `views/QuestionListView`、`components/` | FR-B1/B2/B3/B7、TC-L-06/07、TC-T-03、TC-S-02 | F-1、B-7 契约 | 组件测试绿；XSS 消毒用例绿 | ☐ |
| F-3 | ⭐ 新增/编辑页：三步流状态机/多图角色/裁剪弹窗/解析中与失败兜底（**转手动填写调 POST drafts**，3.8）/分项确认（知识点联想+标签）/编辑与草稿续编模式（?id=&mode=）；**编辑模式重新解析：confirm 新记录成功后联动 DELETE 旧记录**（契约 3.3 注记，TC-N-06） | `views/QuestionCreateView`、`ImageUploader` | FR-A1~A6、TC-U-02~04、TC-P-06/07、TC-N-01~04/06 | F-1、B-4 契约 | 状态机/校验/联想组件测试绿；转手动与删旧联动用例绿 | ☐ |
| F-4 | 运行日志页：四维筛选/类型徽章/全文详情抽屉 | `views/LogsView` | FR-C3 | F-1、B-2 契约 | 组件测试绿 | ☐ |
| F-5 | 备份页与导出弹窗：导出（格式/重做开关/条件摘要）/导入（skip/overwrite/结果报告/行级错误表） | `views/BackupView`、`ExportDialog` | FR-B4/B5 | F-1/F-2 | 组件测试绿 | ☐ |
| F-6 | 前端质量收口：Vitest 组件用例齐备、vue-tsc 零 error、ESLint 零违规 | `package.json` scripts | 质量基线 §2 | F-2~F-5 | `npm run test`/`type-check`/`lint` 全绿 | ☐ |

## 5. T9 集成验收（收尾）

| ID | 任务 | 内容 | 追溯 | 依赖 | DoD | 状态 |
|---|---|---|---|---|---|---|
| T9-1 | 端到端联调 | 三工程联合启动，按规约场景 1a/1b + 规约 5 全流程走查（含失败兜底转手动、编辑重解析删旧、标签、日志全文）；AI 边界代码走查（BR-01） | FR 全集、TC-E-07、TC-N-05 | A-5、B-11、F-6 | 走查清单逐项通过并留痕 | ☐ |
| T9-2 | 测试全量与门禁核对 | 56 个 TC 分层执行核对表；JaCoCo **双 Maven 工程（BE/AI）分别达标**：总行 ≥70%/核心包 ≥85%（AI 服务 acl/client 纳入核心包） | test-strategy 全文 | T9-1 | 覆盖率报告 + TC 执行记录归档 spec/ | ☐ |
| T9-3 | 打包部署验证 | FE 构建产物并入 BE 静态托管；双 JAR 启动（8080/8081）+ 外置配置模板（api-key 环境变量、**springdoc 生产关闭开关**）；灾备演练（拷贝 data/ 迁移恢复）；备份页明示「JSON 迁移不含图片，跨机需配合 data/ 目录」 | 部署架构 §8、test-strategy §7 手工 E2E | T9-2 | 双击启动可用；备份迁移演练通过 | ☐ |

## 6. 状态总览

| Workstream | 任务数 | 完成 |
|---|---|---|
| T0 骨架 | 2 | 0 |
| A（AI 服务） | 5 | 0 |
| B（后端） | 11 | 0 |
| F（前端） | 6 | 0 |
| T9 集成 | 3 | 0 |
| **合计** | **27** | **0** |

> 原 `ui/tasks/` 草稿已归档删除，以本文件为准（2026-11-11 17:24）。
