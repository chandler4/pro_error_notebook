import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ElementPlus, { ElRadioGroup, ElSwitch } from 'element-plus'
import ExportDialog from '../ExportDialog.vue'
import { exportQuestions } from '../../api/question'
import { useListStore } from '../../store/list'

vi.mock('../../api/question', () => ({
  exportQuestions: vi.fn(),
  toQueryString: vi.fn(() => ''),
}))

const mockedExport = vi.mocked(exportQuestions)

beforeEach(() => {
  vi.clearAllMocks()
  mockedExport.mockResolvedValue(undefined)
})

function mountDialog() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useListStore()
  store.setFilters({ subject: '数学', keyword: '函数', page: 1, size: 10 })
  const wrapper = mount(ExportDialog, {
    props: { modelValue: true, 'onUpdate:modelValue': (v: boolean) => void v },
    global: { plugins: [ElementPlus, pinia] },
  })
  return { wrapper, store }
}

/** 导出弹窗（prototype-spec §2.6）：格式单选/重做开关（仅 PDF）/筛选摘要/下载反馈。 */
describe('ExportDialog', () => {
  it('展示当前筛选条件摘要（listStore 共享）', async () => {
    const { wrapper } = mountDialog()
    await flushPromises()
    expect(wrapper.text()).toContain('科目=数学')
    expect(wrapper.text()).toContain('题干含「函数」')
    wrapper.unmount()
  })

  it('Markdown 格式下重做开关禁用；PDF 下可开', async () => {
    const { wrapper } = mountDialog()
    await flushPromises()
    const switches = wrapper.findAllComponents(ElSwitch)
    expect((switches[0].props('disabled') as boolean)).toBe(true)

    wrapper.findComponent(ElRadioGroup).vm.$emit('update:modelValue', 'pdf')
    await flushPromises()
    expect((wrapper.findAllComponents(ElSwitch)[0].props('disabled') as boolean)).toBe(false)
    wrapper.unmount()
  })

  it('点击导出携带筛选+格式+重做并关闭弹窗', async () => {
    const { wrapper } = mountDialog()
    await flushPromises()
    wrapper.findComponent(ElRadioGroup).vm.$emit('update:modelValue', 'pdf')
    await flushPromises()
    wrapper.findAllComponents(ElSwitch)[0].vm.$emit('update:modelValue', true)
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text() === '导出')!.trigger('click')
    await flushPromises()
    expect(mockedExport).toHaveBeenCalledWith(
      expect.objectContaining({ format: 'pdf', rework: true, subject: '数学', keyword: '函数' }),
    )
    const closed = wrapper.emitted('update:modelValue')!
    expect(closed[closed.length - 1][0]).toBe(false)
    wrapper.unmount()
  })
})
