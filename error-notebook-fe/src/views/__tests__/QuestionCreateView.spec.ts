import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import QuestionCreateView from '../QuestionCreateView.vue'
import ImageUploader from '../../components/ImageUploader.vue'
import router from '../../router'
import {
  parseImage,
  parseQuestion,
  createEmptyDraft,
  confirmQuestion,
  getQuestion,
  updateQuestion,
  deleteQuestion,
} from '../../api/question'
import { getEnums, getKnowledgePoints } from '../../api/meta'
import type { WrongQuestion } from '../../types'

vi.mock('../../api/question', () => ({
  parseImage: vi.fn(),
  parseQuestion: vi.fn(),
  createEmptyDraft: vi.fn(),
  confirmQuestion: vi.fn(),
  getQuestion: vi.fn(),
  updateQuestion: vi.fn(),
  deleteQuestion: vi.fn(),
  listQuestions: vi.fn(),
  listDrafts: vi.fn(),
  deleteDraft: vi.fn(),
}))
vi.mock('../../api/meta', () => ({ getEnums: vi.fn(), getKnowledgePoints: vi.fn() }))
vi.mock('../../utils/image', () => ({ compress: vi.fn(async (f: Blob) => f) }))
vi.mock('../../utils/crop', () => ({
  attachCropper: vi.fn(() => ({})),
  cropToBlob: vi.fn(async () => new Blob(['c'], { type: 'image/jpeg' })),
  destroyCropper: vi.fn(),
}))

const mockedParse = vi.mocked(parseQuestion)
const mockedParseImage = vi.mocked(parseImage)
const mockedEmptyDraft = vi.mocked(createEmptyDraft)
const mockedConfirm = vi.mocked(confirmQuestion)
const mockedGet = vi.mocked(getQuestion)
const mockedUpdate = vi.mocked(updateQuestion)
const mockedKp = vi.mocked(getKnowledgePoints)

function pngFile(name = 'a.png'): File {
  return new File(['png'], name, { type: 'image/png' })
}

function makeDraft(id: string, over: Partial<WrongQuestion> = {}): WrongQuestion {
  return {
    errorQuestionId: id,
    subject: '数学',
    gradeLevel: '高一',
    questionType: '选择题',
    questionStem: '已知函数 f(x)，求 f(2)。',
    questionImages: [{ url: 'images/202611/x.jpg', role: 'QUESTION' }],
    sourceName: '期中试卷',
    questionNumber: '12',
    standardAnswer: ['B'],
    knowledgePoints: ['函数'],
    tags: [],
    knowledgeAnalysis: '**解析**……',
    wrongDate: '2026-11-01',
    status: 'PENDING_CONFIRM',
    createdAt: '2026-11-01 10:00:00',
    ...over,
  }
}

beforeEach(async () => {
  vi.clearAllMocks()
  vi.mocked(getEnums).mockResolvedValue({
    subjects: ['数学', '物理'],
    gradeLevels: ['高一', '高二'],
    questionTypes: ['选择题', '填空题'],
    subjectTabs: ['数学', '物理'],
  })
  mockedParseImage.mockResolvedValue(['images/202611/x.jpg'])
  mockedParse.mockResolvedValue(makeDraft('d-new'))
  mockedEmptyDraft.mockResolvedValue(
    makeDraft('d-empty', {
      questionStem: '',
      subject: '',
      questionImages: [],
      standardAnswer: [''],
    }),
  )
  mockedConfirm.mockResolvedValue(makeDraft('d-new', { status: 'CONFIRMED' }))
  mockedUpdate.mockResolvedValue(makeDraft('q9', { status: 'CONFIRMED' }))
  mockedGet.mockResolvedValue(makeDraft('q9', { status: 'CONFIRMED' }))
  mockedKp.mockResolvedValue([{ name: '函数', useCount: 3 }])
  await router.push('/questions/new')
  await router.isReady()
})

function mountView() {
  return mount(QuestionCreateView, {
    global: { plugins: [ElementPlus, createPinia(), router] },
  })
}

/** 上传一张图并点击开始解析，到达步骤3（分项确认）。 */
async function uploadAndParse(wrapper: ReturnType<typeof mountView>): Promise<void> {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [pngFile()] })
  await input.trigger('change')
  await flushPromises()
  await wrapper
    .findAll('button')
    .find((b) => b.text().includes('开始解析'))!
    .trigger('click')
  await flushPromises()
}

/** TC-N-01：独立页面（路由 /questions/new 非弹窗），可返回列表。 */
describe('QuestionCreateView 独立页面（TC-N-01）', () => {
  it('渲染三步步骤条与返回列表入口', async () => {
    const wrapper = mountView()
    await flushPromises()
    const text = wrapper.text()
    ;['上传图片', 'AI 解析', '分项确认'].forEach((s) => expect(text).toContain(s))
    const back = wrapper.findAll('button').find((b) => b.text().includes('返回列表'))
    expect(back).toBeTruthy()
    await back!.trigger('click')
    await vi.waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/questions')
    })
    wrapper.unmount()
  })
})

