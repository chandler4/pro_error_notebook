<script setup lang="ts">
// P1 错题记录（prototype-spec §2）：分科 Tab + 待确认入口 + 搜索区 + 表格 + 分页 + 详情抽屉。
// 筛选状态存 listStore（与导出弹窗共享摘要）。
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listQuestions,
  listDrafts,
  deleteQuestion,
  deleteDraft,
  getQuestion,
} from '../api/question'
import { getEnums } from '../api/meta'
import { useListStore } from '../store/list'
import type { WrongQuestion } from '../types'
import QuestionDetailDrawer from '../components/QuestionDetailDrawer.vue'

const router = useRouter()
const listStore = useListStore()

// ---- 分科 Tab ----
const subjectTabs = ref<string[]>([])
const activeTab = ref('')
const FALLBACK_TABS = ['数学', '物理', '化学']

// ---- 搜索区（TC-L-06）----
const search = reactive({ keyword: '', knowledgePoint: '', tag: '' })

// ---- 主列表 ----
const loading = ref(false)
const rows = ref<WrongQuestion[]>([])
const total = ref(0)
const size = 10

// ---- 待确认草稿 ----
const draftMode = ref(false)
const draftLoading = ref(false)
const draftRows = ref<WrongQuestion[]>([])
const draftTotal = ref(0)
const draftPage = ref(1)

// ---- 详情抽屉 ----
const drawerVisible = ref(false)
const detail = ref<WrongQuestion | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listQuestions({ ...listStore.filters })
    rows.value = res.list
    total.value = res.total
    listStore.total = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

async function loadDraftsCount(): Promise<void> {
  try {
    const res = await listDrafts(1, 1)
    draftTotal.value = res.total
  } catch {
    draftTotal.value = 0 // 徽标失败不阻断主列表
  }
}

async function loadDrafts(): Promise<void> {
  draftLoading.value = true
  try {
    const res = await listDrafts(draftPage.value, size)
    draftRows.value = res.list
    draftTotal.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    draftLoading.value = false
  }
}

function onTabChange(tab: string | number): void {
  activeTab.value = String(tab)
  listStore.setFilters({ subject: activeTab.value, page: 1 })
  void load()
}

/** 查询：携带搜索条件并回第一页（TC-L-06）。 */
function onSearch(): void {
  listStore.setFilters({ ...search, page: 1 })
  void load()
}

/** 重置：清空条件并回第一页刷新（TC-L-06；分科 Tab 保留）。 */
function onReset(): void {
  search.keyword = ''
  search.knowledgePoint = ''
  search.tag = ''
  listStore.reset()
  listStore.setFilters({ subject: activeTab.value })
  void load()
}

function onPageChange(page: number): void {
  listStore.setFilters({ page })
  void load()
}

function onDraftPageChange(page: number): void {
  draftPage.value = page
  void loadDrafts()
}

function toggleDraftMode(): void {
  draftMode.value = !draftMode.value
  if (draftMode.value) void loadDrafts()
}

async function onView(row: WrongQuestion): Promise<void> {
  detail.value = null
  drawerVisible.value = true
  try {
    detail.value = await getQuestion(row.errorQuestionId)
  } catch (e) {
    ElMessage.error((e as Error).message)
    drawerVisible.value = false
  }
}

function onEdit(row: WrongQuestion): void {
  void router.push(`/questions/new?id=${encodeURIComponent(row.errorQuestionId)}`)
}

