<script setup lang="ts">
import { CopyDocument, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import {
  simulatePrivateMessage,
  simulateGroupMessage,
  type PrivateCommandResult,
  type RouteResult,
  type SimulateGroupMessageRequest,
  type SimulatePrivateMessageRequest,
} from '@/api/simulate'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const activeTab = ref('group')
const loading = ref(false)
const privateLoading = ref(false)
const result = ref<RouteResult | null>(null)
const privateResult = ref<PrivateCommandResult | null>(null)
const lastRequest = ref<SimulateGroupMessageRequest | null>(null)
const privateLastRequest = ref<SimulatePrivateMessageRequest | null>(null)

const form = reactive<SimulateGroupMessageRequest>({
  groupId: '',
  userId: '',
  messageId: newMessageId(),
  rawMessage: '',
  atBot: false,
  botNicknameMatched: false,
})
const privateForm = reactive({
  targetGroupId: '',
  userId: '',
  messageId: newPrivateMessageId(),
  rawMessage: '',
})

const presets = [
  {
    type: 'meme',
    title: '表情包 A',
    description: '普通群消息，验证关键词/语义场景是否能命中表情包。',
    rawMessage: '哈哈',
    atBot: false,
    botNicknameMatched: false,
  },
  {
    type: 'passive',
    title: '被动聊天 B',
    description: '模拟 @ 机器人，验证 passive-chat-reply 工作流。',
    rawMessage: '介绍一下你自己',
    atBot: true,
    botNicknameMatched: false,
  },
  {
    type: 'nickname',
    title: '昵称触发 B',
    description: '模拟没有 @，但命中机器人昵称的被动聊天入口。',
    rawMessage: '式部茉优 你怎么看这波操作',
    atBot: false,
    botNicknameMatched: true,
  },
  {
    type: 'active',
    title: '主动插话 C',
    description: '模拟普通群聊候选消息，验证主动插话策略和冷却。',
    rawMessage: '这个操作也太经典了',
    atBot: false,
    botNicknameMatched: false,
  },
  {
    type: 'status',
    title: '管理员状态',
    description: '模拟 #状态 指令，是否成功取决于 userId 是否在管理员白名单。',
    rawMessage: '#状态',
    atBot: false,
    botNicknameMatched: false,
  },
  {
    type: 'miss',
    title: '未命中静默',
    description: '普通消息不触发 B，A/C 未命中时应静默。',
    rawMessage: '今天路过一下',
    atBot: false,
    botNicknameMatched: false,
  },
]

const resultJson = computed(() => (result.value ? JSON.stringify(result.value, null, 2) : ''))
const requestJson = computed(() => (lastRequest.value ? JSON.stringify(lastRequest.value, null, 2) : ''))
const privateResultJson = computed(() =>
  privateResult.value ? JSON.stringify(privateResult.value, null, 2) : '',
)
const privateRequestJson = computed(() =>
  privateLastRequest.value ? JSON.stringify(privateLastRequest.value, null, 2) : '',
)

const privatePresets = [
  { title: '状态', command: '状态', description: '查看目标群 group_config 摘要，不写 admin_op_log。' },
  { title: '开启表情包', command: '开启表情包', description: '打开 A 表情包通路。' },
  { title: '关闭表情包', command: '关闭表情包', description: '关闭 A 表情包通路。' },
  { title: '开启知识库', command: '开启知识库', description: '打开知识库总开关。' },
  { title: '关闭知识库', command: '关闭知识库', description: '关闭知识库总开关。' },
  { title: '冷却 300', command: '冷却 300', description: '把主动插话冷却调整为 300 秒。' },
]

const routeSummary = computed(() => {
  const value = result.value
  if (!value) return []
  return [
    { label: '去重', active: value.dedupPassed, activeText: 'PASS', inactiveText: 'DUP' },
    { label: '发送', active: value.shouldSend, activeText: 'YES', inactiveText: 'NO' },
    { label: '管理员指令', active: value.adminCommandHit, activeText: 'HIT', inactiveText: 'MISS' },
    { label: '表情包 A', active: value.memeHit, activeText: 'HIT', inactiveText: 'MISS' },
    { label: '被动聊天 B', active: value.passiveChatHit, activeText: 'HIT', inactiveText: 'MISS' },
    { label: '主动插话 C', active: Boolean(value.activeChatHit), activeText: 'HIT', inactiveText: 'MISS' },
  ]
})

function newMessageId() {
  return `admin-ui-${Date.now()}`
}

function newPrivateMessageId() {
  return `admin-ui-private-${Date.now()}`
}

function refreshMessageId() {
  if (activeTab.value === 'private') {
    privateForm.messageId = newPrivateMessageId()
  } else {
    form.messageId = newMessageId()
  }
}

function applyPreset(preset: (typeof presets)[number]) {
  refreshMessageId()
  form.rawMessage = preset.rawMessage
  form.atBot = preset.atBot
  form.botNicknameMatched = preset.botNicknameMatched
}

function applyPrivatePreset(preset: (typeof privatePresets)[number]) {
  refreshMessageId()
  const groupId = privateForm.targetGroupId.trim() || form.groupId.trim() || '251288204'
  privateForm.targetGroupId = groupId
  privateForm.rawMessage = `#群 ${groupId} ${preset.command}`
}

async function submitSimulation() {
  if (!form.groupId.trim()) {
    ElMessage.warning('请先输入 groupId')
    return
  }
  if (!form.userId.trim()) {
    ElMessage.warning('请先输入 userId')
    return
  }
  if (!form.rawMessage.trim()) {
    ElMessage.warning('请先输入 rawMessage')
    return
  }
  loading.value = true
  try {
    const request: SimulateGroupMessageRequest = {
      groupId: form.groupId.trim(),
      userId: form.userId.trim(),
      messageId: form.messageId?.trim() || undefined,
      rawMessage: form.rawMessage.trim(),
      atBot: form.atBot,
      botNicknameMatched: form.botNicknameMatched,
    }
    lastRequest.value = request
    result.value = await simulateGroupMessage(request)
    ElMessage.success('模拟消息已路由')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模拟消息失败')
  } finally {
    loading.value = false
  }
}

async function submitPrivateSimulation() {
  if (!privateForm.userId.trim()) {
    ElMessage.warning('请先输入私聊 userId')
    return
  }
  if (!privateForm.rawMessage.trim()) {
    ElMessage.warning('请先输入私聊 rawMessage')
    return
  }
  privateLoading.value = true
  try {
    const request: SimulatePrivateMessageRequest = {
      userId: privateForm.userId.trim(),
      messageId: privateForm.messageId?.trim() || undefined,
      rawMessage: privateForm.rawMessage.trim(),
    }
    privateLastRequest.value = request
    privateResult.value = await simulatePrivateMessage(request)
    ElMessage.success(privateResult.value.handled ? '私聊控制命令已解析' : '私聊消息被忽略')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '私聊模拟失败')
  } finally {
    privateLoading.value = false
  }
}

