<script setup lang="ts">
import { CopyDocument, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import {
  listAdminOpLogs,
  listTriggerLogs,
  type AdminOpLogItem,
  type TriggerLogItem,
} from '@/api/logs'
import PageHeader from '@/components/common/PageHeader.vue'

const activeTab = ref('triggers')
const loading = ref(false)
const triggerLogs = ref<TriggerLogItem[]>([])
const adminOps = ref<AdminOpLogItem[]>([])
const triggerFilter = reactive({
  groupId: '',
  userId: '',
  messageId: '',
  responseType: '',
  workflowType: '',
  success: '',
  limit: 100,
})
const adminFilter = reactive({
  groupId: '',
  operatorUid: '',
  operation: '',
  limit: 100,
})

const responseTypeOptions = ['SILENT', 'COMMAND', 'MEME', 'PASSIVE_CHAT', 'ACTIVE_CHAT', 'SAFE_WORD']
const workflowOptions = ['MEME_KEYWORD', 'MEME_DIFY_SCENE', 'PASSIVE_DIFY_CHAT', 'ACTIVE_DIFY_CHAT']

const triggerStats = computed(() => {
  const total = triggerLogs.value.length
  const success = triggerLogs.value.filter((item) => item.success === true).length
  const failed = triggerLogs.value.filter((item) => item.success === false).length
  const silent = triggerLogs.value.filter((item) => item.responseType === 'SILENT').length
  const sent = triggerLogs.value.filter((item) => item.responseType !== 'SILENT').length
  const durations = triggerLogs.value
    .map((item) => item.durationMs)
    .filter((item): item is number => typeof item === 'number')
  const avgDuration = durations.length
    ? Math.round(durations.reduce((sum, item) => sum + item, 0) / durations.length)
    : null
  return [
    `已载入 ${total} 条`,
    `成功 ${success}`,
    `失败 ${failed}`,
    `静默 ${silent}`,
    `有出站 ${sent}`,
    `平均耗时 ${avgDuration ?? '-'} ms`,
  ]
})

const adminStats = computed(() => {
  const total = adminOps.value.length
  const groups = new Set(adminOps.value.map((item) => item.groupId).filter(Boolean)).size
  const operators = new Set(adminOps.value.map((item) => item.operatorUid).filter(Boolean)).size
  return [`已载入 ${total} 条`, `涉及群 ${groups}`, `操作人 ${operators}`]
})

const adminOperationShortcuts = computed(() =>
  Array.from(new Set(adminOps.value.map((item) => item.operation).filter(Boolean)))
    .sort()
    .slice(0, 8),
)

async function loadTriggers() {
  loading.value = true
  try {
    triggerLogs.value = await listTriggerLogs({
      ...triggerFilter,
      success: triggerFilter.success || null,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '触发日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadAdminOps() {
  loading.value = true
  try {
    adminOps.value = await listAdminOpLogs(adminFilter)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '管理员操作日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCurrent() {
  if (activeTab.value === 'admin-ops') {
    await loadAdminOps()
  } else {
    await loadTriggers()
  }
}

function resetTriggerFilter() {
  Object.assign(triggerFilter, {
    groupId: '',
    userId: '',
    messageId: '',
    responseType: '',
    workflowType: '',
    success: '',
    limit: 100,
  })
}

function resetAdminFilter() {
  Object.assign(adminFilter, {
    groupId: '',
    operatorUid: '',
    operation: '',
    limit: 100,
  })
}

async function applyTriggerShortcut(type: 'silent' | 'meme' | 'passive' | 'active' | 'failed') {
  if (type === 'silent') {
    triggerFilter.responseType = 'SILENT'
    triggerFilter.workflowType = ''
    triggerFilter.success = ''
  } else if (type === 'meme') {
    triggerFilter.responseType = 'MEME'
    triggerFilter.workflowType = ''
    triggerFilter.success = ''
  } else if (type === 'passive') {
    triggerFilter.responseType = 'PASSIVE_CHAT'
    triggerFilter.workflowType = 'PASSIVE_DIFY_CHAT'
    triggerFilter.success = ''
  } else if (type === 'active') {
    triggerFilter.responseType = 'ACTIVE_CHAT'
    triggerFilter.workflowType = 'ACTIVE_DIFY_CHAT'
    triggerFilter.success = ''
  } else if (type === 'failed') {
    triggerFilter.responseType = ''
    triggerFilter.workflowType = ''
    triggerFilter.success = 'false'
  }
  await loadTriggers()
}

async function applyAdminOperationShortcut(operation: string) {
  adminFilter.operation = operation
  await loadAdminOps()
}

async function copyCurrentFilter() {
  const lines = activeTab.value === 'admin-ops'
    ? [
        'admin_op_log filter',
        `groupId=${display(adminFilter.groupId)}`,
        `operatorUid=${display(adminFilter.operatorUid)}`,
        `operation=${display(adminFilter.operation)}`,
        `limit=${display(adminFilter.limit)}`,
      ]
    : [
        'trigger_log filter',
        `groupId=${display(triggerFilter.groupId)}`,
        `userId=${display(triggerFilter.userId)}`,
        `messageId=${display(triggerFilter.messageId)}`,
        `responseType=${display(triggerFilter.responseType)}`,
        `workflowType=${display(triggerFilter.workflowType)}`,
        `success=${display(triggerFilter.success)}`,
        `limit=${display(triggerFilter.limit)}`,
      ]
  await copyText(lines.join('\n'), '当前筛选条件已复制')
}

async function copyTrigger(row: TriggerLogItem) {
  await copyText(
    [
      `trigger_log #${row.id}`,
      `time=${shortText(row.createdAt)}`,
      `groupId=${row.groupId}`,
      `userId=${row.userId}`,
      `messageId=${shortText(row.messageId)}`,
      `responseType=${row.responseType}`,
      `workflowType=${shortText(row.workflowType)}`,
      `success=${display(row.success)}`,
      `durationMs=${display(row.durationMs)}`,
      `memeId=${display(row.memeId)}`,
      `error=${shortText(row.errorMsg)}`,
    ].join('\n'),
    '触发日志摘要已复制',
  )
}

async function copyAdminOp(row: AdminOpLogItem) {
  await copyText(
    [
      `admin_op_log #${row.id}`,
      `time=${shortText(row.createdAt)}`,
      `groupId=${row.groupId}`,
      `operatorUid=${row.operatorUid}`,
      `operation=${row.operation}`,
      `detail=${shortText(row.detail)}`,
    ].join('\n'),
    '管理员操作摘要已复制',
  )
}

async function copyText(text: string, successMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.error('复制失败，可以手动选中表格内容')
  }
}

function tagType(success?: boolean | null) {
  if (success === true) return 'success'
  if (success === false) return 'danger'
  return 'info'
}

function shortText(value?: string | null, empty = '-') {
  return value && value.trim() ? value : empty
}

function display(value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'boolean') return value ? 'true' : 'false'
  return String(value)
}

onMounted(async () => {
  await Promise.all([loadTriggers(), loadAdminOps()])
})
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="运行日志"
      description="查看 trigger_log 与 admin_op_log，定位真实群路由、静默原因和管理员操作。"
    >
      <template #eyebrow>Logs</template>
      <template #actions>
        <el-button :icon="CopyDocument" @click="copyCurrentFilter">复制筛选</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadCurrent">刷新当前页</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <el-tabs v-model="activeTab" class="knowledge-tabs" @tab-change="loadCurrent">
        <el-tab-pane label="触发日志" name="triggers">
          <div class="log-shortcuts">
            <el-button size="small" @click="applyTriggerShortcut('silent')">只看静默</el-button>
            <el-button size="small" @click="applyTriggerShortcut('meme')">表情包 A</el-button>
            <el-button size="small" @click="applyTriggerShortcut('passive')">被动聊天 B</el-button>
            <el-button size="small" @click="applyTriggerShortcut('active')">主动插话 C</el-button>
            <el-button size="small" @click="applyTriggerShortcut('failed')">只看失败</el-button>
            <el-button size="small" @click="resetTriggerFilter">重置筛选</el-button>
          </div>

          <el-form class="log-filter" :model="triggerFilter" label-position="top">
            <el-form-item label="群号">
              <el-input v-model="triggerFilter.groupId" clearable />
            </el-form-item>
            <el-form-item label="用户">
              <el-input v-model="triggerFilter.userId" clearable />
            </el-form-item>
            <el-form-item label="messageId">
              <el-input v-model="triggerFilter.messageId" clearable />
            </el-form-item>
            <el-form-item label="responseType">
              <el-select v-model="triggerFilter.responseType" clearable>
                <el-option
                  v-for="item in responseTypeOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="workflow">
              <el-select v-model="triggerFilter.workflowType" clearable>
                <el-option
                  v-for="item in workflowOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="成功">
              <el-select v-model="triggerFilter.success" clearable>
                <el-option label="成功" value="true" />
                <el-option label="失败" value="false" />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="triggerFilter.limit" :min="1" :max="500" />
            </el-form-item>
            <el-form-item class="rank-submit">
              <el-button :icon="Search" type="primary" :loading="loading" @click="loadTriggers">查询</el-button>
            </el-form-item>
          </el-form>

          <div class="result-strip log-result-strip">
            <span v-for="item in triggerStats" :key="item">{{ item }}</span>
          </div>

          <el-table v-if="triggerLogs.length" :data="triggerLogs" class="rank-table">
            <el-table-column type="expand" width="42">
              <template #default="{ row }">
                <div class="log-detail-grid">
                  <span>messageId</span>
                  <strong>{{ shortText(row.messageId) }}</strong>
                  <span>originalMsg</span>
                  <strong>{{ shortText(row.originalMsg) }}</strong>
                  <span>responseText</span>
                  <strong>{{ shortText(row.responseText, '无回复文本') }}</strong>
                  <span>errorMsg</span>
                  <strong>{{ shortText(row.errorMsg) }}</strong>
                  <span>token / cost</span>
                  <strong>{{ display(row.tokenUsed) }} / {{ display(row.cost) }}</strong>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="78" />
            <el-table-column prop="createdAt" label="时间" width="172" />
            <el-table-column prop="groupId" label="群" width="118" />
            <el-table-column prop="userId" label="用户" width="118" />
            <el-table-column label="类型" width="160">
              <template #default="{ row }">
                <el-tag :type="tagType(row.success)" effect="plain">
                  {{ row.responseType }}
                </el-tag>
                <div class="table-snippet">{{ row.workflowType || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="消息 / 回复" min-width="320">
              <template #default="{ row }">
                <div class="table-title">{{ shortText(row.originalMsg) }}</div>
                <div class="table-snippet">{{ shortText(row.responseText, '无回复文本') }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="memeId" label="meme" width="86" />
            <el-table-column prop="durationMs" label="耗时ms" width="96" />
            <el-table-column label="错误" min-width="220">
              <template #default="{ row }">
                <div class="table-snippet">{{ shortText(row.errorMsg) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="92" fixed="right">
              <template #default="{ row }">
                <el-button :icon="CopyDocument" link type="primary" @click="copyTrigger(row)">复制</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block compact">暂无触发日志。</div>
        </el-tab-pane>

        <el-tab-pane label="管理员操作" name="admin-ops">
          <div class="log-shortcuts">
            <el-button
              v-for="operation in adminOperationShortcuts"
              :key="operation"
              size="small"
              @click="applyAdminOperationShortcut(operation)"
            >
              {{ operation }}
            </el-button>
            <el-button size="small" @click="resetAdminFilter">重置筛选</el-button>
          </div>

          <el-form class="log-filter" :model="adminFilter" label-position="top">
            <el-form-item label="群号">
              <el-input v-model="adminFilter.groupId" clearable />
            </el-form-item>
            <el-form-item label="操作人">
              <el-input v-model="adminFilter.operatorUid" clearable />
            </el-form-item>
            <el-form-item label="操作">
              <el-input v-model="adminFilter.operation" clearable />
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="adminFilter.limit" :min="1" :max="500" />
            </el-form-item>
            <el-form-item class="rank-submit">
              <el-button :icon="Search" type="primary" :loading="loading" @click="loadAdminOps">查询</el-button>
            </el-form-item>
          </el-form>

          <div class="result-strip log-result-strip">
            <span v-for="item in adminStats" :key="item">{{ item }}</span>
          </div>

          <el-table v-if="adminOps.length" :data="adminOps" class="rank-table">
            <el-table-column type="expand" width="42">
              <template #default="{ row }">
                <div class="log-detail-grid">
                  <span>operation</span>
                  <strong>{{ shortText(row.operation) }}</strong>
                  <span>detail</span>
                  <strong>{{ shortText(row.detail) }}</strong>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="78" />
            <el-table-column prop="createdAt" label="时间" width="172" />
            <el-table-column prop="groupId" label="群" width="118" />
            <el-table-column prop="operatorUid" label="操作人" width="128" />
            <el-table-column prop="operation" label="操作" width="180" />
            <el-table-column label="详情" min-width="320">
              <template #default="{ row }">
                <div class="table-snippet">{{ shortText(row.detail) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="92" fixed="right">
              <template #default="{ row }">
                <el-button :icon="CopyDocument" link type="primary" @click="copyAdminOp(row)">复制</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block compact">暂无管理员操作日志。</div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>
