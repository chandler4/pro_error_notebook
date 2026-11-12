/**
 * 错题域接口模块（api-contract §3.1~3.10，F-1）。
 * parse 长请求覆写 timeout 120s（契约 §1：前端超时 120s）。
 */
import { request, downloadFile } from './request'
import type {
  ExportQuery,
  ImageRef,
  ImportResult,
  PageResult,
  QuestionDraftInput,
  QuestionQuery,
  WrongQuestion,
} from '../types'

/** 3.1 上传处理图（前端已压缩+裁剪）；返回相对路径列表。 */
export function parseImage(files: File[]): Promise<string[]> {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  return request('POST', '/api/v1/questions/parse-image', { form })
}

/** 3.8 创建空草稿（FR-A4 转手动填写，零 AI 调用）。 */
export function createEmptyDraft(): Promise<WrongQuestion> {
  return request('POST', '/api/v1/questions/drafts', { json: {} })
}

/** 3.2 AI 解析（同步等待，超时 120s）：草稿入库 PENDING_CONFIRM 并返回。 */
export function parseQuestion(images: ImageRef[]): Promise<WrongQuestion> {
  return request('POST', '/api/v1/questions/parse', { json: { images }, timeout: 120_000 })
}

/** 3.3 确认入库（必填 subject、questionStem；知识点归并在 BE 完成）。 */
export function confirmQuestion(id: string, payload: QuestionDraftInput): Promise<WrongQuestion> {
  return request('POST', `/api/v1/questions/${encodeURIComponent(id)}/confirm`, { json: payload })
}

/** 3.4 错题/草稿详情。 */
export function getQuestion(id: string): Promise<WrongQuestion> {
  return request('GET', `/api/v1/questions/${encodeURIComponent(id)}`)
}

/** 3.5 分页筛选（仅 CONFIRMED）。 */
export function listQuestions(query: QuestionQuery): Promise<PageResult<WrongQuestion>> {
  return request('GET', `/api/v1/questions?${toQueryString(query)}`)
}

/** 3.6 编辑（全字段，行为同 confirm：更新 + 知识点归并累积）。 */
export function updateQuestion(id: string, payload: QuestionDraftInput): Promise<WrongQuestion> {
  return request('PUT', `/api/v1/questions/${encodeURIComponent(id)}`, { json: payload })
}

/** 3.7 硬删除（记录+图片联动删，二次确认由前端交互承担）。 */
export function deleteQuestion(id: string): Promise<null> {
  return request('DELETE', `/api/v1/questions/${encodeURIComponent(id)}`)
}

/** 3.8 待确认草稿列表（PENDING_CONFIRM，创建倒序）。 */
export function listDrafts(page = 1, size = 10): Promise<PageResult<WrongQuestion>> {
  return request('GET', `/api/v1/questions/drafts?page=${page}&size=${size}`)
}

/** 3.8 手动删除草稿（永久保留策略；不删图片）。 */
export function deleteDraft(id: string): Promise<null> {
  return request('DELETE', `/api/v1/questions/drafts/${encodeURIComponent(id)}`)
}

/** 3.9 导出（文件流下载；rework 仅 PDF 有意义）。 */
export async function exportQuestions(query: ExportQuery): Promise<void> {
  const { format, ...rest } = query
  const qs = toQueryString({ ...rest, format, rework: format === 'pdf' && rest.rework ? 'true' : undefined })
  const ext = format === 'pdf' ? 'pdf' : 'md'
  await downloadFile(`/api/v1/questions/export?${qs}`, `错题本导出.${ext}`)
}

/** 3.10 备份导出（全量 CONFIRMED，规约 JSON 数组）。 */
export function backupExport(): Promise<void> {
  return downloadFile('/api/v1/backup/export', '错题本备份.json')
}

/** 3.10 备份导入（mode=skip|overwrite；返回行级结果报告）。 */
export function backupImport(file: File, mode: 'skip' | 'overwrite'): Promise<ImportResult> {
  const form = new FormData()
  form.append('file', file)
  return request('POST', `/api/v1/backup/import?mode=${mode}`, { form, timeout: 120_000 })
}

/** 拼接查询串（跳过空值）。 */
export function toQueryString(query: object): string {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') params.append(k, String(v))
  })
  return params.toString()
}
