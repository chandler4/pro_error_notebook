/**
 * Markdown/LaTeX 渲染封装（F-1；security.md §4：markdown-it + KaTeX + DOMPurify 双端渲染）。
 *
 * 全局唯一允许 v-html 的数据出口：所有富文本（题干/解析/AI 返回）必须经 renderMarkdown
 * 消毒后渲染（组件统一用 HtmlView，vue/no-v-html 对其余位置报 error）。
 */
import MarkdownIt from 'markdown-it'
import katex from 'katex'
import DOMPurify from 'dompurify'

const md = new MarkdownIt({ html: true, linkify: true, breaks: true })

/** KaTeX 预处理：块级 $$...$$ 与行内 $...$ 转为 KaTeX HTML（markdown 渲染前）。 */
function renderMath(src: string): string {
  return src
    .replace(/\$\$([\s\S]+?)\$\$/g, (_, expr: string) =>
      katex.renderToString(expr, { displayMode: true, throwOnError: false }),
    )
    .replace(/\$([^$\n]+?)\$/g, (_, expr: string) =>
      katex.renderToString(expr, { throwOnError: false }),
    )
}

/** 渲染 + 消毒：输出可安全 v-html 的 HTML 字符串（TC-S-02）。 */
export function renderMarkdown(src: string): string {
  if (!src) return ''
  const withMath = renderMath(src)
  const html = md.render(withMath)
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true, mathMl: true, svg: true } })
}
