import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from '../App.vue'
import router from '../router'

/** F-1：三菜单布局 + 正式路由可达。 */
describe('App 布局与路由（F-1）', () => {
  it('左侧三菜单（错题记录/运行日志/数据备份）', async () => {
    router.push('/questions')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [ElementPlus, router, createPinia()] } })
    await flushPromises()
    const menuText = wrapper.text()
    expect(menuText).toContain('错题记录')
    expect(menuText).toContain('运行日志')
    expect(menuText).toContain('数据备份')
  })

  it('根路径重定向到 /questions', async () => {
    router.push('/')
    await router.isReady()
    expect(router.currentRoute.value.path).toBe('/questions')
  })

  it('T0-2 演示页仍可达（联通演示保留）', async () => {
    await router.push('/demo')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [ElementPlus, router, createPinia()] } })
    await flushPromises()
    expect(wrapper.text()).toContain('三服务联通演示')
  })
})
