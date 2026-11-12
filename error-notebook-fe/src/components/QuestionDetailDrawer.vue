<script setup lang="ts">
// 详情抽屉（prototype-spec §2.5）：错题查看。解析经 renderMarkdown 消毒后由 HtmlView 渲染。
import { computed } from 'vue'
import type { WrongQuestion } from '../types'
import { fileUrl } from '../api/request'
import { renderMarkdown } from '../utils/markdown'
import HtmlView from './HtmlView.vue'

const props = defineProps<{
  modelValue: boolean
  question: WrongQuestion | null
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const roleLabel: Record<string, string> = { QUESTION: '题目图', ANSWER_SHEET: '解答图' }

const analysisHtml = computed(() => renderMarkdown(props.question?.knowledgeAnalysis ?? ''))
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    title="错题详情"
    size="46%"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="question" class="detail">
      <!-- 图片区：多图横向 + 角色角标 -->
      <div v-if="question.questionImages?.length" class="img-row">
        <div v-for="(img, i) in question.questionImages" :key="i" class="img-cell">
          <span class="role-badge" :class="img.role">{{ roleLabel[img.role] ?? img.role }}</span>
          <img :src="fileUrl(img.url)" :alt="`错题图片${i + 1}`" />
        </div>
      </div>

      <h3 class="sec">题干</h3>
      <p class="stem">{{ question.questionStem }}</p>

      <template v-if="question.standardAnswer?.length">
        <h3 class="sec">标准答案</h3>
        <ol class="answers">
          <li v-for="(a, i) in question.standardAnswer" :key="i">{{ a }}</li>
        </ol>
      </template>

      <h3 class="sec">知识点 / 标签</h3>
      <div class="tags">
        <el-tag v-for="kp in question.knowledgePoints ?? []" :key="kp" size="small">{{ kp }}</el-tag>
        <span v-for="t in question.tags ?? []" :key="t" class="free-tag">{{ t }}</span>
      </div>

      <template v-if="question.knowledgeAnalysis">
        <h3 class="sec">解析</h3>
        <HtmlView :html="analysisHtml" />
      </template>

      <h3 class="sec">元信息</h3>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="科目">{{ question.subject }}</el-descriptions-item>
        <el-descriptions-item label="年级">{{ question.gradeLevel || '—' }}</el-descriptions-item>
        <el-descriptions-item label="题型">{{ question.questionType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ question.sourceName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="题号">{{ question.questionNumber || '—' }}</el-descriptions-item>
        <el-descriptions-item label="错题日期">{{ question.wrongDate || '—' }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <el-empty v-else description="加载中……" />
  </el-drawer>
</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sec {
  font-size: 14px;
  margin: 12px 0 4px;
  color: var(--el-text-color-primary);
}
.stem {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}
.img-row {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.img-cell {
  position: relative;
  flex: 0 0 auto;
}
.img-cell img {
  height: 140px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-light);
  display: block;
}
.role-badge {
  position: absolute;
  left: 0;
  bottom: 0;
  font-size: 12px;
  padding: 1px 6px;
  border-radius: 0 4px 0 0;
  color: #fff;
  background: var(--el-color-primary);
  z-index: 1;
}
.role-badge.ANSWER_SHEET {
  background: var(--el-color-success);
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
/* 自由标签：灰底虚线，与知识点样式区分（prototype-spec P1 表格同规则） */
.free-tag {
  font-size: 12px;
  padding: 2px 8px;
  border: 1px dashed var(--el-border-color);
  border-radius: 3px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}
.answers {
  margin: 0;
  padding-left: 20px;
}
</style>
