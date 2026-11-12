/* eslint-env node */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:@typescript-eslint/recommended',
    'prettier',
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 'latest',
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  rules: {
    // v-html 全局禁止（security.md §4）：唯一出口为 components/HtmlView.vue（输入必须经
    // utils/markdown.ts renderMarkdown 的 DOMPurify 消毒），该文件内以 disable-next-line 豁免
    'vue/no-v-html': 'error',
    'vue/multi-word-component-names': ['error', { ignores: ['App'] }],
    '@typescript-eslint/no-explicit-any': 'warn',
  },
  ignorePatterns: ['dist/', 'node_modules/', 'coverage/', '*.cjs'],
}
