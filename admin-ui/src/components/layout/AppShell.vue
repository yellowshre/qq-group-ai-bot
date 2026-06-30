<script setup lang="ts">
import {
  ChatDotRound,
  Connection,
  DataAnalysis,
  Files,
  Picture,
  Operation,
  Tickets,
  Setting,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import { getAdminApiToken, setAdminApiToken } from '@/api/client'

const route = useRoute()
const adminToken = ref('')

const navItems = [
  { to: '/', label: '总览', description: '运行状态', icon: DataAnalysis },
  { to: '/groups', label: '群配置', description: '灰度开关', icon: Operation },
  { to: '/memes', label: '表情包', description: '素材路径', icon: Picture },
  { to: '/knowledge', label: '知识库', description: '导入审批', icon: Files },
  { to: '/pipeline', label: '流水线', description: '导入发布', icon: Files },
  { to: '/member-rank', label: '成员排行', description: '统计查询', icon: ChatDotRound },
  { to: '/simulate', label: '消息模拟', description: '路由调试', icon: ChatDotRound },
  { to: '/logs', label: '运行日志', description: '路由诊断', icon: Tickets },
  { to: '/settings', label: '运行配置', description: '本地联调', icon: Setting },
  { to: '/runbook', label: '运维手册', description: '指令参考', icon: Tickets },
]

const pageTitle = computed(() => String(route.meta.title ?? 'QQbot Admin'))

onMounted(() => {
  adminToken.value = getAdminApiToken()
})

function saveAdminToken() {
  setAdminApiToken(adminToken.value)
  if (adminToken.value.trim()) {
    ElMessage.success('Admin API token 已保存')
  } else {
    ElMessage.success('Admin API token 已清空')
  }
}

function clearAdminToken() {
  adminToken.value = ''
  setAdminApiToken('')
  ElMessage.success('Admin API token 已清空')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/">
        <div class="brand-mark">
          <Connection />
        </div>
        <div>
          <div class="brand-title">QQbot Admin</div>
          <div class="brand-subtitle">local control plane</div>
        </div>
      </RouterLink>

      <nav class="nav-list" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          class="nav-item"
          :to="item.to"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>
            <span class="nav-label">{{ item.label }}</span>
            <span class="nav-desc">{{ item.description }}</span>
          </span>
        </RouterLink>
      </nav>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">SnowLuma / Dify / Knowledge</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="topbar-actions">
          <el-input
            v-model="adminToken"
            class="token-input"
            type="password"
            show-password
            clearable
            placeholder="Admin API token"
            @keyup.enter="saveAdminToken"
          />
          <el-button @click="saveAdminToken">保存 Token</el-button>
          <el-button @click="clearAdminToken">清除</el-button>
          <span class="local-pill">dev / local</span>
        </div>
      </header>

      <main class="page-frame">
        <RouterView />
      </main>
    </div>
  </div>
</template>
