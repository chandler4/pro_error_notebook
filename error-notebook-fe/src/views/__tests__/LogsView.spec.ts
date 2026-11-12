import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElSelect } from 'element-plus'
import LogsView from '../LogsView.vue'
import { listLogs } from '../../api/logs'
import type { LogEntry, PageResult } from '../../types'

vi.mock('../../api/logs', () => ({ listLogs: vi.fn() }))

const mocked = vi.mocked(listLogs)

function makeEntry(id: number, over: Partial<LogEntry> = {}): LogEntry {
  return {
    id,
    traceId: `t-${id}`,
    logType: 'OPERATION',
    module: 'QuestionController#create',
    action: '确认入库',
    requestContent: `{"errorQuestionId":"q${id}"}`,
    responseContent: `{"code":0,"message":"OK"}`,
    success: true,
    errorCode: 0,
    durationMs: 42,
    createdAt: '2026-11-01 10:00:00',
    ...over,
  }
}

function pageOf(list: LogEntry[], total = list.length): PageResult<LogEntry> {
  return { list, total, page: 1, size: 10 }
}

beforeEach(() => {
  vi.clearAllMocks()
  mocked.mockResolvedValue(pageOf([makeEntry(1), makeEntry(2, { logType: 'AI_CALL', success: false, errorCode: 42001, action: 'glm-5.3-flash 图片解析', module: 'AiService#parse' })]))
})

function mountView() {
  return mount(LogsView, { global: { plugins: [ElementPlus] } })
}

/** 筛选：类型/模块/成败 查询重置（prototype-spec §2.3 / TC-G-05 前端侧）。 */
describe('LogsView 筛选', () => {
  it('初始全量查询；类型+模块+成败组合查询回第一页；重置清空', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(mocked).toHaveBeenCalledWith(expect.objectContaining({ page: 1 }))

    // 类型下拉选 AI 调用
    const selects = wrapper.findAllComponents(ElSelect)
    selects[0].vm.$emit('update:modelValue', 'AI_CALL')
    await flushPromises()
    // 成功状态下拉选 失败
    selects[1].vm.$emit('update:modelValue', 'false')
    await flushPromises()
    // 模块模糊
    await wrapper.find('input[placeholder*="模块"]').setValue('Ai')
    await wrapper.findAll('button').find((b) => b.text() === '查询')!.trigger('click')
    await flushPromises()
    expect(mocked).toHaveBeenLastCalledWith(
      expect.objectContaining({ logType: 'AI_CALL', module: 'Ai', success: false, page: 1 }),
    )

    await wrapper.findAll('button').find((b) => b.text() === '重置')!.trigger('click')
    await flushPromises()
    expect(mocked).toHaveBeenLastCalledWith(
      expect.not.objectContaining({ logType: expect.anything(), module: expect.anything() }),
    )
    wrapper.unmount()
  })
})

/** 表格：类型徽章、成败、错误码、耗时、空态。 */
describe('LogsView 表格', () => {
  it('渲染徽章与成败标识', async () => {
    const wrapper = mountView()
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('业务操作')
    expect(text).toContain('AI 调用')
    expect(wrapper.find('.log-badge-AI_CALL').exists()).toBe(true)
    expect(wrapper.find('.log-badge-OPERATION').exists()).toBe(true)
    expect(text).toContain('✓')
    expect(text).toContain('✗')
    expect(text).toContain('42001')
    expect(text).toContain('42')
    wrapper.unmount()
  })

  it('空列表展示空态文案', async () => {
    mocked.mockResolvedValue(pageOf([]))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无日志')
    wrapper.unmount()
  })
})

/** 详情抽屉：请求/返回全文等宽展示 + 可复制。 */
describe('LogsView 详情抽屉', () => {
  it('查看打开抽屉，全文等宽展示并支持复制', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === '查看')!.trigger('click')
    await flushPromises()

    const pres = wrapper.findAll('pre')
    expect(pres.length).toBe(2)
    expect(pres[0].text()).toContain('"errorQuestionId":"q1"')
    expect(pres[1].text()).toContain('"code":0')

    await wrapper.findAll('button').find((b) => b.text() === '复制')!.trigger('click')
    await flushPromises()
    expect(writeText).toHaveBeenCalled()
    wrapper.unmount()
  })
})
