import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '../markdown'

/** TC-S-02 安全设计：题干/解析为用户+AI 生成 Markdown，渲染前必须 DOMPurify 消毒。 */
describe('renderMarkdown（TC-S-02 XSS 消毒）', () => {
  it('移除 <script> 注入', () => {
    const html = renderMarkdown('题干 <script>alert(1)</script> 结束')
    expect(html).not.toContain('<script')
    expect(html).not.toContain('alert(1)</script>')
  })

  it('移除 <img onerror> 事件属性', () => {
    const html = renderMarkdown('<img src=x onerror=alert(1)>')
    expect(html).not.toContain('onerror')
    expect(html).not.toContain('alert(1)')
  })

  it('移除内联事件与 javascript: 协议', () => {
    const html = renderMarkdown('<a href="javascript:alert(1)" onclick="steal()">点我</a>')
    expect(html).not.toContain('javascript:')
    expect(html).not.toContain('onclick')
  })

  it('正常 Markdown 语法照常渲染', () => {
    const html = renderMarkdown('**步骤1** 串联电路分压')
    expect(html).toContain('<strong>步骤1</strong>')
  })

  it('行内公式 $...$ 经 KaTeX 渲染（含 MathML）', () => {
    const html = renderMarkdown('电源电压 $U = U_1 + U_2$ 保持不变')
    expect(html).toContain('katex')
  })

  it('块级公式 $$...$$ 渲染为 display 模式', () => {
    const html = renderMarkdown('推导：\n$$U = IR$$\n完毕')
    expect(html).toContain('katex-display')
  })

  it('LaTeX 语法中的特殊 HTML 字符被消毒但公式语义保留', () => {
    const html = renderMarkdown('$a < b$ 且 $x > y$')
    expect(html).toContain('katex')
    expect(html).not.toContain('$a < b$')
  })
})
