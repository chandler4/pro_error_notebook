<script setup lang="ts">
// P4 数据备份（prototype-spec §2.4）：全量 JSON 导出 + 导入（skip|overwrite）+ 行级结果报告。
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { backupExport, backupImport } from '../api/question'
import type { ImportResult } from '../types'

const MAX_SIZE = 20 * 1024 * 1024

const exporting = ref(false)
const importing = ref(false)

// ---- 导入 ----
const fileInput = ref<HTMLInputElement | null>(null)
const file = ref<File | null>(null)
const mode = ref<'skip' | 'overwrite'>('skip')
const result = ref<ImportResult | null>(null)

function pickFile(): void {
  fileInput.value?.click()
}

function onFileChange(e: Event): void {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0]
  if (!f) return
  if (f.size > MAX_SIZE) {
    ElMessage.warning('文件超过 20MB 上限')
    input.value = ''
    file.value = null
    return
  }
  file.value = f
  result.value = null
}

async function onImport(): Promise<void> {
  if (!file.value) {
    ElMessage.warning('请先选择备份 JSON 文件')
    return
  }
  importing.value = true
  try {
    result.value = await backupImport(file.value, mode.value)
    ElMessage.success('导入完成')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    importing.value = false
  }
}

async function onExport(): Promise<void> {
  exporting.value = true
  try {
    await backupExport()
    ElMessage.success('备份文件已开始下载')
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="page">
    <h1 class="page-title">数据备份</h1>

    <el-row :gutter="16">
      <!-- 导出卡片 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>备份导出</template>
          <p class="card-text">
            导出全量 JSON 文件（结构与规约一致，含全部已确认错题）。
            图片文件不包含在备份内，恢复后按服务端存储路径引用。
          </p>
          <el-button type="primary" :loading="exporting" @click="onExport">导出全量 JSON</el-button>
        </el-card>
      </el-col>

      <!-- 导入卡片 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>备份导入</template>
          <div class="import-box">
            <el-button @click="pickFile">{{ file ? file.name : '选择 JSON 文件' }}</el-button>
            <span class="hint">≤20MB，规约 JSON 数组格式</span>
            <input ref="fileInput" type="file" accept=".json,application/json" @change="onFileChange" />
          </div>
          <el-form label-width="90px" class="mode-form" @submit.prevent>
            <el-form-item label="冲突策略">
              <el-radio-group v-model="mode">
                <el-radio value="skip">跳过已存在（skip）</el-radio>
                <el-radio value="overwrite">覆盖更新（overwrite）</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <el-button type="primary" :loading="importing" @click="onImport">开始导入</el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 结果报告 -->
    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>导入结果</template>
      <div class="counters">
        <div class="counter"><b>{{ result.total }}</b><span>总数</span></div>
        <div class="counter ok"><b>{{ result.inserted }}</b><span>新增</span></div>
        <div class="counter skip"><b>{{ result.skipped }}</b><span>跳过</span></div>
        <div class="counter over"><b>{{ result.overwritten }}</b><span>覆盖</span></div>
      </div>

      <template v-if="result.errors.length">
        <h3 class="sec">行级错误明细</h3>
        <el-table :data="result.errors" size="small">
          <el-table-column prop="line" label="行号" width="90" />
          <el-table-column prop="reason" label="原因" min-width="300" />
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 18px;
  margin: 0 0 12px;
}
.card-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
  margin: 0 0 12px;
}
.import-box {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.import-box input[type='file'] {
  display: none;
}
.hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.mode-form {
  margin-bottom: 4px;
}
.result-card {
  margin-top: 16px;
}
.counters {
  display: flex;
  gap: 40px;
}
.counter {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.counter b {
  font-size: 24px;
}
.counter span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.counter.ok b {
  color: var(--el-color-success);
}
.counter.skip b {
  color: var(--el-color-warning);
}
.counter.over b {
  color: var(--el-color-primary);
}
.sec {
  font-size: 14px;
  margin: 16px 0 8px;
}
</style>
