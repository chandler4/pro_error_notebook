<script setup lang="ts">
import { ref } from 'vue'

// T0-2 三服务联通演示页：FE(5173) → BE(8080) → AI mock(8081)
// 注：此处直接用原生 fetch，正式 request 封装（超时/统一 code/toast）随 F-1 落地替换
const enumsResult = ref('')
const parseResult = ref('')
const loading = ref(false)

async function loadEnums(): Promise<void> {
  loading.value = true
  try {
    const res = await fetch('/api/v1/meta/enums')
    enumsResult.value = JSON.stringify(await res.json(), null, 2)
  } catch (e) {
    enumsResult.value = String(e)
  } finally {
    loading.value = false
  }
}

async function callParse(): Promise<void> {
  loading.value = true
  try {
    const res = await fetch('/api/v1/demo/parse', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ images: [{ url: 'images/demo/q.jpg', role: 'QUESTION' }] }),
    })
    parseResult.value = JSON.stringify(await res.json(), null, 2)
  } catch (e) {
    parseResult.value = String(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="demo">
    <h1>T0-2 三服务联通演示</h1>
    <p>链路：FE(5173) → BE(8080) → AI mock(8081)</p>
    <el-space>
      <el-button :loading="loading" @click="loadEnums"
        >GET /api/v1/meta/enums（BE · yml 枚举）</el-button
      >
      <el-button :loading="loading" @click="callParse"
        >POST /api/v1/demo/parse（BE → AI mock）</el-button
      >
    </el-space>
    <h3>枚举结果</h3>
    <pre class="result">{{ enumsResult || '（未调用）' }}</pre>
    <h3>解析结果（mock）</h3>
    <pre class="result">{{ parseResult || '（未调用）' }}</pre>
  </main>
</template>

<style scoped>
.demo {
  max-width: 900px;
  margin: 24px auto;
  font-family: sans-serif;
}
.result {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  min-height: 40px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
