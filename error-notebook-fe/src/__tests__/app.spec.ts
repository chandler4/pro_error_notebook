import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import App from '../App.vue'
import router from '../router'

describe('T0-1/T0-2 FE 骨架', () => {
  it('App 挂载并渲染演示路由', async () => {
    router.push('/demo')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await flushPromises()
    expect(wrapper.text()).toContain('三服务联通演示')
  })
})