function clearResult() {
  result.value = null
  lastRequest.value = null
  privateResult.value = null
  privateLastRequest.value = null
}

async function repeatLastMessageId() {
  if (activeTab.value === 'private') {
    if (!privateLastRequest.value) {
      ElMessage.warning('还没有上一次私聊请求可以重复')
      return
    }
    Object.assign(privateForm, privateLastRequest.value)
    await submitPrivateSimulation()
    return
  }
  if (!lastRequest.value) {
    ElMessage.warning('还没有上一次请求可以重复')
    return
  }
  Object.assign(form, lastRequest.value)
  await submitSimulation()
}

async function copyRequest() {
  const text = activeTab.value === 'private' ? privateRequestJson.value : requestJson.value
  if (!text) {
    ElMessage.warning('还没有请求快照')
    return
  }
  await copyText(text, '请求 JSON 已复制')
}

async function copyResult() {
  const text = activeTab.value === 'private' ? privateResultJson.value : resultJson.value
  if (!text) {
    ElMessage.warning('还没有路由结果')
    return
  }
  await copyText(text, activeTab.value === 'private' ? '私聊模拟结果已复制' : 'RouteResult JSON 已复制')
}

async function copyText(text: string, successMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.error('复制失败，可以手动选中内容')
  }
}

function display(value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  return String(value)
}
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="消息模拟"
      description="通过 dev/local 模拟入口构造群消息或私聊控制命令，观察路由、去重、管理员指令、静默原因和回复内容。"
    >
      <template #eyebrow>Route Lab</template>
      <template #actions>
        <el-button :icon="Refresh" @click="refreshMessageId">生成 messageId</el-button>
        <el-button
          :disabled="activeTab === 'private' ? !privateLastRequest : !lastRequest"
          @click="repeatLastMessageId"
        >
          重复上次 messageId
        </el-button>
        <el-button @click="clearResult">清空结果</el-button>
      </template>
    </PageHeader>

    <section class="route-lab-tabs">
      <el-tabs v-model="activeTab" class="knowledge-tabs">
        <el-tab-pane label="群消息路由" name="group">
          <section class="simulate-layout">
            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>模拟输入</h3>
                <span class="panel-subtitle">POST /dev/simulate/group-message</span>
              </div>
              <el-form class="simulate-form" :model="form" label-position="top">
                <div class="number-form-grid">
                  <el-form-item label="groupId">
                    <el-input v-model="form.groupId" placeholder="例如 251288204" clearable />
                  </el-form-item>
                  <el-form-item label="userId">
                    <el-input v-model="form.userId" placeholder="例如 20001" clearable />
                  </el-form-item>
                </div>
                <el-form-item label="messageId">
                  <el-input v-model="form.messageId" clearable />
                </el-form-item>
                <el-form-item label="rawMessage">
                  <el-input
                    v-model="form.rawMessage"
                    type="textarea"
                    :rows="5"
                    maxlength="500"
                    show-word-limit
                    placeholder="输入一条模拟群消息"
                  />
                </el-form-item>
                <div class="inline-checks">
                  <el-checkbox v-model="form.atBot">atBot</el-checkbox>
                  <el-checkbox v-model="form.botNicknameMatched">botNicknameMatched</el-checkbox>
                </div>
                <div class="simulation-preset-grid">
                  <button
                    v-for="preset in presets"
                    :key="preset.type"
                    class="simulation-preset-card"
                    type="button"
                    @click="applyPreset(preset)"
                  >
                    <span>{{ preset.title }}</span>
                    <small>{{ preset.description }}</small>
                  </button>
                </div>
                <div class="simulate-actions">
                  <el-button type="primary" :icon="Search" :loading="loading" @click="submitSimulation">
                    发送模拟消息
                  </el-button>
                </div>
              </el-form>
            </div>

            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>路由结果</h3>
                <div class="table-actions">
                  <el-button :icon="CopyDocument" :disabled="!result" @click="copyResult">复制结果</el-button>
                </div>
              </div>
              <template v-if="result">
                <div class="result-strip">
                  <span>routeType: {{ result.routeType }}</span>
                  <span>responseType: {{ result.responseType }}</span>
                  <span>durationMs: {{ display(result.durationMs) }}</span>
                </div>
                <div class="status-list route-status-list">
                  <StatusBadge
                    v-for="item in routeSummary"
                    :key="item.label"
                    :label="item.label"
                    :active="item.active"
                    :active-text="item.activeText"
                    :inactive-text="item.inactiveText"
                  />
                </div>
                <div class="settings-kv route-kv">
                  <span>reason</span>
                  <strong>{{ display(result.reason) }}</strong>
                  <span>silentReason</span>
                  <strong>{{ display(result.silentReason) }}</strong>
                  <span>workflowType</span>
                  <strong>{{ display(result.workflowType) }}</strong>
                  <span>sceneCode</span>
                  <strong>{{ display(result.sceneCode) }}</strong>
                  <span>confidence</span>
                  <strong>{{ display(result.confidence) }}</strong>
                  <span>memeId</span>
                  <strong>{{ display(result.memeId) }}</strong>
                  <span>chatConfidence</span>
                  <strong>{{ display(result.chatConfidence) }}</strong>
                  <span>replyText</span>
                  <strong>{{ display(result.replyText) }}</strong>
                  <span>activeShouldReply</span>
                  <strong>{{ display(result.activeShouldReply) }}</strong>
                  <span>activeConfidence</span>
                  <strong>{{ display(result.activeConfidence) }}</strong>
                  <span>activeReason</span>
                  <strong>{{ display(result.activeReason) }}</strong>
                  <span>activePolicyPassed</span>
                  <strong>{{ display(result.activePolicyPassed) }}</strong>
                  <span>activePolicyRejectReason</span>
                  <strong>{{ display(result.activePolicyRejectReason) }}</strong>
                  <span>cooldownSeconds</span>
                  <strong>{{ display(result.cooldownSeconds) }}</strong>
                  <span>hourCount</span>
                  <strong>{{ display(result.hourCount) }}</strong>
                  <span>randomHit</span>
                  <strong>{{ display(result.randomHit) }}</strong>
                </div>
              </template>
              <div v-else class="empty-block">
                填写左侧参数并发送后，这里会显示 RouteResult。
              </div>
            </div>
          </section>

          <section v-if="result" class="panel-grid two">
            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>出站消息</h3>
                <span class="panel-subtitle">MockMessageSender 下不会真的发 QQ</span>
              </div>
              <div v-if="result.outboundMessage" class="outbound-preview">
                <span>text</span>
                <p>{{ display(result.outboundMessage.text) }}</p>
                <span>imagePath</span>
                <p>{{ display(result.outboundMessage.imagePath) }}</p>
              </div>
              <div v-else class="empty-block compact">
                本次路由没有出站消息。
              </div>
            </div>

            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>请求快照</h3>
                <div class="table-actions">
                  <el-button :icon="CopyDocument" @click="copyRequest">复制请求</el-button>
                </div>
              </div>
              <pre class="json-preview">{{ requestJson }}</pre>
            </div>
          </section>

          <section v-if="result" class="panel nested-panel">
            <div class="panel-title-row">
              <h3>RouteResult JSON</h3>
              <span class="panel-subtitle">完整调试字段</span>
            </div>
            <pre class="json-preview">{{ resultJson }}</pre>
          </section>
        </el-tab-pane>

        <el-tab-pane label="私聊控制" name="private">
          <section class="simulate-layout">
            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>私聊输入</h3>
                <span class="panel-subtitle">POST /dev/simulate/private-message</span>
              </div>
              <el-form class="simulate-form" :model="privateForm" label-position="top">
                <div class="number-form-grid">
                  <el-form-item label="管理员 userId">
                    <el-input v-model="privateForm.userId" placeholder="必须在 QQBOT_ADMINS 中" clearable />
                  </el-form-item>
                  <el-form-item label="目标群号">
                    <el-input v-model="privateForm.targetGroupId" placeholder="用于生成预设命令" clearable />
                  </el-form-item>
                </div>
                <el-form-item label="messageId">
                  <el-input v-model="privateForm.messageId" clearable />
                </el-form-item>
                <el-form-item label="rawMessage">
                  <el-input
                    v-model="privateForm.rawMessage"
                    type="textarea"
                    :rows="5"
                    maxlength="1000"
                    show-word-limit
                    placeholder="#群 251288204 状态"
                  />
                </el-form-item>
                <div class="simulation-preset-grid">
                  <button
                    v-for="preset in privatePresets"
                    :key="preset.title"
                    class="simulation-preset-card"
                    type="button"
                    @click="applyPrivatePreset(preset)"
                  >
                    <span>{{ preset.title }}</span>
                    <small>{{ preset.description }}</small>
                  </button>
                </div>
                <div class="simulate-actions">
                  <el-button
                    type="primary"
                    :icon="Search"
                    :loading="privateLoading"
                    @click="submitPrivateSimulation"
                  >
                    发送私聊模拟
                  </el-button>
                </div>
              </el-form>
            </div>

            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>私聊控制结果</h3>
                <div class="table-actions">
                  <el-button :icon="CopyDocument" :disabled="!privateResult" @click="copyResult">复制结果</el-button>
                </div>
              </div>
              <template v-if="privateResult">
                <div class="status-list route-status-list">
                  <StatusBadge
                    label="命令处理"
                    :active="privateResult.handled"
                    active-text="HANDLED"
                    inactive-text="IGNORED"
                  />
                  <StatusBadge
                    label="私聊回复"
                    :active="privateResult.shouldReply"
                    active-text="YES"
                    inactive-text="NO"
                  />
                </div>
                <div class="settings-kv route-kv">
                  <span>userId</span>
                  <strong>{{ display(privateResult.userId) }}</strong>
                  <span>messageId</span>
                  <strong>{{ display(privateResult.messageId) }}</strong>
                  <span>operation</span>
                  <strong>{{ display(privateResult.operation) }}</strong>
                  <span>detail</span>
                  <strong>{{ display(privateResult.detail) }}</strong>
                  <span>replyText</span>
                  <strong>{{ display(privateResult.outboundMessage?.text) }}</strong>
                </div>
                <div class="empty-block compact" v-if="!privateResult.handled">
                  当前私聊在真实 OneBot 链路中会被静默忽略，通常表示不是命令或发送者不在管理员白名单。
                </div>
              </template>
              <div v-else class="empty-block">
                填写左侧参数并发送后，这里会显示私聊命令解析结果。
              </div>
            </div>
          </section>

          <section v-if="privateResult" class="panel-grid two">
            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>请求快照</h3>
                <div class="table-actions">
                  <el-button :icon="CopyDocument" @click="copyRequest">复制请求</el-button>
                </div>
              </div>
              <pre class="json-preview">{{ privateRequestJson }}</pre>
            </div>
            <div class="panel nested-panel">
              <div class="panel-title-row">
                <h3>PrivateCommandResult JSON</h3>
                <span class="panel-subtitle">不会真的发送私聊消息</span>
              </div>
              <pre class="json-preview">{{ privateResultJson }}</pre>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>
