import { createRouter, createWebHistory } from 'vue-router'

// T0-2 演示路由；正式路由（/questions、/questions/new、/logs、/backup）随 F-2~F-5 落地
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/demo' },
    {
      path: '/demo',
      name: 'demo',
      component: () => import('../views/DemoView.vue'),
    },
  ],
})

export default router
