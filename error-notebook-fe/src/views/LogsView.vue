<script setup lang="ts">
// P3 运行日志（prototype-spec §2.3）：类型/模块/成败/日期区间四维筛选、徽章表格、详情抽屉全文展示。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listLogs } from '../api/logs'
import type { LogEntry } from '../types'

const loading = ref(false)
const rows = ref<LogEntry[]>([])
const total = ref(0)
const page = ref(1)
const size = 10

const filter = reactive({
  logType: '' as '' | 'OPERATION' | 'AI_CALL',
  module: '',
  success: '' as '' | 'true' | 'false',
  dateRange: [] as string[],
})

const typeLabel: Record<string, string> = { OPERATION: '业务操作', AI_CALL: 'AI 调用' }

function buildQuery() {
  return {
    logType: filter.logType || undefined,
    module: filter.module || undefined,
    success: filter.success === '' ? undefined : filter.success === 'true',
    dateFrom: filter.dateRange?.[0] || undefined,
    dateTo: filter.dateRange?.[1] || undefined,
    page: page.value,
    size,
  }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await listLogs(buildQuery())
    rows.value = res.list
    total.value = res.total
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    loading.value = false
  }
}

function onSearch(): void {
  page.value = 1
  void load()
}

function onReset(): void {
  filter.logType = ''
  filter.module = ''
  filter.success = ''
  filter.dateRange = []
  page.value = 1
  void load()
}

function onPageChange(p: number): void {
  page.value = p
  void load()
}

// ---- 详情抽屉 ----
const drawerVisible = ref(false)
const detail = ref<LogEntry | null>(null)

function onView(row: LogEntry): void {
  detail.value = row
  drawerVisible.value = true
}

async function copyText(text: string | undefined): Promise<void> {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

onMounted(() => {
  void load()
})
</script>

<template>
  <div class="page">
    <h1 class="page-title">运行日志</h1>

    <el-form inline class="search" @submit.prevent>
      <el-form-item label="类型">
        <el-select v-model="filter.logType" placeholder="全部" clearable style="width: 130px">
          <el-option label="业务操作" value="OPERATION" />
          <el-option label="AI 调用" value="AI_CALL" />
        </el-select>
      </el-form-item>
      <el-form-item label="模块">
        <el-input v-model="filter.module" placeholder="模块名模糊匹配" clearable />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="filter.success" placeholder="全部" clearable style="width: 110px">
          <el-option label="成功" value="true" />
          <el-option label="失败" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker
          v-model="filter.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <span class="log-badge" :class="`log-badge-${row.logType}`">{{ typeLabel[row.logType] ?? row.logType }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="module" label="模块" min-width="180" />
      <el-table-column prop="action" label="操作描述" min-width="160" />
      <el-table-column label="结果" width="80">
        <template #default="{ row }">
          <span :class="row.success ? 'ok' : 'fail'">{{ row.success ? '✓ 成功' : '✗ 失败' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="errorCode" label="错误码" width="90" />
      <el-table-column label="耗时" width="90">
        <template #default="{ row }">{{ row.durationMs != null ? `${row.durationMs}ms` : '—' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="170" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="onView(row)">查看</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :image-size="60" description="暂无日志" />
      </template>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="size"
      :current-page="page"
      @current-change="onPageChange"
    />

    <el-drawer v-model="drawerVisible" title="日志详情" size="46%">
      <div v-if="detail" class="log-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="TraceId">{{ detail.traceId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeLabel[detail.logType] ?? detail.logType }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.module || '—' }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ detail.action || '—' }}</el-descriptions-item>
          <el-descriptions-item label="结果">{{ detail.success ? '成功' : `失败（${detail.errorCode ?? '—'}）` }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ detail.durationMs != null ? `${detail.durationMs}ms` : '—' }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="sec">请求内容</h3>
        <div class="pre-box">
          <el-button size="small" class="copy-btn" @click="copyText(detail.requestContent)">复制</el-button>
          <pre class="mono">{{ detail.requestContent || '（空）' }}</pre>
        </div>

        <h3 class="sec">返回内容</h3>
        <div class="pre-box">
          <el-button size="small" class="copy-btn" @click="copyText(detail.responseContent)">复制</el-button>
          <pre class="mono">{{ detail.responseContent || '（空）' }}</pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 18px;
  margin: 0 0 12px;
}
.search {
  margin-bottom: 8px;
}
.log-badge {
  display: inline-block;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 3px;
}
/* OPERATION 蓝 / AI_CALL 橙（prototype-spec §2.3） */
.log-badge-OPERATION {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.log-badge-AI_CALL {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}
.ok {
  color: var(--el-color-success);
}
.fail {
  color: var(--el-color-danger);
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
.sec {
  font-size: 14px;
  margin: 16px 0 6px;
}
.pre-box {
  position: relative;
}
.copy-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 1;
}
.mono {
  margin: 0;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow: auto;
}
</style>
