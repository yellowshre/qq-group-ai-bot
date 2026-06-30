<script setup lang="ts">
import { CopyDocument, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import { getFullHealth, type FullHealthResponse } from '@/api/health'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const loading = ref(false)
const health = ref<FullHealthResponse | null>(null)

const sampleGroupId = computed(() => health.value?.oneBot.allowedGroupIds?.[0] || '736566774')
const privatePrefix = computed(() => health.value?.privateAdmin.commandPrefix || '#')
const rankPrefix = computed(() => health.value?.memberRankCommand.commandPrefix || '#排行')
const activeOffWords = computed(() => health.value?.commandAliases.activeChatOffWords ?? [])
const activeOnWords = computed(() => health.value?.commandAliases.activeChatOnWords ?? [])
const botAliases = computed(() => health.value?.botIdentity.aliases ?? [])

const privateCommands = computed(() => {
  const prefix = privatePrefix.value
  const groupId = sampleGroupId.value
  return [
    `${prefix}群 ${groupId} 状态`,
    `${prefix}群 ${groupId} 开启机器人`,
    `${prefix}群 ${groupId} 关闭机器人`,
    `${prefix}群 ${groupId} 开启表情包`,
    `${prefix}群 ${groupId} 关闭表情包`,
    `${prefix}群 ${groupId} 开启被动聊天`,
    `${prefix}群 ${groupId} 关闭被动聊天`,
    `${prefix}群 ${groupId} 开启主动插话`,
    `${prefix}群 ${groupId} 关闭主动插话`,
    `${prefix}群 ${groupId} 开启知识库`,
    `${prefix}群 ${groupId} 关闭知识库`,
    `${prefix}群 ${groupId} 冷却 300`,
    `${prefix}群 ${groupId} 小时上限 5`,
    `${prefix}群 ${groupId} 每日上限 30`,
    `${prefix}群 ${groupId} 人设 你是式部茉优，像普通群友一样自然聊天……`,
    `${prefix}群 ${groupId} 清空人设`,
  ]
})

const groupCommands = computed(() => [
  ...activeOffWords.value.map((item) => `${item}  # 关闭主动插话`),
  ...activeOnWords.value.map((item) => `${item}  # 开启主动插话`),
  '#状态',
  '#开启表情包',
  '#关闭表情包',
  '#开启知识库',
  '#关闭知识库',
])

const rankCommands = computed(() => {
  const prefix = rankPrefix.value
  const groupId = sampleGroupId.value
  return [
    `${prefix} 发言 top5`,
    `${prefix} 回复 2026-06-01 2026-06-29 top10`,
    `${prefix} 被回复 前5`,
    `${privatePrefix.value}群 ${groupId} 排行 发言 top5`,
    `${privatePrefix.value}群 ${groupId} 排行 回复 2026-06-01 2026-06-29 top10`,
  ]
})

const startupCommands = [
  'copy scripts\\onebot-local.env.example scripts\\onebot-local.env',
  'notepad scripts\\onebot-local.env',
  '.\\scripts\\run-onebot-local.ps1',
]

const envGroups = [
  {
    title: 'OneBot / SnowLuma',
    keys: [
      'QQBOT_ONEBOT_WS_ENABLED',
      'QQBOT_ONEBOT_WS_URL',
      'QQBOT_ONEBOT_WS_TOKEN',
      'QQBOT_ONEBOT_SELF_ID',
      'QQBOT_ONEBOT_ALLOWED_GROUP_IDS',
      'QQBOT_ONEBOT_DRY_RUN',
      'QQBOT_ADMINS',
    ],
  },
  {
    title: 'Dify A/B/C',
    keys: [
      'DIFY_ENABLED',
      'DIFY_BASE_URL',
      'DIFY_MEME_SCENE_API_KEY',
      'DIFY_PASSIVE_CHAT_API_KEY',
      'DIFY_ACTIVE_CHAT_API_KEY',
      'DIFY_MEME_SCENE_WORKFLOW',
      'DIFY_PASSIVE_CHAT_WORKFLOW',
      'DIFY_ACTIVE_CHAT_WORKFLOW',
    ],
  },
  {
    title: '身份与触发',
    keys: [
      'QQBOT_DISPLAY_NAME',
      'QQBOT_ALIAS_PRIMARY',
      'QQBOT_ALIAS_SECONDARY',
      'QQBOT_ALIAS_FALLBACK',
      'QQBOT_PASSIVE_CHAT_TRIGGER_WORDS',
      'QQBOT_DEFAULT_PERSONA',
    ],
  },
  {
    title: '群内 / 私聊运维',
    keys: [
      'QQBOT_CMD_ACTIVE_CHAT_OFF_WORDS',
      'QQBOT_CMD_ACTIVE_CHAT_ON_WORDS',
      'QQBOT_PRIVATE_ADMIN_COMMAND_ENABLED',
      'QQBOT_PRIVATE_ADMIN_LIMIT_TO_ALLOWED_GROUPS',
      'QQBOT_PRIVATE_ADMIN_COMMAND_PREFIX',
      'QQBOT_ADMIN_REPLY_SUCCESS',
    ],
  },
  {
    title: '知识库 / 排行',
    keys: [
      'QQBOT_KNOWLEDGE_EMBEDDING_ENABLED',
      'OLLAMA_BASE_URL',
      'OLLAMA_EMBEDDING_MODEL',
      'QQBOT_MEMBER_RANK_ENABLED',
      'QQBOT_MEMBER_RANK_GROUP_COMMAND_ENABLED',
      'QQBOT_MEMBER_RANK_PRIVATE_COMMAND_ENABLED',
      'QQBOT_MEMBER_RANK_COMMAND_PREFIX',
    ],
  },
]

async function loadHealth() {
  loading.value = true
  try {
    health.value = await getFullHealth()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '运维手册配置加载失败')
  } finally {
    loading.value = false
  }
}

