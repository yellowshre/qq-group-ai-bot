<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import {
  simulateGroupMessage,
  type RouteResult,
  type SimulateGroupMessageRequest,
} from '@/api/simulate'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const loading = ref(false)
const result = ref<RouteResult | null>(null)
const lastRequest = ref<SimulateGroupMessageRequest | null>(null)

const form = reactive<SimulateGroupMessageRequest>({
  groupId: '',
  userId: '',
  messageId: newMessageId(),
  rawMessage: '',
  atBot: false,
  botNicknameMatched: false,
})

const resultJson = computed(() => (result.value ? JSON.stringify(result.value, null, 2) : ''))
const requestJson = computed(() => (lastRequest.value ? JSON.stringify(lastRequest.value, null, 2) : ''))

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

function refreshMessageId() {
  form.messageId = newMessageId()
}

function applyPreset(type: 'meme' | 'passive' | 'active' | 'miss') {
  refreshMessageId()
  form.atBot = false
  form.botNicknameMatched = false
  if (type === 'meme') {
    form.rawMessage = '哈哈'
  } else if (type === 'passive') {
    form.rawMessage = '介绍一下你自己'
    form.atBot = true
  } else if (type === 'active') {
    form.rawMessage = '这个操作也太经典了'
  } else {
    form.rawMessage = '今天路过一下'
  }
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

function clearResult() {
  result.value = null
  lastRequest.value = null
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
      description="通过 dev 模拟入口构造群消息，观察 A/B/C 路由、去重、Dify 分支、静默原因和发送内容。"
    >
      <template #eyebrow>Route Lab</template>
      <template #actions>
        <el-button @click="refreshMessageId">生成 messageId</el-button>
        <el-button @click="clearResult">清空结果</el-button>
      </template>
    </PageHeader>

    <section class="simulate-layout">
      <div class="panel">
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
          <div class="preset-row">
            <el-button @click="applyPreset('meme')">普通表情包</el-button>
            <el-button @click="applyPreset('passive')">被动聊天</el-button>
            <el-button @click="applyPreset('active')">主动插话候选</el-button>
            <el-button @click="applyPreset('miss')">未命中静默</el-button>
          </div>
          <div class="simulate-actions">
            <el-button type="primary" :loading="loading" @click="submitSimulation">
              发送模拟消息
            </el-button>
          </div>
        </el-form>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <h3>路由结果</h3>
          <span class="panel-subtitle">{{ result ? result.routeType : '尚未模拟' }}</span>
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
            <span>activeShouldReply</span>
            <strong>{{ display(result.activeShouldReply) }}</strong>
            <span>activeConfidence</span>
            <strong>{{ display(result.activeConfidence) }}</strong>
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
      <div class="panel">
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

      <div class="panel">
        <div class="panel-title-row">
          <h3>请求快照</h3>
          <span class="panel-subtitle">便于复现</span>
        </div>
        <pre class="json-preview">{{ requestJson }}</pre>
      </div>
    </section>

    <section v-if="result" class="panel">
      <div class="panel-title-row">
        <h3>RouteResult JSON</h3>
        <span class="panel-subtitle">完整调试字段</span>
      </div>
      <pre class="json-preview">{{ resultJson }}</pre>
    </section>
  </div>
</template>
