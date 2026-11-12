<script setup lang="ts">
// 根布局（prototype-spec §1）：左侧三菜单（错题记录/运行日志/数据备份）+ 主内容区。
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const activeMenu = computed(() => '/' + (route.path.split('/')[1] ?? 'questions'))
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="200px" class="app-aside">
      <div class="app-logo">📓 错题本</div>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/questions">错题记录</el-menu-item>
        <el-menu-item index="/logs">运行日志</el-menu-item>
        <el-menu-item index="/backup">数据备份</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}
.app-aside {
  border-right: 1px solid var(--el-border-color-light);
}
.app-logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: 600;
  color: var(--el-color-primary);
  border-bottom: 1px solid var(--el-border-color-light);
}
</style>