/** TC-N-02：三步流状态机——上传→解析→确认联动；返回上一步保留数据。 */
describe('QuestionCreateView 三步流（TC-N-02）', () => {
  it('无图时开始解析禁用；解析成功进入确认；返回上一步图保留', async () => {
    const wrapper = mountView()
    await flushPromises()
    const startBtn = wrapper.findAll('button').find((b) => b.text().includes('开始解析'))!
    expect((startBtn.element as HTMLButtonElement).disabled).toBe(true)

    await uploadAndParse(wrapper)
    // 上传接口 + 解析接口被调用，进入步骤3（表单可见）
    expect(mockedParseImage).toHaveBeenCalledTimes(1)
    expect(mockedParse).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('分项确认')
    // 题干预填在 textarea 的 DOM value（text() 不含表单值）
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toContain(
      '已知函数 f(x)，求 f(2)。',
    )

    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('返回上一步'))!
      .trigger('click')
    await flushPromises()
    // 图片保留（缩略图仍在上传区）
    const uploader = wrapper.findComponent(ImageUploader)
    expect((uploader.props('modelValue') as unknown[]).length).toBe(1)
    wrapper.unmount()
  })
})

/** TC-N-07（FR-A7）：仅题目图（无解答图）直接解析——空答案/解析草稿可不经补全直接确认。 */
describe('QuestionCreateView 仅题目图直接解析（TC-N-07）', () => {
  it('单张 QUESTION 图解析按钮可用，请求仅含 QUESTION 角色；空答案草稿直接确认成功', async () => {
    mockedParse.mockResolvedValueOnce(
      makeDraft('d-q-only', {
        questionImages: [{ url: 'images/202611/x.jpg', role: 'QUESTION' }],
        standardAnswer: [],
        knowledgeAnalysis: '',
        subject: '物理',
      }),
    )
    const wrapper = mountView()
    await flushPromises()

    // 上传单张题目图（默认角色 QUESTION）→ 解析按钮由禁用转为可用
    const startBtn0 = wrapper.findAll('button').find((b) => b.text().includes('开始解析'))!
    expect((startBtn0.element as HTMLButtonElement).disabled).toBe(true)
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', { value: [pngFile()] })
    await input.trigger('change')
    await flushPromises()
    const startBtn = wrapper.findAll('button').find((b) => b.text().includes('开始解析'))!
    expect((startBtn.element as HTMLButtonElement).disabled).toBe(false)
    await startBtn.trigger('click')
    await flushPromises()

    // parse 请求只带 QUESTION 角色，不要求 ANSWER_SHEET
    expect(mockedParse).toHaveBeenCalledWith([{ url: 'images/202611/x.jpg', role: 'QUESTION' }])
    expect(wrapper.text()).toContain('分项确认')

    // 答案/解析为空不阻塞：题干已预填（科目来自草稿）→ 直接确认录入
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toContain('已知函数')
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('确认录入'))!
      .trigger('click')
    await flushPromises()
    expect(mockedConfirm).toHaveBeenCalledTimes(1)
    const payload = mockedConfirm.mock.calls[0][1] as {
      standardAnswer: string[]
      knowledgeAnalysis: string
    }
    expect(payload.standardAnswer).toEqual([]) // 空答案行被过滤，原样为空
    expect(payload.knowledgeAnalysis).toBe('')
    expect(wrapper.text()).toContain('录入成功')
    wrapper.unmount()
  })
})

/** TC-N-03：分项确认交互——答案增删、知识点回车加/×删、标签回车加。 */
describe('QuestionCreateView 分项确认（TC-N-03）', () => {
  it('答案增删；知识点联想点选复用 + 回车自由输入 + × 删除；标签回车添加且不联想', async () => {
    const wrapper = mountView()
    await flushPromises()
    await uploadAndParse(wrapper)

    // 答案：默认 1 行 → 添加一行 → 删除一行
    const addAnswer = wrapper.findAll('button').find((b) => b.text().includes('添加答案'))!
    await addAnswer.trigger('click')
    await flushPromises()
    expect(wrapper.findAll('textarea').length).toBeGreaterThanOrEqual(2) // 答案行 + 题干 + 解析
    const delBtns = wrapper.findAll('button').filter((b) => b.text() === '删')
    await delBtns[delBtns.length - 1].trigger('click')
    await flushPromises()

    // 知识点联想：输入触发查询，点选复用
    const kpInput = wrapper.find('input[placeholder*="知识点"]')
    await kpInput.setValue('函')
    await flushPromises()
    expect(mockedKp).toHaveBeenCalledWith('函', expect.anything())
    // 候选渲染并点选复用（BR-03 归并；按钮文本含使用次数「函数（3 次）」）
    await wrapper
      .findAll('.kp-candidate')
      .find((b) => b.text().includes('函数'))!
      .trigger('click')
    await flushPromises()

    // 自由输入回车添加
    await kpInput.setValue('新知识点')
    await kpInput.trigger('keydown.enter')
    await flushPromises()

    // 标签：回车添加，不触发联想
    const tagInput = wrapper.find('input[placeholder*="标签"]')
    await tagInput.setValue('易错题')
    await tagInput.trigger('keydown.enter')
    await flushPromises()
    expect(wrapper.text()).toContain('易错题')
    expect(mockedKp).not.toHaveBeenCalledWith('易错题', expect.anything())

    // 知识点 × 删除（删除“新知识点”）
    const kpTags = wrapper.findAll('.kp-item')
    expect(kpTags.length).toBeGreaterThanOrEqual(2)
    await kpTags
      .find((t) => t.text().includes('新知识点'))!
      .find('.kp-remove')
      .trigger('click')
    await flushPromises()
    expect(wrapper.text()).not.toContain('新知识点')
    wrapper.unmount()
  })
})

