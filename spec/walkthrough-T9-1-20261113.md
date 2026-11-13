# T9-1 端到端联调走查记录

> 2026-11-13 · 执行：AI 助手（API 层 + 代码走查）/ 待 chandler（浏览器项）· 依据：tasks.md T9-1、api-contract v1.6、notebook.md 场景 1a/1b、test-strategy §7

## 0. 环境与前置

- 三服务联合启动：FE 5173（Vite dev）/ BE 8080 / AI 8081，均仅 127.0.0.1 监听（security §2 ✓）
- `ERRNOTE_AI_API_KEY` 注入，**真实调用智谱 glm-5.3-flash**（Coding Plan 端点）
- 按 2026-11-12 落档决定清除 `error-notebook-be/data` 旧库（DDL 变更不自动迁移）
- 走查用真图由 `error-notebook-fe/scripts/gen-walkthrough-img*.cjs`（node-canvas）生成

## 1. API 层走查结果（本节全部通过）

| # | 走查项 | 契约 | 结果 | 证据摘要 |
|---|---|---|---|---|
| 1 | 多图上传 | 3.1 | ✓ | 2 图 multipart → 相对路径 `images/202611/*.png` |
| 2 | 真实 AI 解析 → 草稿 | 3.2 | ✓ | glm-5.3-flash 真调成功（25389ms）；subject=math/题干 LaTeX/知识点识别准确；`tags=[]` 恒空（TC-T-04）；PENDING_CONFIRM + errorQuestionId |
| 3 | 确认入库 + 标签 | 3.3 | ✓ | CONFIRMED；tags=["二刷","易错"] 持久化（TC-T-01） |
| 4 | 分科/模糊/标签/组合筛选 + 分页 | 3.5 | ✓ | subject=math 命中；keyword「抛物线」命中/无关词空集；tag 单独与 subject+tag 组合均正确（1a、TC-T-02）；默认排序 wrongDate DESC |
| 5 | 知识点归并累积/删除减计 | 3.11 | ✓ | confirm 后 useCount=1；删题后 useCount=0 |
| 6 | 转手动填写（空草稿） | 3.8 | ✓ | POST drafts 生成 PENDING_CONFIRM 空草稿；**零 AI 调用**（AI_CALL 日志总数佐证，TC-P-07）；补全 → confirm → 列表可见 |
| 7 | 编辑重解析删旧 | 3.2/3.3/3.7 | ✓ | TC-N-06 语义全链路：换图重解析→**新草稿新 id**→confirm 新→DELETE 旧；旧记录 2 图磁盘清理（BR-04） |
| 8 | 详情/编辑/删除 | 3.4/3.6/3.7 | ✓ | GET 详情回读；PUT 编辑 tags 增改生效；DELETE 硬删 |
| 9 | 导出 markdown | 3.9 | ✓ | HTTP 200 text/markdown，golden 模板结构正确 |
| 10 | 备份导出 + 导入幂等 | 3.10 | ✓ | 导出 JSON 数组；skip 导入 total=2/skipped=2/inserted=0/errors=[] |
| 11 | 业务日志查询 | 3.12 | ✓ | AI_CALL 成功条目含 **raw 原文全文** + model + durationMs（TC-G-03）；OPERATION 切面条目含请求/返回内容 |
| 12 | W-1 回归（昨日修复） | 3.12 | ✓ | 失败 AI 调用落 AI_CALL：success=false + errorCode=40000 + durationMs |
| 13 | W-2 回归（昨日修复） | 3.2/§7.1 | ✓ | 坏图 URL → code 40000，message 带失败 url 明细；BE 透传 |
| 14 | 静态图片访问 | — | ✓ | `GET /files/images/**` → 200 image/png（B-5 映射） |
| 15 | 元数据 | 3.11 | ✓ | enums 全量返回 |
| 16 | 草稿区 | 3.8 | ✓ | 待确认列表（创建倒序）+ 草稿删除；删后列表空 |

**TC-E-07 AI 边界代码走查（BR-01）✓**：BE `AiServiceClient` 仅 `ParseServiceImpl`（解析用例）引用，BE 无直连模型代码（bigmodel/openai/RestTemplate/WebClient 零命中）；AI 服务唯一端点 `/internal/parse`、`OpenAiVisionAdapter` 仅 ParseController 引用、无 DataSource（无库无状态）；FE 零 AI 感知（仅 DemoView.vue 注释提及，见 W-3）。

**附带实证**：A-5 遗留风险「Coding Plan 套餐 key 自研后端调用额度归属」——真调成功 ×2，风险关闭。

## 2. 走查发现与处置（2026-11-13 chandler 决策）

