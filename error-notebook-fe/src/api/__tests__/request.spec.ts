import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { request, ApiError, downloadFile } from '../request'

/** F-1：request 封装（契约 §5：统一解析 {code,message,data}，code!==0 抛 ApiError）。 */
describe('request 封装', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
  })

  it('成功响应返回 data 字段', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ code: 0, message: 'OK', data: { id: 42 } }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    const data = await request<{ id: number }>('GET', '/api/v1/demo')
    expect(data).toEqual({ id: 42 })
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/v1/demo')
    expect(init.method).toBe('GET')
  })

  it('code!==0 时抛 ApiError(code, message)', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ code: 42001, message: 'AI 解析失败', data: null }), {
        status: 200,
      }),
    )
    await expect(request('POST', '/api/v1/questions/parse', { json: {} })).rejects.toMatchObject({
      name: 'ApiError',
      code: 42001,
      message: 'AI 解析失败',
    })
  })

  it('HTTP 非 2xx 抛 ApiError（HTTP 状态恒 200 约定外的防御）', async () => {
    fetchMock.mockResolvedValue(new Response('not found', { status: 404 }))
    await expect(request('GET', '/api/v1/none')).rejects.toMatchObject({
      name: 'ApiError',
      code: 404,
    })
  })

  it('json 请求体自动序列化并带 Content-Type', async () => {
    fetchMock.mockResolvedValue(okResponse())
    await request('POST', '/api/v1/x', { json: { a: 1 } })
    const [, init] = fetchMock.mock.calls[0]
    expect(init.headers['Content-Type']).toBe('application/json')
    expect(init.body).toBe('{"a":1}')
  })

  it('form 请求体直接透传 FormData', async () => {
    fetchMock.mockResolvedValue(okResponse())
    const fd = new FormData()
    fd.append('files', new Blob(['x']))
    await request('POST', '/api/v1/questions/parse-image', { form: fd })
    const [, init] = fetchMock.mock.calls[0]
    expect(init.body).toBe(fd)
    expect(init.headers['Content-Type']).toBeUndefined()
  })

  it('超时中止请求并抛错（默认 30s，此处用短超时验证机制）', async () => {
    fetchMock.mockImplementation(
      (_url: string, init?: RequestInit) =>
        new Promise((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () => {
            const e = new Error('The operation was aborted.')
            e.name = 'AbortError'
            reject(e)
          })
        }),
    )
    await expect(request('GET', '/api/v1/slow', { timeout: 30 })).rejects.toThrow()
  })

  it('downloadFile 下载 blob 并触发保存', async () => {
    const blob = new Blob(['hello'], { type: 'text/markdown' })
    fetchMock.mockResolvedValue(new Response(blob, { status: 200 }))
    const clickSpy = vi.fn()
    const anchor = { click: clickSpy, download: '', href: '' }
    const createSpy = vi.spyOn(document, 'createElement').mockImplementation(((tag: string) => {
      if (tag === 'a') return anchor as unknown as HTMLAnchorElement
      return document.createElement(tag)
    }) as typeof document.createElement)
    await downloadFile('/api/v1/questions/export?format=markdown', 'export.md')
    expect(fetchMock).toHaveBeenCalled()
    expect(clickSpy).toHaveBeenCalled()
    expect(anchor.download).toBe('export.md')
    createSpy.mockRestore()
  })
})

function okResponse(): Response {
  return new Response(JSON.stringify({ code: 0, message: 'OK', data: null }), { status: 200 })
}

// ApiError 形状自检（供类型与运行时双重确认）
it('ApiError 携带业务码', () => {
  const e = new ApiError(40000, '参数校验失败')
  expect(e).toBeInstanceOf(Error)
  expect(e.code).toBe(40000)
  expect(e.message).toBe('参数校验失败')
})
