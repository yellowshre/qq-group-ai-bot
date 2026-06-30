<script setup lang="ts">
import { CopyDocument, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  getGroupConfig,
  listGroupConfigs,
  updateGroupConfig,
  type GroupConfigListResponse,
  type GroupConfigUpdateRequest,
} from '@/api/groupConfig'
import type { GroupConfigSnapshot } from '@/api/health'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

const loading = ref(false)
const saving = ref(false)
const inputGroupId = ref('')
const selectedGroupId = ref('')
const groupList = ref<GroupConfigListResponse>({
  allowedGroupIds: [],
  configuredGroups: [],
})
const baseline = ref<GroupConfigUpdateRequest | null>(null)

const form = reactive<GroupConfigUpdateRequest>({
  botOn: true,
  enableChat: true,
  enableMeme: true,
  enablePassiveChat: true,
  enableAutoJoin: false,
  activeCooldownSeconds: 180,
  activeMaxPerHour: 20,
  activeMaxPerDay: 80,
  safeWord: '',
  safeWordReply: '',
  persona: '',
  memoryMode: 'SHORT',
  enableKnowledgeContext: false,
  enableMemeKnowledge: false,
  enablePassiveChatKnowledge: false,
  enableActiveChatKnowledge: false,
})

const configuredGroupIds = computed(() => groupList.value.configuredGroups.map((item) => item.groupId))
const allGroupIds = computed(() => {
  const merged = new Set<string>()
  for (const groupId of groupList.value.allowedGroupIds) merged.add(groupId)
  for (const groupId of configuredGroupIds.value) merged.add(groupId)
  return Array.from(merged)
})

const selectedConfigured = computed(() => configuredGroupIds.value.includes(selectedGroupId.value))
const selectedSourceText = computed(() => (selectedConfigured.value ? '数据库记录' : '默认配置'))
const statusSummary = computed(() => [
  `总开关 ${displayValue(form.botOn)}`,
  `A ${displayValue(form.enableMeme)}`,
  `B ${displayValue(form.enablePassiveChat)}`,
  `C ${displayValue(form.enableAutoJoin)}`,
  `知识 ${displayValue(form.enableKnowledgeContext)}`,
  `冷却 ${form.activeCooldownSeconds}s`,
])
const diffItems = computed(() => {
  if (!baseline.value) return []
  const current = normalizeRequest()
  return diffFields
    .map((field) => ({
      ...field,
      before: displayValue(baseline.value?.[field.key]),
      after: displayValue(current[field.key]),
      changed: !sameValue(baseline.value?.[field.key], current[field.key]),
    }))
    .filter((item) => item.changed)
})
const isDirty = computed(() => diffItems.value.length > 0)

const diffFields: Array<{ key: keyof GroupConfigUpdateRequest; label: string; group: string }> = [
  { key: 'botOn', label: '机器人总开关', group: '主链路' },
  { key: 'enableChat', label: '聊天总开关', group: '主链路' },
  { key: 'enableMeme', label: '表情包 A', group: '主链路' },
  { key: 'enablePassiveChat', label: '被动聊天 B', group: '主链路' },
  { key: 'enableAutoJoin', label: '主动插话 C', group: '主链路' },
  { key: 'enableKnowledgeContext', label: '知识库总开关', group: '知识库' },
  { key: 'enableMemeKnowledge', label: '表情包知识', group: '知识库' },
  { key: 'enablePassiveChatKnowledge', label: '聊天知识', group: '知识库' },
  { key: 'enableActiveChatKnowledge', label: '主动知识', group: '知识库' },
  { key: 'activeCooldownSeconds', label: '冷却时间', group: '主动风控' },
  { key: 'activeMaxPerHour', label: '每小时上限', group: '主动风控' },
  { key: 'activeMaxPerDay', label: '每日上限', group: '主动风控' },
  { key: 'memoryMode', label: '记忆模式', group: '记忆' },
  { key: 'safeWord', label: '安全词', group: '安全词' },
  { key: 'safeWordReply', label: '安全词回复', group: '安全词' },
  { key: 'persona', label: '群级人设', group: '人设' },
]

async function loadList() {
  loading.value = true
  try {
    groupList.value = await listGroupConfigs()
    if (!selectedGroupId.value && allGroupIds.value.length > 0) {
      await selectGroup(allGroupIds.value[0])
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '群配置列表加载失败')
  } finally {
    loading.value = false
  }
}

