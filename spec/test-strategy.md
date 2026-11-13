# 错题整理应用 · 测试策略与 TDD 设计

| 项 | 内容 |
|---|---|
| 文档版本 | v1.1 |
| 日期 | 2026-11-12 |
| 作者 | chandler（决策评审）、AI 助手（起草） |
| 状态 | ✅ 已评审通过（评审人：chandler，2026-11-11） |
| 输入 | `spec/notebook.md`（规约 1~4 + 场景设计）、`.claude.md`、[architecture-business.md](architecture-business.md)（BR-01~08）、[security.md](security.md) |
| 关联文档 | [architecture.md](architecture.md)（v2.2，技术架构）、[api-contract.md](api-contract.md)、[communication-log.md](communication-log.md) |

**本层确认决策（2026-11-11 16:04，chandler）**：TDD 严格度＝核心域严格 TDD；前端测试＝纯逻辑 + @vue/test-utils 组件测试（新依赖，评审通过）；覆盖率门禁＝JaCoCo（总行 ≥70%、核心业务包 ≥85%，新插件，评审通过）。

**v1.1 修订（2026-11-12，chandler）**：review 随 v3.0 双服务拆分复核——§4 示例补重试位置注记；§6 JaCoCo 扩为 BE/AI 双工程分别达标（AI 服务 acl/client 入核心包）；TC-P-07 补暴露端点（契约 3.8）；新增 TC-N-06（编辑重解析删旧），用例总数 55→**56**。

**v1.2 修订（2026-11-13，chandler）**：新增 TC-N-07（FR-A7 仅题目图直接解析——单张 QUESTION 图解析可用、空答案草稿可直接确认），用例总数 56→**57**。

---

## 1. TDD 实施策略

### 1.1 严格度：核心域严格 TDD（红 → 绿 → 重构）

| 模块 | TDD 方式 | 说明 |
|---|---|---|
| ParseService（解析编排） | **严格测试先行** | 草稿映射、重试、异常码、手动填写路径 |
| OpenAiVisionAdapter（防腐层） | **严格测试先行** | DTO 转换、异常映射、多图排序（mock Feign） |
| KnowledgeService（归并） | **严格测试先行** | 纯逻辑：累积、候选排序 |
| ExportService / BackupService | **严格测试先行** | golden 文件比对、round-trip、幂等 |
| FileStorage（路径安全） | **严格测试先行** | 穿越攻击用例先行 |
| Controller / Mapper 通道 | 随实现补测 | MockMvc 接口测试，不强制先行 |
| 前端 utils（image/markdown/request）与核心组件 | **严格测试先行** | 压缩、消毒、校验交互 |
| 其余前端视图 | 随实现补测 | 组件测试跟进 |

### 1.2 流程与门禁集成
1. 红：先写失败测试（用例编号引用规约条目，如 `// TC-P-02 规约2`）；
2. 绿：最小实现通过；
3. 重构：在测试保护下重构，pre-commit 全量回归。
pre-commit 链尾追加 `mvn jacoco:check`（见第 6 章），不达标阻断提交。

## 2. 可测试性设计原则（业务域不依赖具体实现）

1. **依赖倒置**：ParseService 依赖 `AiParseService` 接口（而非 Feign 实现）；QuestionService 依赖 `FileStorageService` 接口——单测用 Mockito 替身，**不启动容器、不连库、不发 HTTP**。
2. **防腐层隔离**：Feign 的 OpenAI 原生 DTO 不越出 `integration.ai` 包；领域层只见 `WrongQuestionDraft`。
3. **纯函数化**：知识点归并候选、Markdown/PDF 模板渲染、规约 JSON 导出均为纯函数（输入→输出，无 IO），可直接表驱动测试。
4. **注入点**：`Clock` 与 `IdGenerator`（UUID）通过构造器注入——wrongDate/createdAt/errorQuestionId 在测试中可控可断言。
5. **测试分层矩阵**：

| 层级 | 工具 | 依赖处理 | 速度 |
|---|---|---|---|
| 业务域单元测试 | JUnit 5 + Mockito | mock 全部协作者接口 | 毫秒级 |
| Web 切片 | @WebMvcTest + MockMvc | mock Service 层 | 秒级 |
| 接口集成 | MockMvc + @SpringBootTest（H2 内存 test profile） | 真实库（内存）、mock AI | 秒级 |
| 前端纯逻辑 | Vitest | 无依赖 | 毫秒级 |
| 前端组件 | Vitest + @vue/test-utils | mock request/api 模块与 cropperjs | 秒级 |

