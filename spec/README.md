# spec/ 文档地图（单一事实来源索引）

> v1.1 · 2026-11-12 · 依据就绪评审报告 [readiness-review-20261111.md](readiness-review-20261111.md) INT-01/02 建立；v1.1 = 需求/任务 review 补缺落档（见沟通记录 2026-11-12）

> **📍 当前断点（2026-11-12 更新）**：需求/五层架构/就绪评审/任务分解/review 补缺全部完成；**原型 ui/ v2.0 已产出但浏览器走查确认尚未执行**（16:52 遗留门禁）——走查确认后进入编码 **T0-1**（[tasks.md](tasks.md) v1.1：git init + 三工程 error-notebook-ai/be/fe 脚手架 + 质量链装配），按 tasks.md 依赖与状态推进。

## 1. 单一事实来源声明（Single Source of Truth）

| 关注点 | 唯一来源文档 | 其他文档中的引用方式 |
|---|---|---|
| 需求与规约 | [notebook.md](notebook.md)（FR 编号 + EARS） | 引用 FR-xx |
| 业务规则 | [architecture-business.md](architecture-business.md)（BR-xx） | 引用 BR-xx |
| 技术架构/技术栈评审 | [architecture.md](architecture.md) | 不重复维护接口/数据明细 |
| 数据模型/DDL/生命周期 | [architecture-data.md](architecture-data.md) | 引用表名/字段 |
| 接口契约 | [api-contract.md](api-contract.md) | **技术架构 §7 为指针**，禁止双处维护 |
| 安全设计 | [security.md](security.md) | 引用章节 |
| 测试用例/TDD | [test-strategy.md](test-strategy.md)（TC-xx） | 引用 TC-xx |
| 原型规格 | [prototype-spec.md](prototype-spec.md) | ui/ 产物按此重构 |
| 实施任务 | [tasks.md](tasks.md) | **任务执行唯一来源**（27 任务：T0 骨架/A·B·F workstream/T9 集成），状态随进度更新 |
| 决策记录 | [communication-log.md](communication-log.md) | 全部评审决策留痕，只追加不删除 |
| 后续项目模板 | [claude_standard.md](../claude_standard.md) | 与项目内实际栈保持同步 |

## 2. 阶段流程与门禁

```
需求（.claude.md + notebook.md 规约）
  → 架构（业务→技术→数据→契约→安全→测试）   [✅ 完成 2026-11-11]
  → 就绪评审（readiness-review）            [✅ 完成 2026-11-11，条件通过→已补缺]
  → 原型（prototype-spec → ui/）            [⏳ 待 chandler 浏览器走查确认（当前断点）]
  → 编码（spec/tasks.md：T0 骨架 → A/B/F 三工程 workstream → T9 集成验收）[待启动，走查确认后从 T0-1 开始]
  → 测试（test-strategy TC-xx 全量执行）    [待启动]
```

**每阶段结束执行一次一致性巡检**（对照上表核对无漂移），巡检结果记入沟通记录。

## 3. 追溯矩阵（FR → BR → API → TC → UI）

| FR（notebook.md） | BR（业务架构） | API（api-contract.md） | TC（test-strategy.md） | UI（prototype-spec.md） |
|---|---|---|---|---|
| FR-A1 多图上传 | BR-06 | 3.1 | TC-U-02/03/05/06/07 | P2 步骤1 |
| FR-A2 裁剪去干扰 | BR-07 | —（前端处理） | TC-U-01/04 | P2 步骤1 |
| FR-A3 AI 解析 | BR-01 | 3.2 | TC-P-01/04/05 | P2 步骤2 |
| FR-A4 解析失败兜底 | BR-01 | 3.2（42001/42002）、3.8（POST drafts，2026-11-12 补） | TC-P-02/03/06/07 | P2 步骤2 |
| FR-A5 草稿暂存 | BR-02 | 3.8 | TC-P-05、TC-C-06 | P1 待确认入口、P2 续编 |
| FR-A6 分项确认 | BR-03 | 3.3、3.11 | TC-C-01~04 | P2 步骤3 |
| FR-A7 仅题目图直接解析 | BR-01 | 3.2、3.3（答案/解析非必填） | TC-N-07 | P2 步骤2/3 |
| FR-B1 分科列表 | BR-02（仅 CONFIRMED） | 3.5 | TC-L-01/07 | P1 |
| FR-B2 搜索筛选 | BR-02（仅 CONFIRMED） | 3.5 | TC-L-02~06 | P1 |
| FR-B3 详情/编辑/删除 | BR-04 | 3.4/3.6/3.7 | TC-C-05 | P1 抽屉+复用 P2 |
| FR-B4 导出 | BR-08 | 3.9 | TC-E-01~03 | P1 导出弹窗 |
| FR-B5 备份迁移 | BR-08 | 3.10 | TC-E-04~06 | P4 |
| FR-B6 标签标注 | BR-11 | 3.3/3.4/3.6（tags） | TC-T-01/03/04 | P2 步骤3 |
| FR-B7 标签筛选 | BR-11 | 3.5（tag） | TC-T-02 | P1 筛选区 |
| FR-C1 切面日志 | BR-09 | —（自动采集） | TC-G-01/02 | —（后端行为） |
| FR-C2 AI 返回记录 | BR-09 | — | TC-G-03 | P3 详情全文 |
| FR-C3 运行日志查看 | BR-10 | 3.12 | TC-G-05 | P3 |
| FR-C4 日志可靠性 | BR-09 | — | TC-G-04 | —（后端行为） |

## 4. 变更规约

- 任何文档修改需在沟通记录留痕；涉及决策的修改需交互确认。
- 追溯矩阵新增/变更需求时同步更新（FR→BR→API→TC→UI 五点对齐）。
