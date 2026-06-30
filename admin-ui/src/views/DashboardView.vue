<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { getFullHealth, type FullHealthResponse } from '@/api/health'
import { getAdminOverview, type AdminOverviewResponse } from '@/api/overview'
import MetricCard from '@/components/common/MetricCard.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const loading = ref(false)
const health = ref<FullHealthResponse | null>(null)
const overview = ref<AdminOverviewResponse | null>(null)
const groupId = ref('')

const profiles = computed(() => health.value?.activeProfiles?.join(', ') ?? '-')
const keyStatus = computed(() => {
  const dify = health.value?.dify
  if (!dify) return []
  return [
    { label: 'A 表情包', active: dify.memeSceneApiKeyConfigured },
    { label: 'B 被动聊天', active: dify.passiveChatApiKeyConfigured },
    { label: 'C 主动插话', active: dify.activeChatApiKeyConfigured },
  ]
})
const latestImport = computed(() => overview.value?.latestImport ?? null)
const pendingTotal = computed(() =>
  Number(overview.value?.pendingKnowledgeCandidates ?? 0)
  + Number(overview.value?.pendingMemberCandidates ?? 0),
)
const readiness = computed(() => [
  { label: 'MySQL', active: Boolean(health.value?.mysql.reachable), detail: health.value?.mysql.detail || 'database' },
  { label: 'Redis', active: Boolean(health.value?.redis.reachable), detail: health.value?.redis.detail || 'cache' },
  { label: 'OneBot WS', active: Boolean(health.value?.oneBot.wsEnabled), detail: health.value?.oneBot.selfId || 'not enabled' },
  { label: 'Dify', active: Boolean(health.value?.dify.enabled), detail: health.value?.dify.baseUrlConfigured ? 'base url ok' : 'base url empty' },
  { label: 'A key', active: Boolean(health.value?.dify.memeSceneApiKeyConfigured), detail: 'meme scene' },
  { label: 'B key', active: Boolean(health.value?.dify.passiveChatApiKeyConfigured), detail: 'passive chat' },
  { label: 'C key', active: Boolean(health.value?.dify.activeChatApiKeyConfigured), detail: 'active chat' },
  { label: 'Sender', active: Boolean(health.value?.messageSenderType), detail: health.value?.messageSenderType || '-' },
])
const pipelineSteps = computed(() => [
  { label: '导入批次', value: overview.value?.importBatches, hint: 'chat_import_batch' },
  { label: '清洗消息', value: overview.value?.cleanMessages, hint: 'chat_clean_message' },
  { label: '待审候选', value: pendingTotal.value, hint: 'knowledge/member candidates' },
  { label: '正式知识', value: overview.value?.enabledGroupKnowledge, hint: 'enabled group knowledge' },
  { label: '成员画像', value: overview.value?.enabledMemberProfiles, hint: 'enabled member profile' },
  { label: 'Embedding', value: overview.value?.successfulEmbeddings, hint: 'SUCCESS' },
])
const adminLinks = [
  { title: '群配置', description: '调整 A/B/C、知识灰度、人设和冷却上限', to: '/groups' },
  { title: '知识审批', description: '生成、审批、发布、embedding 和召回预览', to: '/knowledge' },
  { title: '表情包素材', description: '维护 scene_dict、关键词、权重和相对路径', to: '/memes' },
  { title: '运行日志', description: '排查真实群路由、静默原因和管理员操作', to: '/logs' },
]

async function loadHealth() {
  loading.value = true
  try {
    const normalizedGroupId = groupId.value.trim() || undefined
    const [healthResponse, overviewResponse] = await Promise.all([
      getFullHealth(normalizedGroupId),
      getAdminOverview(normalizedGroupId),
    ])
    health.value = healthResponse
    overview.value = overviewResponse
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '健康检查请求失败')
  } finally {
    loading.value = false
  }
}

function display(value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  return String(value)
}