async function selectGroup(groupId: string) {
  if (!groupId) return
  selectedGroupId.value = groupId
  inputGroupId.value = groupId
  loading.value = true
  try {
    const config = await getGroupConfig(groupId)
    applySnapshot(config)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '群配置加载失败')
  } finally {
    loading.value = false
  }
}

async function loadManualGroup() {
  const groupId = inputGroupId.value.trim()
  if (!/^\d+$/.test(groupId)) {
    ElMessage.warning('请输入数字群号')
    return
  }
  await selectGroup(groupId)
}

async function save() {
  if (!selectedGroupId.value) {
    ElMessage.warning('请先选择或输入群号')
    return
  }
  if (!isDirty.value) {
    ElMessage.info('当前没有需要保存的变更')
    return
  }
  saving.value = true
  try {
    const saved = await updateGroupConfig(selectedGroupId.value, normalizeRequest())
    applySnapshot(saved)
    await loadList()
    selectedGroupId.value = saved.groupId
    inputGroupId.value = saved.groupId
    ElMessage.success('群配置已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function applyPreset(type: 'quiet' | 'memeOnly' | 'memeAndPassive' | 'memeKnowledge' | 'activeConservative') {
  if (type === 'quiet') {
    form.botOn = false
    form.enableAutoJoin = false
  } else if (type === 'memeOnly') {
    form.botOn = true
    form.enableChat = true
    form.enableMeme = true
    form.enablePassiveChat = false
    form.enableAutoJoin = false
  } else if (type === 'memeAndPassive') {
    form.botOn = true
    form.enableChat = true
    form.enableMeme = true
    form.enablePassiveChat = true
    form.enableAutoJoin = false
  } else if (type === 'memeKnowledge') {
    form.enableKnowledgeContext = true
    form.enableMemeKnowledge = true
    form.enablePassiveChatKnowledge = false
    form.enableActiveChatKnowledge = false
  } else if (type === 'activeConservative') {
    form.botOn = true
    form.enableChat = true
    form.enableAutoJoin = true
    form.activeCooldownSeconds = 300
    form.activeMaxPerHour = 3
    form.activeMaxPerDay = 10
  }
}

function clearPersona() {
  form.persona = ''
}

async function copyStatus() {
  if (!selectedGroupId.value) {
    ElMessage.warning('请先选择群')
    return
  }
  const text = [
    `群 ${selectedGroupId.value} 当前配置`,
    `机器人总开关：${displayValue(form.botOn)}`,
    `聊天总开关：${displayValue(form.enableChat)}`,
    `表情包 A：${displayValue(form.enableMeme)}`,
    `被动聊天 B：${displayValue(form.enablePassiveChat)}`,
    `主动插话 C：${displayValue(form.enableAutoJoin)}`,
    `知识库总开关：${displayValue(form.enableKnowledgeContext)}`,
    `表情包知识：${displayValue(form.enableMemeKnowledge)}`,
    `聊天知识：${displayValue(form.enablePassiveChatKnowledge)}`,
    `主动知识：${displayValue(form.enableActiveChatKnowledge)}`,
    `冷却：${form.activeCooldownSeconds} 秒`,
    `小时上限：${form.activeMaxPerHour}`,
    `每日上限：${form.activeMaxPerDay}`,
    `记忆模式：${displayValue(form.memoryMode)}`,
    `人设：${form.persona?.trim() ? '已配置' : '默认'}`,
  ].join('\n')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('群配置摘要已复制')
  } catch {
    ElMessage.error('复制失败，可以手动查看页面配置')
  }
}

function applySnapshot(config: GroupConfigSnapshot) {
  selectedGroupId.value = config.groupId
  inputGroupId.value = config.groupId
  form.botOn = config.botOn
  form.enableChat = config.enableChat
  form.enableMeme = config.enableMeme
  form.enablePassiveChat = config.enablePassiveChat
  form.enableAutoJoin = config.enableAutoJoin
  form.activeCooldownSeconds = config.activeCooldownSeconds
  form.activeMaxPerHour = config.activeMaxPerHour
  form.activeMaxPerDay = config.activeMaxPerDay
  form.safeWord = config.safeWord ?? ''
  form.safeWordReply = config.safeWordReply ?? ''
  form.persona = config.persona ?? ''
  form.memoryMode = config.memoryMode ?? 'SHORT'
  form.enableKnowledgeContext = config.enableKnowledgeContext
  form.enableMemeKnowledge = config.enableMemeKnowledge
  form.enablePassiveChatKnowledge = config.enablePassiveChatKnowledge
  form.enableActiveChatKnowledge = config.enableActiveChatKnowledge
  baseline.value = snapshotToRequest(config)
}

function normalizeRequest(): GroupConfigUpdateRequest {
  return {
    ...form,
    activeCooldownSeconds: Math.max(1, Number(form.activeCooldownSeconds || 1)),
    activeMaxPerHour: Math.max(0, Number(form.activeMaxPerHour || 0)),
    activeMaxPerDay: Math.max(0, Number(form.activeMaxPerDay || 0)),
    safeWord: form.safeWord?.trim() || null,
    safeWordReply: form.safeWordReply?.trim() || null,
    persona: form.persona?.trim() || null,
  }
}

function snapshotToRequest(config: GroupConfigSnapshot): GroupConfigUpdateRequest {
  return {
    botOn: config.botOn,
    enableChat: config.enableChat,
    enableMeme: config.enableMeme,
    enablePassiveChat: config.enablePassiveChat,
    enableAutoJoin: config.enableAutoJoin,
    activeCooldownSeconds: config.activeCooldownSeconds,
    activeMaxPerHour: config.activeMaxPerHour,
    activeMaxPerDay: config.activeMaxPerDay,
    safeWord: config.safeWord?.trim() || null,
    safeWordReply: config.safeWordReply?.trim() || null,
    persona: config.persona?.trim() || null,
    memoryMode: config.memoryMode ?? 'SHORT',
    enableKnowledgeContext: config.enableKnowledgeContext,
    enableMemeKnowledge: config.enableMemeKnowledge,
    enablePassiveChatKnowledge: config.enablePassiveChatKnowledge,
    enableActiveChatKnowledge: config.enableActiveChatKnowledge,
  }
}

function sameValue(left: unknown, right: unknown) {
  return String(left ?? '') === String(right ?? '')
}

function displayValue(value: unknown) {
  if (typeof value === 'boolean') return value ? '开' : '关'
  if (value === null || value === undefined || value === '') return '空'
  return String(value)
}

onMounted(loadList)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="群配置控制台"
      description="集中维护 group_config：总开关、A/B/C、知识库灰度、人设、安全词和主动插话风控。"
    >
      <template #eyebrow>Group Ops</template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadList">刷新</el-button>
        <el-button :icon="CopyDocument" :disabled="!selectedGroupId" @click="copyStatus">复制状态</el-button>
        <el-button type="primary" :disabled="!isDirty" :loading="saving" @click="save">保存配置</el-button>
      </template>
    </PageHeader>

    <section class="group-config-layout">
      <aside class="panel group-picker">
        <div class="panel-title-row">
          <h3>群列表</h3>
          <span class="panel-subtitle">allowed + configured</span>
        </div>
        <div class="manual-group-row">
          <el-input
            v-model="inputGroupId"
            placeholder="输入群号"
            clearable
            @keyup.enter="loadManualGroup"
          />
          <el-button @click="loadManualGroup">载入</el-button>
        </div>
        <div v-if="allGroupIds.length" class="group-list">
          <button
            v-for="groupId in allGroupIds"
            :key="groupId"
            class="group-list-item"
            :class="{ active: groupId === selectedGroupId }"
            type="button"
            @click="selectGroup(groupId)"
          >
            <span>{{ groupId }}</span>
            <small>{{ configuredGroupIds.includes(groupId) ? 'configured' : 'allowed only' }}</small>
          </button>
        </div>
        <div v-else class="empty-block compact">
          暂无群配置。可以手动输入群号载入默认配置。
        </div>
      </aside>

      <section class="panel config-editor">
        <div class="panel-title-row">
          <h3>{{ selectedGroupId ? `群 ${selectedGroupId}` : '未选择群' }}</h3>
          <StatusBadge
            :label="selectedSourceText"
            :active="selectedConfigured"
            active-text="已存在"
            inactive-text="保存后创建"
          />
        </div>

        <div class="result-strip group-config-summary">
          <span v-for="item in statusSummary" :key="item">{{ item }}</span>
        </div>

        <div class="group-preset-row">
          <el-button size="small" @click="applyPreset('quiet')">安静模式</el-button>
          <el-button size="small" @click="applyPreset('memeOnly')">只开表情包 A</el-button>
          <el-button size="small" @click="applyPreset('memeAndPassive')">开启 A+B</el-button>
          <el-button size="small" @click="applyPreset('memeKnowledge')">灰度表情包知识</el-button>
          <el-button size="small" @click="applyPreset('activeConservative')">主动插话保守档</el-button>
        </div>

        <div class="change-preview">
          <div class="change-preview-head">
            <div>
              <h4>保存前变更预览</h4>
              <p>{{ isDirty ? `检测到 ${diffItems.length} 项变更` : '当前配置没有未保存变更' }}</p>
            </div>
            <el-tag :type="isDirty ? 'warning' : 'success'" effect="plain">
              {{ isDirty ? '待保存' : '干净' }}
            </el-tag>
          </div>
          <div v-if="diffItems.length" class="change-list">
            <div v-for="item in diffItems" :key="item.key" class="change-item">
              <span>{{ item.group }} / {{ item.label }}</span>
              <strong>
                <em>{{ item.before }}</em>
                <b>→</b>
                <em>{{ item.after }}</em>
              </strong>
            </div>
          </div>
        </div>

        <el-form label-position="top" class="config-form">
          <div class="form-section">
            <h4>主链路开关</h4>
            <div class="switch-form-grid">
              <el-form-item label="机器人总开关">
                <el-switch v-model="form.botOn" />
              </el-form-item>
              <el-form-item label="聊天总开关">
                <el-switch v-model="form.enableChat" />
              </el-form-item>
              <el-form-item label="表情包 A">
                <el-switch v-model="form.enableMeme" />
              </el-form-item>
              <el-form-item label="被动聊天 B">
                <el-switch v-model="form.enablePassiveChat" />
              </el-form-item>
              <el-form-item label="主动插话 C">
                <el-switch v-model="form.enableAutoJoin" />
              </el-form-item>
            </div>
          </div>

          <div class="form-section">
            <h4>知识库灰度</h4>
            <div class="switch-form-grid">
              <el-form-item label="知识库总开关">
                <el-switch v-model="form.enableKnowledgeContext" />
              </el-form-item>
              <el-form-item label="表情包知识">
                <el-switch v-model="form.enableMemeKnowledge" />
              </el-form-item>
              <el-form-item label="聊天知识">
                <el-switch v-model="form.enablePassiveChatKnowledge" />
              </el-form-item>
              <el-form-item label="主动知识">
                <el-switch v-model="form.enableActiveChatKnowledge" />
              </el-form-item>
            </div>
          </div>

          <div class="form-section">
            <h4>主动插话风控</h4>
            <div class="number-form-grid">
              <el-form-item label="冷却时间（秒）">
                <el-input-number v-model="form.activeCooldownSeconds" :min="1" :step="30" />
              </el-form-item>
              <el-form-item label="每小时上限">
                <el-input-number v-model="form.activeMaxPerHour" :min="0" />
              </el-form-item>
              <el-form-item label="每日上限">
                <el-input-number v-model="form.activeMaxPerDay" :min="0" />
              </el-form-item>
              <el-form-item label="记忆模式">
                <el-select v-model="form.memoryMode">
                  <el-option label="短期" value="SHORT" />
                  <el-option label="长期" value="LONG" />
                </el-select>
              </el-form-item>
            </div>
          </div>

          <div class="form-section">
            <h4>人设与安全词</h4>
            <el-form-item label="群级人设">
              <el-input
                v-model="form.persona"
                type="textarea"
                :autosize="{ minRows: 4, maxRows: 8 }"
                placeholder="留空则使用默认人设"
              />
            </el-form-item>
            <div class="persona-tools">
              <span>{{ form.persona?.length || 0 }} 字，留空会回退默认人设</span>
              <el-button size="small" @click="clearPersona">清空人设</el-button>
            </div>
            <div class="number-form-grid">
              <el-form-item label="安全词">
                <el-input v-model="form.safeWord" placeholder="留空关闭" clearable />
              </el-form-item>
              <el-form-item label="安全词回复">
                <el-input v-model="form.safeWordReply" placeholder="触发安全词时回复" clearable />
              </el-form-item>
            </div>
          </div>
        </el-form>
      </section>
    </section>
  </div>
</template>
