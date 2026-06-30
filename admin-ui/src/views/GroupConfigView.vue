<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'
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
        <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
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
            :label="selectedConfigured ? '数据库记录' : '默认配置'"
            :active="selectedConfigured"
            active-text="已存在"
            inactive-text="保存后创建"
          />
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
