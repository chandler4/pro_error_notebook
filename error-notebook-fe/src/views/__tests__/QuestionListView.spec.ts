import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessageBox, ElPagination } from 'element-plus'
import QuestionListView from '../QuestionListView.vue'
import QuestionDetailDrawer from '../../components/QuestionDetailDrawer.vue'
import router from '../../router'
import { listQuestions, listDrafts, deleteQuestion, deleteDraft, getQuestion } from '../../api/question'
import { getEnums } from '../../api/meta'
import type { PageResult, WrongQuestion } from '../../types'

vi.mock('../../api/question', () => ({
  listQuestions: vi.fn(),
  listDrafts: vi.fn(),
  deleteQuestion: vi.fn(),
  deleteDraft: vi.fn(),
  getQuestion: vi.fn(),
}))
vi.mock('../../api/meta', () => ({
  getEnums: vi.fn(),
  getKnowledgePoints: vi.fn(),
}))
vi.mock('element-plus', async (importOriginal) => {
  const orig = (await importOriginal()) as Record<string, unknown>
  return { ...orig, ElMessageBox: { confirm: vi.fn() } }
})

const mockedList = vi.mocked(listQuestions)
const mockedDrafts = vi.mocked(listDrafts)
const mockedEnums = vi.mocked(getEnums)

function makeRow(id: string, over: Partial<WrongQuestion> = {}): WrongQuestion {
  return {
    errorQuestionId: id,
    subject: '数学',
    gradeLevel: '高一',
    questionType: '选择题',
    questionStem: `已知函数 f(x)=x^2，求导（${id}）`,
    questionImages: [{ url: 'images/202611/a1.jpg', role: 'QUESTION' }],
    sourceName: '期中试卷',
    questionNumber: '12',
    standardAnswer: ['A'],
    knowledgePoints: ['函数', '导数'],
    tags: ['易错'],
    knowledgeAnalysis: '**解析**：由题意知……',
    wrongDate: '2026-11-01',
    status: 'CONFIRMED',
    createdAt: '2026-11-01 10:00:00',
    ...over,
  }
}

function pageOf(list: WrongQuestion[], total = list.length): PageResult<WrongQuestion> {
  return { list, total, page: 1, size: 10 }
}

function mountView() {
  return mount(QuestionListView, {
    global: { plugins: [ElementPlus, createPinia(), router] },
  })
}

beforeEach(async () => {
  vi.clearAllMocks()
  // 初始导航完成后再断言路由（避免 redirect 竞争）
  await router.push('/questions')
  await router.isReady()
  mockedEnums.mockResolvedValue({
    subjects: ['数学', '物理', '化学', '语文'],
    gradeLevels: ['高一', '高二', '高三'],
    questionTypes: ['选择题', '填空题', '解答题'],
    subjectTabs: ['数学', '物理', '化学'],
  })
  mockedList.mockResolvedValue(pageOf([makeRow('q1'), makeRow('q2')]))
  mockedDrafts.mockResolvedValue(pageOf([makeRow('d1', { status: 'PENDING_CONFIRM', questionStem: '待确认草稿一' })], 1))
  vi.mocked(getQuestion).mockImplementation(async (id: string) => makeRow(id))
  vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
})

/** TC-L-06：查询重置 page=1；重置清空条件并刷新。 */
describe('QuestionListView 查询/重置（TC-L-06）', () => {
  it('查询携带关键词并回第一页；重置清空条件并刷新', async () => {
    const wrapper = mountView()
    await flushPromises()
    // 初始：首 Tab 科目 + page 1
    expect(mockedList).toHaveBeenCalledWith(expect.objectContaining({ subject: '数学', page: 1 }))

    // 翻到第 2 页后输入题干关键词点查询 → 回 page 1 且带 keyword
    wrapper.findComponent(ElPagination).vm.$emit('current-change', 2)
    await flushPromises()
    expect(mockedList).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 }))

    await wrapper.find('input[placeholder="题干关键词"]').setValue('函数')
    await wrapper.findAll('button').find((b) => b.text() === '查询')!.trigger('click')
    await flushPromises()
    expect(mockedList).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: '函数', page: 1 }))

    // 重置 → 清空条件、回第一页、重新查询
    await wrapper.findAll('button').find((b) => b.text() === '重置')!.trigger('click')
    await flushPromises()
    expect(mockedList).toHaveBeenLastCalledWith(
      expect.not.objectContaining({ keyword: expect.anything() }),
    )
    expect(mockedList).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
    expect((wrapper.find('input[placeholder="题干关键词"]').element as HTMLInputElement).value).toBe('')
  })
})