/** TC-N-04：确认必填——题干空阻断并提示。 */
describe('QuestionCreateView 确认必填（TC-N-04）', () => {
  it('题干为空时确认被阻断且不调用接口；补齐后成功进入完成态', async () => {
    const wrapper = mountView()
    await flushPromises()
    await uploadAndParse(wrapper)

    const stemInput = wrapper.find('textarea')
    await stemInput.setValue('')
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('确认录入'))!
      .trigger('click')
    await flushPromises()
    expect(mockedConfirm).not.toHaveBeenCalled()

    await stemInput.setValue('补齐的题干')
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('确认录入'))!
      .trigger('click')
    await flushPromises()
    expect(mockedConfirm).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('录入成功')
    wrapper.unmount()
  })
})

/** TC-P-06：解析状态展示——loading 态；失败态提供重试/转手动填写。 */
describe('QuestionCreateView 解析状态（TC-P-06）', () => {
  it('解析失败展示错误与重试/转手动；转手动建空草稿零 AI 调用直达确认', async () => {
    const wrapper = mountView()
    await flushPromises()
    mockedParse.mockRejectedValueOnce(new Error('AI 解析失败（42001）'))
    await uploadAndParse(wrapper)

    expect(wrapper.text()).toContain('AI 解析失败')
    expect(wrapper.text()).toContain('重试')
    expect(wrapper.text()).toContain('转手动填写')

    // 重试成功 → 确认步
    await wrapper
      .findAll('button')
      .find((b) => b.text() === '重试')!
      .trigger('click')
    await flushPromises()
    expect(mockedParse).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('分项确认')

    // 回到失败路径再走转手动
    mockedParse.mockRejectedValueOnce(new Error('AI 解析失败（42001）'))
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('返回上一步'))!
      .trigger('click')
    await flushPromises()
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('开始解析'))!
      .trigger('click')
    await flushPromises()
    await wrapper
      .findAll('button')
      .find((b) => b.text() === '转手动填写')!
      .trigger('click')
    await flushPromises()
    expect(mockedEmptyDraft).toHaveBeenCalledTimes(1)
    // 转手动：POST drafts 一次；解析调用保持 3 次（首次+重试+换图再解析，转手动不增加 AI 调用）
    expect(mockedParse).toHaveBeenCalledTimes(3)
    expect(wrapper.text()).toContain('分项确认')
    wrapper.unmount()
  })
})

/** TC-N-06：编辑模式重新解析——confirm 新记录成功后联动 DELETE 旧记录。 */
describe('QuestionCreateView 编辑重解析删旧（TC-N-06）', () => {
  it('编辑模式直达步骤3；重新解析换图；确认新记录后删除旧记录', async () => {
    await router.push('/questions/new?id=q9')
    await router.isReady()
    const wrapper = mountView()
    await flushPromises()
    expect(mockedGet).toHaveBeenCalledWith('q9')
    // 直达步骤3 且预填（textarea DOM value）
    expect((wrapper.find('textarea').element as HTMLTextAreaElement).value).toContain(
      '已知函数 f(x)，求 f(2)。',
    )
    expect(wrapper.text()).toContain('重新解析')

    // 重新解析 → 回步骤1（换图）；按钮文本为「重新解析（换图）」
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('重新解析'))!
      .trigger('click')
    await flushPromises()
    expect(wrapper.find('input[type="file"]').exists()).toBe(true)

    // 换图重新走解析流（新草稿 d-new）
    await uploadAndParse(wrapper)
    // 直接编辑模式确认（非重解析路径）应走 PUT；重解析路径 confirm 新 + 删旧
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('确认录入'))!
      .trigger('click')
    await flushPromises()
    expect(mockedConfirm).toHaveBeenCalledWith(
      'd-new',
      expect.objectContaining({ questionStem: '已知函数 f(x)，求 f(2)。' }),
    )
    expect(deleteQuestion).toHaveBeenCalledWith('q9')
    expect(wrapper.text()).toContain('录入成功')
    wrapper.unmount()
  })

  it('编辑模式未重解析时确认走 PUT 更新', async () => {
    await router.push('/questions/new?id=q9')
    await router.isReady()
    const wrapper = mountView()
    await flushPromises()
    await wrapper
      .findAll('button')
      .find((b) => b.text().includes('确认录入'))!
      .trigger('click')
    await flushPromises()
    expect(mockedUpdate).toHaveBeenCalledWith('q9', expect.anything())
    expect(mockedConfirm).not.toHaveBeenCalled()
    expect(deleteQuestion).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})