6. **标准测试夹具（golden data）**：`spec/notebook.md` 规约 4 的物理题 JSON 全字段作为固定夹具 `fixtures/notebook-sample.json`——字段映射、导出、round-trip 测试共用同一份期望数据，规约变更时夹具同步更新。

## 3. 测试用例库（从规约 / 用户场景提炼）

> 层级：**域**＝业务域单测（不依赖实现）｜**接口**＝MockMvc｜**组件**＝@vue/test-utils｜**前端单测**＝Vitest 纯逻辑｜**手工**＝E2E。

### 3.1 规约 1「录入错题、图片上传、干扰内容人工选择」→ TC-U

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-U-01 | 前端单测 | image.compress 长边 >2000px | 等比压缩至 2000px、不放大小图 |
| TC-U-02 | 组件 | 拖拽多文件上传 | 生成缩略图；非图片类型被过滤 |
| TC-U-03 | 组件 | 图片角色标注 | 首图默认题目图；点徽标切换题目/解答图 |
| TC-U-04 | 组件 | 裁剪交互 | 确定后以裁剪区域输出替换原图（mock cropperjs） |
| TC-U-05 | 接口 | 上传类型校验 | jpg/png（魔数）通过；改扩展名的文本文件 → 41001 |
| TC-U-06 | 接口 | 上传限制 | 单张 >10MB → 41002；单次 >6 张 → 41003 |
| TC-U-07 | 域 | 存储文件名 | 服务端 UUID 命名，原始文件名不落盘 |

### 3.2 规约 2「按数据结构识别字段、AI 解析、供人工确认」→ TC-P

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-P-01 | 域 | 解析成功映射（mock AiParseService） | 草稿字段与规约结构一一对应（标准夹具） |
| TC-P-02 | 域 | AI 输出非法 JSON | 重试 1 次→仍失败→BusinessException 42001；调用计数=2 |
| TC-P-03 | 域 | AI 超时/网络异常 | ACL 映射为 42002，不泄漏底层异常细节 |
| TC-P-04 | 域 | 多图排序 | Prompt 中题目图在前、解答图在后 |
| TC-P-05 | 接口 | 解析后草稿入库 | status=PENDING_CONFIRM；响应含 errorQuestionId（BR-02） |
| TC-P-06 | 组件 | 解析状态展示 | loading 态；失败态提供重试/转手动填写 |
| TC-P-07 | 域 | 转手动填写 | 空草稿入库且**零 AI 调用**（verify 无交互）；经 `POST /api/v1/questions/drafts` 暴露（契约 3.8，2026-11-12 补） |

### 3.3 规约 3「持久化」+ BR-02/03/04 → TC-C

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-C-01 | 接口 | confirm 必填校验 | subject/questionStem 缺失 → 40000 带字段明细 |
| TC-C-02 | 接口 | 确认入库 | 转 CONFIRMED；errorQuestionId 不变 |
| TC-C-03 | 域 | 知识点自动累积 | 新名入字典；已存在 use_count+1；同名不重复 |
| TC-C-04 | 域 | 归并候选 | keyword 过滤 + use_count 降序 + limit（表驱动） |
| TC-C-05 | 域 | 硬删除联动 | 记录删除 + verify FileStorageService.delete（BR-04） |
| TC-C-06 | 接口 | 草稿管理 | 列表仅 PENDING_CONFIRM；手动删除成功（永久保留策略） |

### 3.4 规约 4「错题数据结构」→ TC-D

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-D-01 | 域 | 字段映射 round-trip | 表↔规约 JSON 一一映射（标准夹具快照） |
| TC-D-02 | 域 | 多图导出兼容 | questionImage=首个 QUESTION 角色图；questionImages 完整 |
| TC-D-03 | 接口 | 响应命名 | 字段 camelCase 与规约 JSON 一致 |

