import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { QuestionQuery } from '../types'

/**
 * 错题列表筛选状态（F-1/F-2/F-5）：列表页与导出弹窗共享——
 * 导出弹窗读取当前筛选生成条件摘要（prototype-spec §2.6）。
 */
export const useListStore = defineStore('question-list', () => {
  const filters = ref<QuestionQuery>({ page: 1, size: 10 })
  const total = ref(0)

  function setFilters(patch: Partial<QuestionQuery>): void {
    filters.value = { ...filters.value, ...patch }
  }

  /** 重置条件并回第一页（TC-L-06）。 */
  function reset(): void {
    filters.value = { page: 1, size: 10 }
  }

  /** 条件摘要文案（导出弹窗展示）。 */
  function summaryText(subjectLabel?: (code: string) => string): string {
    const f = filters.value
    const parts: string[] = []
    if (f.subject) parts.push(`科目=${subjectLabel?.(f.subject) ?? f.subject}`)
    if (f.keyword) parts.push(`题干含「${f.keyword}」`)
    if (f.knowledgePoint) parts.push(`知识点含「${f.knowledgePoint}」`)
    if (f.tag) parts.push(`标签含「${f.tag}」`)
    if (f.questionType) parts.push(`题型=${f.questionType}`)
    if (f.dateFrom) parts.push(`从 ${f.dateFrom}`)
    if (f.dateTo) parts.push(`至 ${f.dateTo}`)
    return parts.length ? parts.join('；') : '全部错题（无筛选条件）'
  }

  return { filters, total, setFilters, reset, summaryText }
})