| # | 级别 | 内容 | 处置（chandler 确认） |
|---|---|---|---|
| W-1 | — | （昨日）失败 AI 调用未落 AI_CALL 日志 | 已修复（87e9a3d），本次回归 ✓ |
| W-2 | — | （昨日）取图失败未映射 40000+url 明细 | 已修复（b8ccd10），本次回归 ✓ |
| W-3 | 中 | `/demo` T0-2 演示页「调用解析」按钮指向 `/api/v1/demo/parse`，BE DemoController 已随 B-4 删除 → 死链 404（enums 按钮仍可用） | **T9-3 打包前移除演示页路由+页面**（已补入 tasks.md T9-3） |
| W-4 | 低 | AI_CALL 失败条目 `action="AI解析 model="` 型号名为空 | **已修复**：内部契约 v1.7 错误信封 `data={"model"}`，BE 失败留痕携带（BE 传输层失败无从获知仍留空）；AI 26 测试 / BE 101 测试全绿，修复后真机复验 ✓（见 §1 第 12/13 项与 §5 复验记录） |
| W-5 | 中 | **chandler 浏览器走查发现（10:17）**：真实照片解析超 90s，BE Feign 读超时（90s）与 AI 模型读超时（90s）**同值竞速**——AI 的 42002 信封未及到达 BE 已判传输失败，明细丢失（log id=70：90015ms/默认文案/model 空）；外部智谱接口本身健康（直连 `/internal/parse` 真调正常返回） | **已修复（阶梯化，chandler 确认）**：`AI 90s < BE Feign 100s < FE 120s`（application.yml + FeignTimeoutLadderTest 阶梯断言）；架构 v3.3 同步（§2.1 图/明细表/时序图）；修复后全链路真调复验 ✓（见 §5） |
| W-6 | 中 | **chandler 走查重试反馈（10:36）+ curl 外部接口实测**：外部智谱接口对「长思考+非流式」响应**间歇性断连/超时**——文本 ping ✓ 秒回；同一真实照片视觉调用第 1 次 ✓（物理电路题完整转写）、第 2 次 ✗ 25.7s 断连（HTTP 000）；无代理劫持、TLS 1.0s 正常 → 配置正确、外呼链路时稳时断（AI 日志两见 RetryableException） | **已修复（网络级重试 1 次，chandler 确认）**：`OpenAiVisionAdapter.callModel` 网络异常先重发 1 次再判 42002（与 JSON 非法重试独立，最坏物理调用 ≤4；快断连类故障重试落在 W-5 阶梯内）；AI 27 测试全绿（重试自愈用例 + 计数断言）；架构 §5.2 重试语义同步；修复后真实照片全链路复验 ✓（见 §5） |
| W-7 | 高 | **chandler 走查持续失败根因（W-5/W-6 之后仍现）**：模型端「默认深思考」——完整 Prompt+真实照片 curl 对照实测：默认思考 **168.3s**（超一切超时阶梯，走查超时/断连多为此等待期被掐断的表现）、`thinking:{type:"disabled"}` 仅 **5.6s** 且草稿质量不降（standardAnswer 反而解出） | **已修复（默认禁用思考，chandler 确认）**：请求体下发 `thinking:{type:"disabled"}`（`app.ai.thinking-type` 可配，空值省略字段走端点默认；切 enabled 须同步评估 W-5 阶梯）；AI 29 测试全绿（组装用例 + DTO 线格式用例）；架构 §5.2/v3.4 同步；修复后真实照片全链路复验 ✓ **10.8s** 成功（AI_CALL id=113 model=glm-5.3-flash，standardAnswer 非空）（见 §5） |
| 观察 | 低 | 真调下 AI 草稿 `standardAnswer`/`knowledgeAnalysis` 常为空（答案纸学生解答未映射到标准答案；解析未生成）——分项确认本就允许人工补全 | **v1 接受不调 Prompt**，留档后续迭代项 |

## 3. 浏览器侧走查项（待 chandler 确认，三服务已保持运行）

> 访问 http://localhost:5173 （数据已就绪：math 第16题「二刷」、physics 第14题「粗心/二刷」，日志含 1 成功 1 失败 AI_CALL 全文）

- [ ] P1 列表页：三菜单/分科 Tab 切换/待确认区入口/查询+重置交互/详情抽屉多图/导出弹窗/硬删二次确认
- [ ] P2 新增三步流：拖拽多图/裁剪弹窗/角色标记/解析等待与失败兜底转手动入口/分项确认表单/知识点联想/标签输入
- [ ] P2 编辑模式：预填回显/换图重解析→confirm 新自动删旧提示
- [ ] P3 日志页：类型/模块/成败/时间筛选/全文抽屉（raw 可读）
- [ ] P4 备份页：导出/导入 skip-overwrite/行级错误展示

## 4. 浏览器不可代验项归属

- PDF 中文字体视觉、浏览器兼容（Chrome/Edge）、灾备演练 → T9-3（tasks.md 既定归属）

## 5. W-4 修复后真机复验（2026-11-13，修复提交后）

- 重启 AI+BE 新代码后，重放坏图 URL 解析（40000 路径）→ `GET /api/v1/logs?logType=AI_CALL&success=false` 失败条目 `action="AI解析 model=glm-5.3-flash"` ✓（复验细节以 communication-log 当日记录为准）
- **W-5 修复后全链路复验**：重启 BE（新 read-timeout 100s）后经 8080 全链路真实解析 ✓（细节见 communication-log 当日记录）
- **W-6 修复后真实照片复验**：重启 AI（网络级重试版）后用 chandler 真实物理照片经 8080 全链路解析 ✓（细节见 communication-log 当日记录）
- **W-7 修复后真实照片复验（11:11）**：重启 AI（思考禁用版）后同一张真实物理照片经 8080 全链路 **10.8s** 解析成功（默认思考 168.3s 必超时）——physics/grade9/fillBlank 草稿 standardAnswer 非空，AI_CALL id=113 success=true durationMs=10603 model=glm-5.3-flash；临时草稿已硬删清理（细节见 communication-log 当日记录）
