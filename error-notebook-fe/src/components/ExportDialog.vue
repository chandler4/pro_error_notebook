<script setup lang="ts">
// 导出弹窗（prototype-spec §2.6，P1 工具栏入口）：格式单选 + 重做开关（仅 PDF）+ 当前筛选摘要。
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportQuestions } from '../api/question'
import { useListStore } from '../store/list'
import type { ExportQuery } from '../types'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const listStore = useListStore()

const format = ref<'markdown' | 'pdf'>('markdown')
const rework = ref(false)
const exporting = ref(false)

/** 当前筛选摘要（与列表页共享 listStore）。 */
const summary = computed(() => listStore.summaryText())

function onFormatChange(v: string | number | boolean | undefined): void {
  format.value = (v === 'pdf' ? 'pdf' : 'markdown')
  if (format.value !== 'pdf') rework.value = false // 重做仅对 PDF 有意义
}

function close(): void {
  emit('update:modelValue', false)
}

async function onExport(): Promise<void> {
  exporting.value = true
  try {
    const query: ExportQuery = {
      ...listStore.filters,
      format: format.value,
      rework: format.value === 'pdf' && rework.value ? true : undefined,
    }
    await exportQuestions(query)
    ElMessage.success('导出已开始下载')
    close()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="导出错题"
    width="480px"
    @update:model-value="(v: boolean) => (v || close())"
  >
    <el-form label-width="90px" @submit.prevent>
      <el-form-item label="导出格式">
        <el-radio-group :model-value="format" @update:model-value="onFormatChange">
          <el-radio value="markdown">Markdown</el-radio>
          <el-radio value="pdf">PDF</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="重做模式">
        <el-switch v-model="rework" :disabled="format !== 'pdf'" />
        <div class="tip">仅 PDF 支持：遮盖答案生成重做卷，答案页置尾</div>
      </el-form-item>
      <el-form-item label="筛选范围">
        <div class="summary">{{ summary }}</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="exporting" @click="onExport">导出</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.4;
}
.summary {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
