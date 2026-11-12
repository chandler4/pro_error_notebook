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
    include: ['src/**/*.{spec,test}.ts'],
  },
})