async function onDelete(row: WrongQuestion): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后记录与图片将一并移除，且不可恢复。确认删除？', '删除确认', {
      type: 'warning',
    })
  } catch {
    return // 取消
  }
  try {
    await deleteQuestion(row.errorQuestionId)
    ElMessage.success('已删除')
    void load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

async function onDeleteDraft(row: WrongQuestion): Promise<void> {
  try {
    await ElMessageBox.confirm('草稿删除后不可恢复。确认删除？', '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteDraft(row.errorQuestionId)
    ElMessage.success('草稿已删除')
    void loadDrafts()
    void loadDraftsCount()
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}

/** 导出弹窗在 F-5（备份/导出模块）接入，当前给出占位提示。 */
function onExport(): void {
  ElMessage.info('导出弹窗将随备份模块（F-5）提供')
}

onMounted(async () => {
  try {
    const enums = await getEnums()
    subjectTabs.value = enums.subjectTabs?.length ? enums.subjectTabs : FALLBACK_TABS
  } catch {
    subjectTabs.value = FALLBACK_TABS
  }
  activeTab.value = subjectTabs.value[0]
  listStore.setFilters({ subject: activeTab.value, page: 1 })
  void load()
  void loadDraftsCount()
})
</script>

<template>
  <div class="page">
    <!-- ===== 主视图（错题列表） ===== -->
    <template v-if="!draftMode">
      <div class="head-row">
        <h1 class="page-title">错题记录</h1>
        <el-button link type="warning" @click="toggleDraftMode">待确认 ({{ draftTotal }})</el-button>
      </div>

      <el-tabs :model-value="activeTab" @tab-change="onTabChange">
        <el-tab-pane v-for="t in subjectTabs" :key="t" :label="t" :name="t" />
      </el-tabs>

      <el-form inline class="search" @submit.prevent>
        <el-form-item label="题干">
          <el-input v-model="search.keyword" placeholder="题干关键词" clearable />
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="search.knowledgePoint" placeholder="知识点" clearable />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="search.tag" placeholder="标签" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="router.push('/questions/new')">＋ 新增错题</el-button>
        <el-button @click="onExport">导出</el-button>
      </div>

      <el-table v-loading="loading" :data="rows">
        <el-table-column label="题干" min-width="240">
          <template #default="{ row }">
            <div class="stem-clamp">{{ row.questionStem }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="questionType" label="题型" width="90" />
        <el-table-column label="知识点 / 标签" min-width="160">
          <template #default="{ row }">
            <div class="kp-cell">
              <el-tag v-for="kp in row.knowledgePoints ?? []" :key="kp" size="small" class="kp-tag">{{ kp }}</el-tag>
              <span v-for="t in row.tags ?? []" :key="t" class="free-tag">{{ t }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源 / 题号" min-width="130">
          <template #default="{ row }">
            <span>{{ row.sourceName || '—' }} {{ row.questionNumber ? `· ${row.questionNumber}` : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="wrongDate" label="错题日期" width="110" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :image-size="60" description="暂无符合条件的错题，点击右上角新增录入">
            <el-button type="primary" @click="router.push('/questions/new')">去新增</el-button>
          </el-empty>
        </template>
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="listStore.filters.page"
        @current-change="onPageChange"
      />
    </template>

    <!-- ===== 草稿视图（待确认入口切换） ===== -->
    <template v-else>
      <div class="head-row">
        <h1 class="page-title">待确认草稿</h1>
        <el-button link type="primary" @click="toggleDraftMode">返回错题列表</el-button>
      </div>

      <el-table v-loading="draftLoading" :data="draftRows">
        <el-table-column label="题干预览" min-width="300">
          <template #default="{ row }">
            <div class="stem-clamp">{{ row.questionStem || '（空白草稿——转手动填写）' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row)">继续编辑</el-button>
            <el-button link type="danger" @click="onDeleteDraft(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :image-size="60" description="暂无待确认草稿" />
        </template>
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="draftTotal"
        :page-size="size"
        :current-page="draftPage"
        @current-change="onDraftPageChange"
      />
    </template>

    <QuestionDetailDrawer v-model="drawerVisible" :question="detail" />
  </div>
</template>

<style scoped>
.page-title {
  font-size: 18px;
  margin: 0;
}
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.search {
  margin-bottom: 4px;
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
/* 题干两行截断（prototype-spec P1 表格） */
.stem-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  word-break: break-word;
}
.kp-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.kp-tag {
  margin: 0;
}
/* 自由标签：灰底虚线区分知识点（FR-B6） */
.free-tag {
  font-size: 12px;
  padding: 1px 6px;
  border: 1px dashed var(--el-border-color);
  border-radius: 3px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
