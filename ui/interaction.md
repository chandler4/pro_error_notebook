# ui/ 原型交互说明

| 项 | 内容 |
|---|---|
| 版本 | v2.0（按 [spec/prototype-spec.md](../spec/prototype-spec.md) v1.0 重构，2026-11-11） |
| 查看方式 | 浏览器直接打开各 HTML（纯静态、零依赖、mock 数据）；页面间菜单/按钮可真实跳转 |
| 状态 | 待 chandler 走查确认 → 确认后与 prototype-spec 共同升格为前端实现规格 |

## 1. 原型清单

| 文件 | 页面（prototype-spec） | 路由 | 覆盖 FR |
|---|---|---|---|
| [list.html](list.html) | P1 错题记录 | /questions | FR-B1/B2/B3、FR-A5（草稿入口）、FR-B4（导出弹窗） |
| [create.html](create.html) | P2 新增/编辑三步流 | /questions/new[?id=&mode=] | FR-A1~A6 |
| [logs.html](logs.html) | P3 运行日志 | /logs | FR-C3 |
| [backup.html](backup.html) | P4 数据备份 | /backup | FR-B5 |

## 2. 关键交互说明

### 2.1 错题记录（list.html）
- **分科 Tab**（数学/物理/化学）：切换携带科目重新查询；Tab 数据源 `meta.enums.subjectTabs`（yml 可扩充）。
- **待确认 (N) 入口**：Tab 行右侧，点击**同页切换**为草稿列表视图（题干预览/科目/创建时间 + 继续编辑/删除）；「继续编辑」→ create.html?id=&mode=draft 直达步骤3。
- **查询/重置**：题干模糊 + 知识点 + **标签**（FR-B7，灰底虚线标签样式区分知识点）三个条件；查询重置 page=1；重置清空并刷新（TC-L-06）。
- **导出弹窗**：格式单选（Markdown/PDF）+ 重做模式开关（仅 PDF 可用，说明「遮盖答案、答案页置尾」）+ 当前筛选条件摘要；下载 toast 反馈。
- **详情抽屉**：多图横向展示（题目图/解答图角色角标）、题干、答案、知识点、解析（说明按 DOMPurify 消毒渲染）、元信息。
- **删除**：二次确认弹窗（说明硬删除不可恢复）。

### 2.2 新增/编辑三步流（create.html）
- **步骤1 上传**：拖拽/点选多图（≤6 张限制演示）；缩略图角色徽标点击切换（题目图/解答图）；裁剪弹窗（cropperjs 占位演示）；删除。
- **步骤2 解析中**：loading + 分步提示；「模拟解析失败」链接演示**异常态**（42001 + 重试 + 转手动填写）。
- **步骤3 分项确认**：基本信息表单、题干必填校验（标红+40000 提示）、答案动态增删、**知识点归并联想**（输入 ≥1 字触发候选，点选「复用」优先已有知识点，也可回车自由添加）、**标签**（自由多标签回车添加、灰底样式与知识点区分，FR-B6/BR-11：手工标注、AI 不自动生成）、解析多行。
- **编辑/草稿续编模式**：URL `?id=xxx&mode=edit|draft` 顶部显示模式横幅、直达步骤3、提供「重新解析」入口（演示从 list.html 编辑按钮/草稿继续编辑进入）。
- **状态机**：上传 →(≥1图)→ 解析中 →(成功)→ 确认 →(校验)→ 完成；失败态可重试/转手动；返回上一步保留图片。

### 2.3 运行日志（logs.html）
- 筛选：类型（业务操作/AI 调用）、模块模糊、成功状态、日期区间；查询/重置。
- 表格：类型徽章、模块（等宽字体）、操作描述（AI 行含 model=xxx）、成败、错误码、耗时、时间。
- 详情抽屉：traceId/元信息 + 请求内容 + **返回内容全文**（深色等宽块，含 AI 返回原文演示、失败原因、导入错误明细等真实形态样例）。
- 页脚说明：本页查询不产生业务日志（防递归，api-contract 3.12）。

### 2.4 数据备份（backup.html）
- 导出卡片：全量已确认错题 JSON（说明不含图片、需配合 data/ 目录迁移）。
- 导入卡片：选择文件（≤20MB）+ 模式单选（skip/overwrite）→ 结果报告（total/inserted/skipped/overwritten/errors 计数卡 + 行级错误明细表，演示 43002 场景）。

## 3. 异常与空态演示入口

| 场景 | 演示方式 |
|---|---|
| 解析失败（42001） | create.html 步骤2「模拟解析失败」 |
| 空列表 | list.html 输入不命中关键词查询 |
| 必填校验（40000） | create.html 步骤3 清空题干后确认 |
| 上传超限（41003） | create.html 一次拖入 >6 张 |
| 导入行级错误（43002） | backup.html 选择文件后开始导入 |
| 日志失败态（42001/43002/41001） | logs.html 筛选「失败」 |

## 4. 与实现的对齐要点（给编码阶段）

- 所有交互对应 api-contract v1.1 端点；响应按 `{code,message,data}` 统一拦截（request 封装）。
- 知识点联想 = GET /api/v1/meta/knowledge-points；Tab = GET /api/v1/meta/enums → subjectTabs。
- Markdown/LaTeX 渲染：markdown-it + KaTeX + DOMPurify（v-html 仅消毒后内容）。
- 图片：cropperjs 裁剪 + canvas 压缩（长边 ≤2000px）后再上传。
