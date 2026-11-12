import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElRadioGroup } from 'element-plus'
import BackupView from '../BackupView.vue'
import { backupExport, backupImport } from '../../api/question'
import type { ImportResult } from '../../types'

vi.mock('../../api/question', () => ({
  backupExport: vi.fn(),
  backupImport: vi.fn(),
}))

const mockedExport = vi.mocked(backupExport)
const mockedImport = vi.mocked(backupImport)

const okResult: ImportResult = {
  total: 10,
  inserted: 8,
  skipped: 1,
  overwritten: 1,
  errors: [{ line: 3, reason: 'questionStem 缺失' }],
}

beforeEach(() => {
  vi.clearAllMocks()
  mockedExport.mockResolvedValue(undefined)
  mockedImport.mockResolvedValue(okResult)
})

function mountView() {
  return mount(BackupView, { global: { plugins: [ElementPlus] } })
}

function jsonFile(name = 'backup.json', size = 128): File {
  return new File([new Uint8Array(size)], name, { type: 'application/json' })
}

/** 备份导出卡片（3.10 / prototype-spec §2.4）。 */
describe('BackupView 导出', () => {
  it('说明文案 + 点击导出全量 JSON', async () => {
    const wrapper = mountView()
    expect(wrapper.text()).toContain('已确认错题')
    await wrapper.findAll('button').find((b) => b.text().includes('导出全量'))!.trigger('click')
    await flushPromises()
    expect(mockedExport).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})

/** 导入：模式单选 + 结果报告 + 行级错误表。 */
describe('BackupView 导入', () => {
  it('选择文件默认 skip 模式导入并展示结果报告与错误明细', async () => {
    const wrapper = mountView()
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [jsonFile()] })
    await input.trigger('change')
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('开始导入'))!.trigger('click')
    await flushPromises()
    expect(mockedImport).toHaveBeenCalledWith(expect.any(File), 'skip')

    const text = wrapper.text()
    expect(text).toContain('10')
    expect(text).toContain('8')
    expect(text).toContain('questionStem 缺失')
    expect(text).toContain('3')
    wrapper.unmount()
  })

  it('切换 overwrite 模式生效', async () => {
    const wrapper = mountView()
    wrapper.findComponent(ElRadioGroup).vm.$emit('update:modelValue', 'overwrite')
    await flushPromises()
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [jsonFile()] })
    await input.trigger('change')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text().includes('开始导入'))!.trigger('click')
    await flushPromises()
    expect(mockedImport).toHaveBeenCalledWith(expect.any(File), 'overwrite')
    wrapper.unmount()
  })

  it('未选文件或超 20MB 阻断导入', async () => {
    const wrapper = mountView()
    // 未选文件
    await wrapper.findAll('button').find((b) => b.text().includes('开始导入'))!.trigger('click')
    await flushPromises()
    expect(mockedImport).not.toHaveBeenCalled()

    // 超 20MB
    const big = new File([new Uint8Array(21 * 1024 * 1024)], 'big.json', { type: 'application/json' })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [big] })
    await input.trigger('change')
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text().includes('开始导入'))!.trigger('click')
    await flushPromises()
    expect(mockedImport).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
