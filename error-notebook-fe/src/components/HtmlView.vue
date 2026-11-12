<script setup lang="ts">
// 富文本唯一渲染出口（security.md §4）：html 必须来自 utils/markdown.ts renderMarkdown
// 的消毒输出——除此之外任何位置禁止 v-html（ESLint vue/no-v-html error，见 .eslintrc.cjs）。
defineProps<{ html: string }>()
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -- 唯一许可出口：输入必须经 DOMPurify 消毒 -->
  <div class="html-view" v-html="html"></div>
</template>

<style scoped>
.html-view {
  line-height: 1.8;
  word-break: break-word;
}
.html-view :deep(img) {
  max-width: 100%;
}
.html-view :deep(table) {
  border-collapse: collapse;
}
.html-view :deep(td),
.html-view :deep(th) {
  border: 1px solid var(--el-border-color-light);
  padding: 4px 8px;
}
</style>
