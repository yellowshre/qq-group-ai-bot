import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { title: '总览' },
    },
    {
      path: '/groups',
      name: 'groups',
      component: () => import('@/views/GroupConfigView.vue'),
      meta: { title: '群配置' },
    },
    {
      path: '/memes',
      name: 'memes',
      component: () => import('@/views/MemeMaterialView.vue'),
      meta: { title: '表情包素材' },
    },
    {
      path: '/knowledge',
      name: 'knowledge',
      component: () => import('@/views/KnowledgeView.vue'),
      meta: { title: '知识库' },
    },
    {
      path: '/pipeline',
      name: 'pipeline',
      component: () => import('@/views/ChatPipelineView.vue'),
      meta: { title: '数据流水线' },
    },
    {
      path: '/member-rank',
      name: 'member-rank',
      component: () => import('@/views/MemberRankView.vue'),
      meta: { title: '成员排行' },
    },
    {
      path: '/simulate',
      name: 'simulate',
      component: () => import('@/views/MessageSimulationView.vue'),
      meta: { title: '消息模拟' },
    },
    {
      path: '/logs',
      name: 'logs',
      component: () => import('@/views/LogDiagnosisView.vue'),
      meta: { title: '运行日志' },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/RuntimeSettingsView.vue'),
      meta: { title: '运行配置' },
    },
    {
      path: '/runbook',
      name: 'runbook',
      component: () => import('@/views/OpsRunbookView.vue'),
      meta: { title: '运维手册' },
    },
  ],
})

export default router