### 3.5 场景 1a「分科 Tab、模糊/知识点搜索、分页、查询重置」→ TC-L

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-L-01 | 接口 | 科目筛选 | 仅返回该科目 CONFIRMED（草稿不出现） |
| TC-L-02 | 接口 | 题干模糊搜索 | 命中/不命中/空关键词 |
| TC-L-03 | 接口 | 知识点搜索 | LIKE 命中任一知识点 |
| TC-L-04 | 接口 | 分页边界 | page=1 / 超尾页收敛 / size>50 收敛 50 |
| TC-L-05 | 接口 | 组合筛选 | 科目+题型+日期区间 |
| TC-L-06 | 组件 | 查询/重置 | 查询重置 page=1；重置清空条件并刷新 |
| TC-L-07 | 组件 | 分科 Tab | 切换携带科目重新查询 |

### 3.6 场景 1b「新增独立页、拖拽多图、解析分项确认后录入列表」→ TC-N

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-N-01 | 组件 | 独立页面 | 路由 /questions/new（非弹窗）；返回列表 |
| TC-N-02 | 组件 | 三步流状态机 | 上传→解析→确认步骤条联动；返回上一步保留数据 |
| TC-N-03 | 组件 | 分项确认交互 | 全字段可编辑；答案增删；知识点回车加/×删 |
| TC-N-04 | 组件 | 确认必填 | 题干空阻断并提示 |
| TC-N-05 | 手工 | 全链路 E2E | 上传多图→解析→确认→列表可见 |
| TC-N-06 | 组件 | 编辑模式重新解析删旧（2026-11-12 补） | 换图重新解析→confirm 新记录成功→联动 DELETE 旧记录（契约 3.3 注记） |
| TC-N-07 | 组件 | 仅题目图直接解析（FR-A7，2026-11-13 补） | 单张题目图（无 ANSWER_SHEET）→解析按钮可用→parse 请求仅含 QUESTION 角色→答案/解析为空的草稿可不经补全直接确认 |

### 3.7 `.claude.md` 功能项与约束「导出、迁移、AI 边界」→ TC-E

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-E-01 | 域 | Markdown 导出 | 与规约 JSON 结构一致（golden 文件比对） |
| TC-E-02 | 域 | PDF 重做模式 | 正文无答案；答案页在文末 |
| TC-E-03 | 接口 | 导出文件流 | Content-Type / Content-Disposition 正确 |
| TC-E-04 | 域 | 备份 round-trip | 导出→导入数据等价（BR-08） |
| TC-E-05 | 域 | 导入幂等 | skip 跳过 / overwrite 覆盖；行级错误明细 |
| TC-E-06 | 域 | 导入校验 | 非 JSON → 43001；字段不合法 → 43002 |
| TC-E-07 | 走查 | AI 边界（BR-01） | AiParseService 引用面仅解析用例（评审 checklist） |

### 3.8 安全设计 → TC-S（摘自 security.md 检查清单）

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-S-01 | 域 | 路径穿越 | relativePath 含 `../` → 拒绝 |
| TC-S-02 | 前端单测 | XSS 消毒 | 题干含 `<script>`/`<img onerror>` → DOMPurify 输出转义 |
| TC-S-03 | 域 | 日志脱敏 | 任何级别日志无 api-key |
| TC-S-04 | 接口 | 导入大小 | >20MB 拒绝 |

### 3.9 规约 5「切面日志」（2026-11-11 追加）→ TC-G

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-G-01 | 域 | 切面记录成功请求 | module=类#方法、业务返回全文、success=true、errorCode=0、耗时>0 |
| TC-G-02 | 域 | 业务异常路径 | success=false + 对应错误码入库（mock 抛 BusinessException） |
| TC-G-03 | 域 | AI 返回记录 | recordAi 保存模型返回**原文**（responseContent 完整）、logType=AI_CALL、action 含模型名 |
| TC-G-04 | 域 | 日志入库失败不影响业务 | mapper.insert 抛异常 → 业务方法正常返回、仅 logback warn |
| TC-G-05 | 接口 | 日志查询分页筛选 | logType/success/时间过滤生效；查询接口自身不产生日志（防递归） |

### 3.10 标签归类（FR-B6/B7，2026-11-11 17:05 追加）→ TC-T

| 编号 | 层级 | 用例 | 验证点 |
|---|---|---|---|
| TC-T-01 | 域 | confirm/edit 保存标签 | tags JSON 数组持久化；重复标签去重 |
| TC-T-02 | 接口 | 列表标签筛选 | tag 模糊命中任一标签；空 tag 不过滤 |
| TC-T-03 | 组件 | 录入/编辑标签交互 | 回车添加、✕ 删除；与知识点区互不影响（语义独立） |
| TC-T-04 | 域 | AI 草稿标签恒为空 | parse 返回草稿 tags=[]（BR-11：手工标注，AI 不自动生成） |