onMounted(loadHealth)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="机器人运行总览"
      description="快速确认后端、Redis、MySQL、OneBot、Dify 和知识库上下文是否处于可用状态。"
    >
      <template #eyebrow>Dashboard</template>
      <template #actions>
        <el-input
          v-model="groupId"
          class="group-input"
          placeholder="可选 groupId"
          clearable
          @keyup.enter="loadHealth"
        />
        <el-button :icon="Refresh" :loading="loading" @click="loadHealth">
          刷新
        </el-button>
      </template>
    </PageHeader>

    <section class="status-grid">
      <MetricCard label="Active profile" :value="profiles" />
      <MetricCard label="Message sender" :value="health?.messageSenderType" />
      <MetricCard label="Scene dict" :value="health?.sceneDictCount" hint="场景数量" />
      <MetricCard label="Enabled memes" :value="health?.enabledMemeMaterialCount" hint="启用素材" />
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>运行就绪清单</h3>
          <span class="panel-subtitle">只展示布尔状态和非敏感摘要</span>
        </div>
        <span class="panel-subtitle">generatedAt {{ display(overview?.generatedAt) }}</span>
      </div>
      <div class="readiness-grid">
        <div
          v-for="item in readiness"
          :key="item.label"
          class="readiness-item"
          :class="{ active: item.active }"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.active ? 'OK' : 'CHECK' }}</strong>
          <small>{{ item.detail }}</small>
        </div>
      </div>
    </section>

    <section class="status-grid">
      <MetricCard label="Import batches" :value="overview?.importBatches" hint="导入批次" />
      <MetricCard label="Clean messages" :value="overview?.cleanMessages" hint="清洗消息" />
      <MetricCard label="Formal knowledge" :value="overview?.enabledGroupKnowledge" hint="启用知识" />
      <MetricCard label="Member profiles" :value="overview?.enabledMemberProfiles" hint="启用画像" />
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <h3>数据流水线</h3>
          <span class="panel-subtitle">导入 -> 审核 -> 正式知识 -> embedding</span>
        </div>
        <div class="pipeline-grid">
          <div v-for="item in pipelineSteps" :key="item.label" class="pipeline-step">
            <span>{{ item.label }}</span>
            <strong>{{ display(item.value) }}</strong>
            <small>{{ item.hint }}</small>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>常用入口</h3>
          <span class="panel-subtitle">按日常运维顺序排列</span>
        </div>
        <div class="admin-link-grid">
          <RouterLink v-for="item in adminLinks" :key="item.to" class="admin-link-card" :to="item.to">
            <span>{{ item.title }}</span>
            <small>{{ item.description }}</small>
          </RouterLink>
        </div>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <h3>依赖状态</h3>
          <span class="panel-subtitle">数据库与缓存</span>
        </div>
        <div class="status-list">
          <StatusBadge
            label="MySQL"
            :active="health?.mysql.reachable"
            :active-text="health?.mysql.detail || 'ok'"
            :inactive-text="health?.mysql.detail || 'down'"
          />
          <StatusBadge
            label="Redis"
            :active="health?.redis.reachable"
            :active-text="health?.redis.detail || 'ok'"
            :inactive-text="health?.redis.detail || 'down'"
          />
          <StatusBadge label="Meme cache preheat" :active="health?.memeCachePreheatEnabled" />
          <StatusBadge label="Knowledge embedding" :active="health?.knowledgeEmbeddingEnabled" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>OneBot / Dify</h3>
          <span class="panel-subtitle">不显示 token 与 key</span>
        </div>
        <div class="status-list">
          <StatusBadge label="OneBot WS" :active="health?.oneBot.wsEnabled" />
          <StatusBadge label="Dify" :active="health?.dify.enabled" />
          <StatusBadge label="Base URL" :active="health?.dify.baseUrlConfigured" />
        </div>
        <div class="key-row">
          <StatusBadge
            v-for="item in keyStatus"
            :key="item.label"
            :label="item.label"
            :active="item.active"
            active-text="configured"
            inactive-text="empty"
          />
        </div>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <h3>聊天历史数据</h3>
          <span class="panel-subtitle">{{ overview?.groupId ? `group ${overview.groupId}` : '全部群' }}</span>
        </div>
        <div class="settings-kv">
          <span>raw messages</span>
          <strong>{{ display(overview?.rawMessages) }}</strong>
          <span>clean messages</span>
          <strong>{{ display(overview?.cleanMessages) }}</strong>
          <span>sessions</span>
          <strong>{{ display(overview?.sessions) }}</strong>
          <span>member stats</span>
          <strong>{{ display(overview?.memberStats) }}</strong>
          <span>trigger logs today</span>
          <strong>{{ display(overview?.triggerLogsToday) }}</strong>
          <span>admin ops today</span>
          <strong>{{ display(overview?.adminOpsToday) }}</strong>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>知识库流水线</h3>
          <span class="panel-subtitle">候选 / 正式 / embedding</span>
        </div>
        <div class="settings-kv">
          <span>knowledge candidates</span>
          <strong>{{ display(overview?.knowledgeCandidates) }}</strong>
          <span>pending knowledge</span>
          <strong>{{ display(overview?.pendingKnowledgeCandidates) }}</strong>
          <span>member candidates</span>
          <strong>{{ display(overview?.memberCandidates) }}</strong>
          <span>pending members</span>
          <strong>{{ display(overview?.pendingMemberCandidates) }}</strong>
          <span>active knowledge</span>
          <strong>{{ display(overview?.activeGroupKnowledge) }}</strong>
          <span>successful embeddings</span>
          <strong>{{ display(overview?.successfulEmbeddings) }}</strong>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>最近导入</h3>
        <span class="panel-subtitle">chat_import_batch</span>
      </div>
      <div v-if="latestImport" class="settings-kv">
        <span>batchId</span>
        <strong>{{ latestImport.batchId }}</strong>
        <span>status</span>
        <strong>{{ latestImport.status }}</strong>
        <span>raw / clean</span>
        <strong>{{ latestImport.rawCount }} / {{ latestImport.cleanCount }}</strong>
        <span>sessions / members</span>
        <strong>{{ latestImport.sessionCount }} / {{ latestImport.memberCount }}</strong>
        <span>createdAt</span>
        <strong>{{ latestImport.createdAt }}</strong>
        <span>sourceFile</span>
        <strong>{{ latestImport.sourceFile }}</strong>
      </div>
      <div v-else class="empty-block compact">
        暂无导入批次，或当前数据库还未执行第十阶段聊天历史迁移。
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>当前群配置</h3>
        <span class="panel-subtitle">输入 groupId 后随健康检查一起返回</span>
      </div>
      <div v-if="health?.groupConfig" class="switch-matrix">
        <StatusBadge label="总开关" :active="health.groupConfig.botOn" />
        <StatusBadge label="表情包 A" :active="health.groupConfig.enableMeme" />
        <StatusBadge label="被动聊天 B" :active="health.groupConfig.enablePassiveChat" />
        <StatusBadge label="主动插话 C" :active="health.groupConfig.enableAutoJoin" />
        <StatusBadge label="知识库总开关" :active="health.groupConfig.enableKnowledgeContext" />
        <StatusBadge label="表情包知识" :active="health.groupConfig.enableMemeKnowledge" />
        <StatusBadge label="聊天知识" :active="health.groupConfig.enablePassiveChatKnowledge" />
        <StatusBadge label="主动知识" :active="health.groupConfig.enableActiveChatKnowledge" />
      </div>
      <div v-else class="empty-block">
        暂未选择群。输入 groupId 后刷新，可以查看该群灰度开关。
      </div>
    </section>
  </div>
</template>