async function copyLines(lines: string[], successMessage: string) {
  await copyText(lines.join('\n'), successMessage)
}

async function copyText(text: string, successMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.error('复制失败，可以手动选中内容')
  }
}

function listText(values: string[]) {
  return values.length ? values.join(' / ') : '-'
}

onMounted(loadHealth)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="运维手册"
      description="把真实联调启动、群内命令、私聊控制、排行查询和 env key 清单集中在一个本地速查页。"
    >
      <template #eyebrow>Runbook</template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadHealth">刷新当前配置</el-button>
      </template>
    </PageHeader>

    <section class="status-grid">
      <div class="metric-card">
        <span class="metric-label">Private admin</span>
        <strong class="metric-value">{{ health?.privateAdmin.enabled ? 'ON' : 'OFF' }}</strong>
        <span class="metric-hint">非管理员私聊默认无回应</span>
      </div>
      <div class="metric-card">
        <span class="metric-label">Allowed group</span>
        <strong class="metric-value">{{ sampleGroupId }}</strong>
        <span class="metric-hint">示例命令会使用第一个白名单群</span>
      </div>
      <div class="metric-card">
        <span class="metric-label">Rank command</span>
        <strong class="metric-value">{{ health?.memberRankCommand.enabled ? 'ON' : 'OFF' }}</strong>
        <span class="metric-hint">{{ rankPrefix }}</span>
      </div>
      <div class="metric-card">
        <span class="metric-label">Bot aliases</span>
        <strong class="metric-value">{{ botAliases.length }}</strong>
        <span class="metric-hint">{{ listText(botAliases) }}</span>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>真实联调启动</h3>
            <span class="panel-subtitle">不会显示 token 和 Dify key</span>
          </div>
          <el-button :icon="CopyDocument" @click="copyLines(startupCommands, '启动命令已复制')">复制</el-button>
        </div>
        <div class="code-lines">
          <code v-for="line in startupCommands" :key="line">{{ line }}</code>
        </div>
        <div class="runbook-note">
          先复制 env 模板，填入真实 SnowLuma token / Dify key / 管理员 QQ，再用脚本启动 local profile。
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>当前安全状态</h3>
          <span class="panel-subtitle">只读诊断</span>
        </div>
        <div class="settings-list">
          <StatusBadge label="OneBot WS" :active="health?.oneBot.wsEnabled" />
          <StatusBadge label="Dify" :active="health?.dify.enabled" />
          <StatusBadge label="Admin token" :active="health?.adminUi.apiTokenProtected" />
          <StatusBadge label="私聊控制" :active="health?.privateAdmin.enabled" />
          <StatusBadge label="限制白名单群" :active="health?.privateAdmin.limitToAllowedGroups" />
          <StatusBadge label="排行群内公开" :active="health?.memberRankCommand.groupCommandEnabled" />
        </div>
        <div class="settings-kv">
          <span>message sender</span>
          <strong>{{ health?.messageSenderType || '-' }}</strong>
          <span>meme base dir</span>
          <strong>{{ health?.memeBaseDir || '-' }}</strong>
          <span>allowed groups</span>
          <strong>{{ health?.oneBot.allowedGroupIds?.join(', ') || '-' }}</strong>
        </div>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>私聊控制指令</h3>
            <span class="panel-subtitle">管理员私聊机器人，不在群里暴露控制动作</span>
          </div>
          <el-button :icon="CopyDocument" @click="copyLines(privateCommands, '私聊控制指令已复制')">复制</el-button>
        </div>
        <div class="command-list">
          <code v-for="line in privateCommands" :key="line">{{ line }}</code>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>群内控制指令</h3>
            <span class="panel-subtitle">公开出现在群里，真实群建议少用自然语言别名</span>
          </div>
          <el-button :icon="CopyDocument" @click="copyLines(groupCommands, '群内控制指令已复制')">复制</el-button>
        </div>
        <div class="command-list">
          <code v-for="line in groupCommands" :key="line">{{ line }}</code>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>成员排行指令</h3>
          <span class="panel-subtitle">群内公开与私聊查询由 env 开关分别控制</span>
        </div>
        <el-button :icon="CopyDocument" @click="copyLines(rankCommands, '排行指令已复制')">复制</el-button>
      </div>
      <div class="command-grid">
        <code v-for="line in rankCommands" :key="line">{{ line }}</code>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>Env Key 清单</h3>
        <span class="panel-subtitle">只列 key，不显示真实 value</span>
      </div>
      <div class="env-key-grid">
        <div v-for="group in envGroups" :key="group.title" class="env-key-card">
          <h4>{{ group.title }}</h4>
          <code v-for="key in group.keys" :key="key">{{ key }}</code>
        </div>
      </div>
    </section>

    <section class="panel muted-panel">
      <h3>前端与私聊入口的边界</h3>
      <p>
        前端适合做完整、可视化、可审计的日常运维；私聊控制适合手机 QQ 上的应急操作。
        两者都应复用后端配置服务，真实密钥仍只放在本地 env 文件中。
      </p>
      <p>
        不在管理员白名单里的私聊用户不会得到任何回复，这一点是刻意设计的：不暴露机器人存在控制入口。
      </p>
    </section>
  </div>
</template>
