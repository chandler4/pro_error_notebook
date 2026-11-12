/**
 * 原生 fetch 统一封装（api-contract §5，F-1）。
 *
 * - 统一解析 {code, message, data}；code !== 0 抛 ApiError(code, message)（调用方/全局 toast 处理）
 * - 超时默认 30s；/questions/parse 等长请求在调用处覆写 timeout = 120_000
 * - json 请求体自动序列化；文件上传用 form(FormData)；下载（导出/备份）走 downloadFile
 */

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export interface RequestOptions {
  /** JSON 请求体（自动设置 Content-Type: application/json） */
  json?: unknown
  /** FormData 请求体（上传；Content-Type 由浏览器携带 boundary） */
  form?: FormData
  /** 超时毫秒，默认 30s */
  timeout?: number
}

export async function request<T>(method: string, url: string, opts: RequestOptions = {}): Promise<T> {
  const { timeout = 30_000 } = opts
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)
  try {
    const headers: Record<string, string> = {}
    let body: BodyInit | undefined
    if (opts.json !== undefined) {
      headers['Content-Type'] = 'application/json'
      body = JSON.stringify(opts.json)
    } else if (opts.form !== undefined) {
      body = opts.form
    }
    const res = await fetch(url, { method, headers, body, signal: controller.signal })
    if (!res.ok) {
      throw new ApiError(res.status, `HTTP ${res.status} ${res.statusText}`.trim())
    }
    const payload = (await res.json()) as { code: number; message?: string; data: T }
    if (payload.code !== 0) {
      throw new ApiError(payload.code, payload.message || `业务错误 ${payload.code}`)
    }
    return payload.data
  } finally {
    clearTimeout(timer)
  }
}

/** 下载文件流（导出 Markdown/PDF、备份 JSON）并触发浏览器保存。 */
export async function downloadFile(url: string, filename: string): Promise<void> {
  const res = await fetch(url)
  if (!res.ok) {
    throw new ApiError(res.status, `HTTP ${res.status} ${res.statusText}`.trim())
  }
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}

/** 图片相对路径 → BE 静态访问 URL（架构 §5.4：GET /files/images/**）。 */
export function fileUrl(relativePath: string): string {
  return `/files/${relativePath.replace(/^\/+/, '')}`
}