/** TC-L-07：切换分科 Tab 携带科目重新查询。 */
describe('QuestionListView 分科 Tab（TC-L-07）', () => {
  it('渲染 subjectTabs 并在切换时携带科目重新查询', async () => {
    const wrapper = mountView()
    await flushPromises()
    const tabs = wrapper.findAll('.el-tabs__item').map((t) => t.text())
    expect(tabs).toEqual(['数学', '物理', '化学'])

    await wrapper.findAll('.el-tabs__item').find((t) => t.text() === '物理')!.trigger('click')
    await flushPromises()
    expect(mockedList).toHaveBeenLastCalledWith(expect.objectContaining({ subject: '物理', page: 1 }))
  })
})

/** 待确认入口：草稿视图切换 / 续编路由 / 草稿删除。 */
describe('QuestionListView 待确认草稿入口', () => {
  it('点击「待确认」切换草稿视图，继续编辑跳新增页 ?id=，再点切回', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(mockedDrafts).toHaveBeenCalledTimes(1) // 徽标计数

    await wrapper.findAll('button').find((b) => b.text().includes('待确认'))!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('待确认草稿一')
    expect(mockedDrafts).toHaveBeenCalledTimes(2)

    const editBtn = wrapper.findAll('button').find((b) => b.text() === '继续编辑')
    expect(editBtn).toBeTruthy()
    await editBtn!.trigger('click')
    // 懒加载路由的动态 import 是宏任务，flushPromises 不够——waitFor 轮询等导航完成
    await vi.waitFor(() => {
      expect(router.currentRoute.value.fullPath).toBe('/questions/new?id=d1')
    })

    // 草稿删除（确认框已 mock 为确认）
    await wrapper.findAll('button').find((b) => b.text() === '删除')!.trigger('click')
    await flushPromises()
    expect(deleteDraft).toHaveBeenCalledWith('d1')
    expect(mockedDrafts.mock.calls.length).toBeGreaterThanOrEqual(3)
    wrapper.unmount()
  })
})

/** 删除需二次确认；取消不调用接口。 */
describe('QuestionListView 删除二次确认', () => {
  it('确认后调用 DELETE 并刷新列表；取消不调用', async () => {
    const wrapper = mountView()
    await flushPromises()
    const delBtn = wrapper.findAll('button').find((b) => b.text() === '删除')
    expect(delBtn).toBeTruthy()

    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel' as never)
    await delBtn!.trigger('click')
    await flushPromises()
    expect(deleteQuestion).not.toHaveBeenCalled()

    await wrapper.findAll('button').find((b) => b.text() === '删除')!.trigger('click')
    await flushPromises()
    expect(deleteQuestion).toHaveBeenCalledWith('q1')
    // 删除后刷新（初始 1 次 + 刷新 1 次）
    expect(mockedList).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })
})

/** 查看打开详情抽屉。 */
describe('QuestionListView 详情抽屉', () => {
  it('点击查看请求详情并打开抽屉', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findAll('button').find((b) => b.text() === '查看')!.trigger('click')
    await flushPromises()
    expect(getQuestion).toHaveBeenCalledWith('q1')
    const drawer = wrapper.findComponent(QuestionDetailDrawer)
    expect(drawer.props('modelValue')).toBe(true)
    expect(wrapper.text()).toContain('已知函数 f(x)=x^2，求导（q1）')
    wrapper.unmount()
  })
})