## 4. 业务域单元测试示例（不依赖具体实现）

```java
// TC-P-01 / TC-P-02 规约2：解析编排——mock AI 接口，无 Feign/无库/无容器
// ⚠ v3.0 双服务拆分注记（2026-11-12）：「失败重试 1 次」已收敛于 AI 服务防腐层
// OpenAiVisionAdapter（A-3，对 BE 单次调用即返回 42001/42002）；下例重试验证
// （verify times(2)）对应 AI 服务侧用例，BE ParseService 对内部契约为单次调用。
@ExtendWith(MockitoExtension.class)
class ParseServiceTest {
    @Mock AiParseService ai;              // 领域接口替身
    @Mock WrongQuestionMapper mapper;
    @Mock Clock clock;                    // 时间可注入
    @InjectMocks ParseServiceImpl service;

    @Test void 解析成功_草稿完整映射并入库待确认() {   // TC-P-01, TC-P-05
        when(ai.parse(any())).thenReturn(fixtureDraft());   // notebook.md 标准夹具
        DraftDTO d = service.parse(images("q.jpg","a.jpg"));
        assertThat(d.getStatus()).isEqualTo(PENDING_CONFIRM);
        verify(mapper).insert(any());
    }
    @Test void AI首次非法输出_重试一次_仍失败抛42001() { // TC-P-02
        when(ai.parse(any())).thenThrow(new AiInvalidOutputException());
        assertThatThrownBy(() -> service.parse(images("q.jpg")))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(42001);
        verify(ai, times(2)).parse(any());
    }
}
```

```ts
// TC-U-01 规约1：图片压缩纯逻辑
it('长边超过 2000px 时等比压缩到 2000px', async () => {
  const img = await makeImage(4000, 3000);            // canvas 造图，无网络
  const out = await compress(img, { maxEdge: 2000 });
  expect(out.width).toBe(2000);
  expect(out.height).toBe(1500);
});
```

## 5. 测试数据与夹具

- `fixtures/notebook-sample.json`：规约 4 示例（物理题）全字段——TC-P-01/TC-D-01/TC-D-02/TC-E-01/TC-E-04 共用。
- `fixtures/backup-set.json`：5 条混合数据（三科、多图、多答案）用于导入导出与筛选。
- 前端 `makeImage(w,h)` canvas 工具生成指定尺寸图片，测试零外部资源。

## 6. 覆盖率门禁（JaCoCo，2026-11-11 16:04 评审通过；2026-11-12 随 v3.0 双工程复核）

```xml
<!-- mvn jacoco:check，纳入 pre-commit 链尾（BE 与 AI 服务两 Maven 工程各自配置） -->
<rule><element>BUNDLE</element><limit><counter>LINE</counter><value>0.70</value></limit></rule>
<rule><element>PACKAGE</element>
  <includes>com.errnote.service.impl.*,com.errnote.integration.ai.*</includes>  <!-- BE 工程 -->
  <limit><counter>LINE</counter><value>0.85</value></limit>
</rule>
<rule><element>PACKAGE</element>
  <includes>com.errnote.ai.acl.*,com.errnote.ai.client.*</includes>            <!-- AI 服务工程（2026-11-12 补） -->
  <limit><counter>LINE</counter><value>0.85</value></limit>
</rule>
```
- 指标：**总行覆盖 ≥70%；核心业务包 ≥85%**——v3.0 拆分后 **BE 与 error-notebook-ai 两个 Maven 工程分别达标**（防腐层/模型客户端属 AI 服务核心包）。
- 前端覆盖率：Vitest `--coverage`（v8 provider，内置）报告展示，暂不设门禁（演进预留）。

## 7. 手工 E2E 清单（不可自动化部分）

1. 全链路录入（TC-N-05）：真实图片→裁剪→AI 真实调用→确认→列表可见；
2. PDF 视觉检查：中文字体、公式渲染、重做模式排版；
3. 浏览器兼容：Chrome/Edge 最新版；
4. 灾备演练：拷贝 `data/` 目录到新机器→启动→数据与图片完整。
