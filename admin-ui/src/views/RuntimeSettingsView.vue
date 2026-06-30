<script setup lang="ts">
import { CopyDocument, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { getFullHealth, type FullHealthResponse } from '@/api/health'
import MetricCard from '@/components/common/MetricCard.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { useLastGroupId } from '@/composables/useAdminPreferences'

const loading = ref(false)
const groupId = useLastGroupId()
const health = ref<FullHealthResponse | null>(null)

const profiles = computed(() => health.value?.activeProfiles?.join(', ') || '-')
const allowedGroups = computed(() => health.value?.oneBot.allowedGroupIds?.join(', ') || '-')
const botAliases = computed(() => health.value?.botIdentity.aliases ?? [])
const activeChatOffWords = computed(() => health.value?.commandAliases.activeChatOffWords ?? [])
const activeChatOnWords = computed(() => health.value?.commandAliases.activeChatOnWords ?? [])
const diagnosticSummary = computed(() => {
  const value = health.value
  if (!value) return ''
  return [
    'QQbot runtime diagnostics',
    `profiles=${profiles.value}`,
    `messageSender=${value.messageSenderType}`,
    `mysql=${value.mysql.reachable}`,
    `redis=${value.redis.reachable}`,
    `onebotWs=${value.oneBot.wsEnabled}`,
    `onebotSelfIdConfigured=${Boolean(value.oneBot.selfId)}`,
    `allowedGroups=${allowedGroups.value}`,
    `difyEnabled=${value.dify.enabled}`,
    `difyBaseUrlConfigured=${value.dify.baseUrlConfigured}`,
    `memeSceneWorkflowConfigured=${value.dify.memeSceneWorkflowConfigured}`,
    `passiveChatWorkflowConfigured=${value.dify.passiveChatWorkflowConfigured}`,
    `activeChatWorkflowConfigured=${value.dify.activeChatWorkflowConfigured}`,
    `memeSceneApiKeyConfigured=${value.dify.memeSceneApiKeyConfigured}`,
    `passiveChatApiKeyConfigured=${value.dify.passiveChatApiKeyConfigured}`,
    `activeChatApiKeyConfigured=${value.dify.activeChatApiKeyConfigured}`,
    `memeBaseDir=${value.memeBaseDir}`,
    `privateAdminEnabled=${value.privateAdmin.enabled}`,
    `memberRankEnabled=${value.memberRankCommand.enabled}`,
    `knowledgeContextEnabled=${value.knowledgeContextEnabled}`,
    value.groupConfig
      ? `groupConfig=${value.groupConfig.groupId}, bot=${value.groupConfig.botOn}, A=${value.groupConfig.enableMeme}, B=${value.groupConfig.enablePassiveChat}, C=${value.groupConfig.enableAutoJoin}, K=${value.groupConfig.enableKnowledgeContext}`
      : 'groupConfig=not loaded',
  ].join('\n')
})
const difyWorkflowStatus = computed(() => {
  const dify = health.value?.dify
  if (!dify) return []
  return [
    { label: 'A workflow', active: dify.memeSceneWorkflowConfigured },
    { label: 'B workflow', active: dify.passiveChatWorkflowConfigured },
    { label: 'C workflow', active: dify.activeChatWorkflowConfigured },
  ]
})

function listText(values?: string[]) {
  return values?.length ? values.join(' / ') : '-'
}
const difyKeyStatus = computed(() => {
  const dify = health.value?.dify
  if (!dify) return []
  return [
    { label: 'A api key', active: dify.memeSceneApiKeyConfigured },
    { label: 'B api key', active: dify.passiveChatApiKeyConfigured },
    { label: 'C api key', active: dify.activeChatApiKeyConfigured },
  ]
})

async function loadHealth() {
  loading.value = true
  try {
    health.value = await getFullHealth(groupId.value.trim() || undefined)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运行配置加载失败')
  } finally {
    loading.value = false
  }
}

async function copyDiagnostics() {
  if (!diagnosticSummary.value) {
    ElMessage.warning('请先刷新运行配置')
    return
  }
  try {
    await navigator.clipboard.writeText(diagnosticSummary.value)
    ElMessage.success('诊断摘要已复制，不包含 token 或 api key')
  } catch {
    ElMessage.error('复制失败，可以手动查看页面状态')
  }
}

onMounted(loadHealth)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="运行配置"
      description="查看当前 Spring Boot 进程读取到的非敏感配置状态，用于真实联调前排查。"
    >
      <template #eyebrow>Settings</template>
      <template #actions>
        <el-input
          v-model="groupId"
          class="group-input"
          placeholder="可选 groupId"
          clearable
          @keyup.enter="loadHealth"
        />
        <el-button :icon="CopyDocument" :disabled="!health" @click="copyDiagnostics">复制诊断摘要</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadHealth">刷新</el-button>
      </template>
    </PageHeader>

    <section class="status-grid">
      <MetricCard label="Active profiles" :value="profiles" />
      <MetricCard label="Message sender" :value="health?.messageSenderType" />
      <MetricCard label="Meme base dir" :value="health?.memeBaseDir" />
      <MetricCard label="Allowed groups" :value="allowedGroups" />
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <h3>基础依赖</h3>
          <span class="panel-subtitle">MySQL / Redis / 素材缓存</span>
        </div>
        <div class="settings-list">
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
        <div class="settings-kv">
          <span>scene_dict</span>
          <strong>{{ health?.sceneDictCount ?? '-' }}</strong>
          <span>enabled meme_material</span>
          <strong>{{ health?.enabledMemeMaterialCount ?? '-' }}</strong>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>OneBot</h3>
          <span class="panel-subtitle">不显示 token</span>
        </div>
        <div class="settings-list">
          <StatusBadge label="WebSocket" :active="health?.oneBot.wsEnabled" />
          <StatusBadge label="Self ID" :active="Boolean(health?.oneBot.selfId)" :active-text="health?.oneBot.selfId || 'set'" />
        </div>
        <div class="settings-kv single">
          <span>allowedGroupIds</span>
          <strong>{{ allowedGroups }}</strong>
        </div>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <h3>Dify</h3>
          <span class="panel-subtitle">只显示 configured=true/false</span>
        </div>
        <div class="settings-list">
          <StatusBadge label="Enabled" :active="health?.dify.enabled" />
          <StatusBadge label="Base URL" :active="health?.dify.baseUrlConfigured" />
        </div>
        <div class="key-row">
          <StatusBadge
            v-for="item in difyWorkflowStatus"
            :key="item.label"
            :label="item.label"
            :active="item.active"
            active-text="configured"
            inactive-text="empty"
          />
        </div>
        <div class="key-row">
          <StatusBadge
            v-for="item in difyKeyStatus"
            :key="item.label"
            :label="item.label"
            :active="item.active"
            active-text="configured"
            inactive-text="empty"
          />
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>知识上下文</h3>
          <span class="panel-subtitle">召回前置限制</span>
        </div>
        <div class="settings-list">
          <StatusBadge label="全局 embedding" :active="health?.knowledgeEmbeddingEnabled" />
          <StatusBadge label="群知识开关" :active="health?.knowledgeContextEnabled" />
          <StatusBadge label="表情包知识" :active="health?.memeKnowledgeEnabled" />
          <StatusBadge label="聊天知识" :active="health?.passiveChatKnowledgeEnabled" />
          <StatusBadge label="主动知识" :active="health?.activeChatKnowledgeEnabled" />
        </div>
        <div class="settings-kv">
          <span>maxItems</span>
          <strong>{{ health?.knowledgeContextConfig.maxItems ?? '-' }}</strong>
          <span>maxLength</span>
          <strong>{{ health?.knowledgeContextConfig.maxLength ?? '-' }}</strong>
          <span>minScore</span>
          <strong>{{ health?.knowledgeContextConfig.minScore ?? '-' }}</strong>
          <span>memberProfileLimit</span>
          <strong>{{ health?.knowledgeContextConfig.memberProfileLimit ?? '-' }}</strong>
          <span>maxSearchCandidates</span>
          <strong>{{ health?.knowledgeContextConfig.maxSearchCandidates ?? '-' }}</strong>
          <span>maxItemContentLength</span>
          <strong>{{ health?.knowledgeContextConfig.maxItemContentLength ?? '-' }}</strong>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>Admin UI</h3>
        <span class="panel-subtitle">只显示本地控制台保护状态</span>
      </div>
      <div class="settings-list">
        <StatusBadge label="Token 开关" :active="health?.adminUi.apiTokenEnabled" />
        <StatusBadge label="Token 已配置" :active="health?.adminUi.apiTokenConfigured" />
        <StatusBadge label="/dev/* 保护" :active="health?.adminUi.apiTokenProtected" />
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <h3>身份与群内指令</h3>
          <span class="panel-subtitle">env 读取结果，只读展示</span>
        </div>
        <div class="settings-list">
          <StatusBadge label="管理员白名单" :active="health?.adminAccess.adminsConfigured" />
          <StatusBadge label="默认人设" :active="health?.botIdentity.defaultPersonaConfigured" />
        </div>
        <div class="settings-kv command-kv">
          <span>管理员数量</span>
          <strong>{{ health?.adminAccess.adminCount ?? 0 }}</strong>
          <span>机器人显示名</span>
          <strong>{{ health?.botIdentity.displayName || '-' }}</strong>
          <span>昵称触发词</span>
          <strong>{{ listText(botAliases) }}</strong>
          <span>主动插话关闭词</span>
          <strong>{{ listText(activeChatOffWords) }}</strong>
          <span>主动插话开启词</span>
          <strong>{{ listText(activeChatOnWords) }}</strong>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>私聊控制</h3>
          <span class="panel-subtitle">不显示管理员 QQ 号</span>
        </div>
        <div class="settings-list">
          <StatusBadge label="私聊控制开关" :active="health?.privateAdmin.enabled" />
          <StatusBadge label="限制白名单群" :active="health?.privateAdmin.limitToAllowedGroups" />
        </div>
        <div class="settings-kv command-kv">
          <span>指令前缀</span>
          <strong>{{ health?.privateAdmin.commandPrefix || '#' }}</strong>
          <span>关闭时回复</span>
          <strong>{{ health?.privateAdmin.replies.disabled || '-' }}</strong>
          <span>群不允许回复</span>
          <strong>{{ health?.privateAdmin.replies.groupNotAllowed || '-' }}</strong>
          <span>未知指令回复</span>
          <strong>{{ health?.privateAdmin.replies.unknownCommand || '-' }}</strong>
          <span>成功回复</span>
          <strong>{{ health?.privateAdmin.replies.success || '-' }}</strong>
          <span>状态前缀</span>
          <strong>{{ health?.privateAdmin.replies.statusPrefix || '-' }}</strong>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>排行指令</h3>
        <span class="panel-subtitle">群内和私聊的公开排行查询开关</span>
      </div>
      <div class="settings-list">
        <StatusBadge label="排行模块" :active="health?.memberRankCommand.enabled" />
        <StatusBadge label="群内指令" :active="health?.memberRankCommand.groupCommandEnabled" />
        <StatusBadge label="私聊指令" :active="health?.memberRankCommand.privateCommandEnabled" />
        <StatusBadge label="仅管理员" :active="health?.memberRankCommand.adminOnly" />
      </div>
      <div class="settings-kv">
        <span>指令前缀</span>
        <strong>{{ health?.memberRankCommand.commandPrefix || '#排行' }}</strong>
        <span>默认 TopN</span>
        <strong>{{ health?.memberRankCommand.defaultTopN ?? '-' }}</strong>
        <span>最大 TopN</span>
        <strong>{{ health?.memberRankCommand.maxTopN ?? '-' }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>指定群配置快照</h3>
        <span class="panel-subtitle">输入 groupId 后刷新</span>
      </div>
      <div v-if="health?.groupConfig" class="settings-list">
        <StatusBadge label="机器人总开关" :active="health.groupConfig.botOn" />
        <StatusBadge label="聊天总开关" :active="health.groupConfig.enableChat" />
        <StatusBadge label="表情包 A" :active="health.groupConfig.enableMeme" />
        <StatusBadge label="被动聊天 B" :active="health.groupConfig.enablePassiveChat" />
        <StatusBadge label="主动插话 C" :active="health.groupConfig.enableAutoJoin" />
        <StatusBadge label="知识库总开关" :active="health.groupConfig.enableKnowledgeContext" />
        <StatusBadge label="表情包知识" :active="health.groupConfig.enableMemeKnowledge" />
        <StatusBadge label="聊天知识" :active="health.groupConfig.enablePassiveChatKnowledge" />
        <StatusBadge label="主动知识" :active="health.groupConfig.enableActiveChatKnowledge" />
      </div>
      <div v-else class="empty-block">
        当前未加载群配置。这里不会显示密钥，只用于确认灰度开关状态。
      </div>
    </section>
  </div>
</template>
