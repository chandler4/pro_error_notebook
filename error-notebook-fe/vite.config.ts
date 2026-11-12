import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

// 端口约定（tasks.md §0）：FE dev 5173，代理 → BE 8080（/api 接口、/files 图片静态资源）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/files': { target: 'http://127.0.0.1:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    // resources usable：jsdom 默认 NoOpResourceLoader 会让 Image 的 dataURL 加载
    // 既不触发 load 也不触发 error（Promise 永久悬挂）；启用真 loader 后 data: URL
    // 由 node-canvas 解码并派发 load 事件（image.ts 压缩链路依赖此行为）
    environmentOptions: {
      jsdom: {
        resources: 'usable',
      },
    },
    include: ['src/**/*.{spec,test}.ts'],
    setupFiles: ['vitest.setup.ts'],
    // 懒加载路由组件首次 transform（katex/dompurify 等依赖链）在慢机可超 5s 默认值
    testTimeout: 20_000,
  },
})
