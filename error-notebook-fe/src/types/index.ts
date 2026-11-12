/** 规约 DTO 类型（与 api-contract v1.4 §3 对齐；字段命名 camelCase 与规约 JSON 一致）。 */

export type QuestionStatus = 'PENDING_CONFIRM' | 'CONFIRMED'

export type ImageRole = 'QUESTION' | 'ANSWER_SHEET'

export interface ImageRef {
  /** 相对路径，如 images/202611/a1b2.jpg；展示时拼 /files/ 前缀（BE 静态映射） */
  url: string
  role: ImageRole
}

/** 错题/草稿详情（契约 3.4）。 */
export interface WrongQuestion {
  errorQuestionId: string
  subject: string
  gradeLevel?: string
  questionType?: string
  questionStem: string
  questionImages: ImageRef[]
  sourceName?: string
  questionNumber?: string
  standardAnswer?: string[]
  knowledgePoints?: string[]
  /** 自由文本标签（BR-11：手工标注，与知识点语义独立） */
  tags?: string[]
  knowledgeAnalysis?: string
  wrongDate?: string
  status: QuestionStatus
  createdAt?: string
  updatedAt?: string
}

/** confirm/PUT 请求体：人工确认后的完整字段（必填 subject、questionStem）。 */
export interface QuestionDraftInput {
  subject: string
  gradeLevel?: string
  questionType?: string
  questionStem: string
  questionImages: ImageRef[]
  sourceName?: string
  questionNumber?: string
  standardAnswer?: string[]
  knowledgePoints?: string[]
  tags?: string[]
  knowledgeAnalysis?: string
  wrongDate?: string
}

/** 分页响应（契约 §1：data: {list, total, page, size}）。 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 列表筛选（契约 3.5 查询参数）。 */
export interface QuestionQuery {
  subject?: string
  keyword?: string
  knowledgePoint?: string
  tag?: string
  questionType?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

/** 导出参数（契约 3.9：format/rework + 3.5 全部筛选参数）。 */
export interface ExportQuery extends QuestionQuery {
  format: 'markdown' | 'pdf'
  rework?: boolean
}

/** 枚举字典（契约 3.11）。 */
export interface EnumsData {
  subjects: string[]
  gradeLevels: string[]
  questionTypes: string[]
  subjectTabs: string[]
}

/** 知识点归并候选（契约 3.11，use_count 降序）。 */
export interface KnowledgePointCandidate {
  name: string
  useCount: number
}

/** 业务日志条目（契约 3.12）。 */
export interface LogEntry {
  id: number
  traceId?: string
  logType: 'OPERATION' | 'AI_CALL'
  module?: string
  action?: string
  requestContent?: string
  responseContent?: string
  success: boolean
  errorCode?: number
  durationMs?: number
  createdAt?: string
}

export interface LogQuery {
  logType?: 'OPERATION' | 'AI_CALL'
  module?: string
  success?: boolean
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
}

/** 备份导入结果（契约 3.10）。 */
export interface ImportResult {
  total: number
  inserted: number
  skipped: number
  overwritten: number
  errors: Array<{ line: number; reason: string }>
}
