import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElImage } from 'element-plus'
import QuestionDetailDrawer from '../QuestionDetailDrawer.vue'
import HtmlView from '../HtmlView.vue'
import type { WrongQuestion } from '../../types'

const question: WrongQuestion = {
  errorQuestionId: 'q1',
  subject: '数学',
  gradeLevel: '高一',
  questionType: '选择题',
  questionStem: '已知函数 $f(x)=x^2$，求 $f(2)$。',
  questionImages: [
    { url: 'images/202611/a1.jpg', role: 'QUESTION' },
    { url: 'images/202611/b1.jpg', role: 'ANSWER_SHEET' },
  ],
  sourceName: '期中试卷',
  questionNumber: '12',
  standardAnswer: ['A', '代入得 f(2)=4'],
  knowledgePoints: ['函数'],
  tags: ['易错', '计算'],
  knowledgeAnalysis: '**解析**：代入 $x=2$ 得 4。',
  wrongDate: '2026-11-01',
  status: 'CONFIRMED',
}

/** 详情抽屉（prototype-spec §2.5）：图片/题干/答案/知识点标签/解析渲染/元信息。 */
describe('QuestionDetailDrawer', () => {
  it('渲染题干、编号答案、知识点与标签', () => {
    const wrapper = mount(QuestionDetailDrawer, {
      props: { modelValue: true, question },
    })
    expect(wrapper.text()).toContain('已知函数')
    const answers = wrapper.findAll('ol li')
    expect(answers.length).toBe(2)
    expect(answers[1].text()).toBe('代入得 f(2)=4')
    expect(wrapper.text()).toContain('函数')
    expect(wrapper.text()).toContain('易错')
    expect(wrapper.text()).toContain('计算')
  })

  it('图片横向排列并带角色角标，URL 拼 /files/ 前缀', () => {
    const wrapper = mount(QuestionDetailDrawer, {
      props: { modelValue: true, question },
      global: { components: { ElImage } },
    })
    // jsdom 图片不真实加载（ElImage 加载成功才渲染内层 img），按组件 props 断言 src
    const images = wrapper.findAllComponents(ElImage)
    expect(images.length).toBe(2)
    expect(images[0].props('src')).toBe('/files/images/202611/a1.jpg')
    const badges = wrapper.findAll('.role-badge').map((b) => b.text())
    expect(badges).toEqual(['题目图', '解答图'])
  })

  /** W-10（chandler 走查）：原题图片点击放大——el-image 预览列表含全部图与正确起始索引。 */
  it('W-10: 图片组件带预览列表（点击放大、多图翻页）', () => {
    const wrapper = mount(QuestionDetailDrawer, {
      props: { modelValue: true, question },
      global: { components: { ElImage } },
    })
    const images = wrapper.findAllComponents(ElImage)
    expect(images.length).toBe(2)
    expect(images[0].props('previewSrcList')).toEqual([
      '/files/images/202611/a1.jpg',
      '/files/images/202611/b1.jpg',
    ])
    expect(images[0].props('initialIndex')).toBe(0)
    expect(images[1].props('initialIndex')).toBe(1)
  })

  it('解析经 Markdown+KaTeX 渲染（HtmlView 出口）', () => {
    const wrapper = mount(QuestionDetailDrawer, {
      props: { modelValue: true, question },
    })
    const hv = wrapper.findComponent(HtmlView)
    expect(hv.exists()).toBe(true)
    // markdown-it 输出 <strong>；KaTeX 输出 .katex 容器
    expect(hv.props('html')).toContain('<strong>解析</strong>')
    expect(hv.props('html')).toContain('katex')
  })

  it('渲染元信息（科目/年级/题型/来源/题号/日期）', () => {
    const wrapper = mount(QuestionDetailDrawer, {
      props: { modelValue: true, question },
    })
    const text = wrapper.text()
    ;['数学', '高一', '选择题', '期中试卷', '12', '2026-11-01'].forEach((s) =>
      expect(text).toContain(s),
    )
  })
})
