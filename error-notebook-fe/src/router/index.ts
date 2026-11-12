import { createRouter, createWebHistory } from 'vue-router'

/** 正式路由（prototype-spec §1：三菜单 + 新增/编辑独立页；/demo 为 T0-2 联通演示保留）。 */
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/questions' },
    {
      path: '/questions',
      name: 'questions',
      component: () => import('../views/QuestionListView.vue'),
    },
    {
      path: '/questions/new',
      name: 'question-create',
      component: () => import('../views/QuestionCreateView.vue'),
    },
    { path: '/logs', name: 'logs', component: () => import('../views/LogsView.vue') },
    { path: '/backup', name: 'backup', component: () => import('../views/BackupView.vue') },
    {
      path: '/demo',
      name: 'demo',
      component: () => import('../views/DemoView.vue'),
    },
  ],
})

export default router
