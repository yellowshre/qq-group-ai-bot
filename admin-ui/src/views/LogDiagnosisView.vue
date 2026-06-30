<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

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

const responseTypeOptions = ['SILENT', 'MEME_IMAGE', 'PASSIVE_CHAT', 'ACTIVE_CHAT', 'ADMIN_COMMAND']
const workflowOptions = ['MEME_KEYWORD', 'MEME_DIFY_SCENE', 'PASSIVE_CHAT', 'ACTIVE_CHAT']

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

function tagType(success?: boolean | null) {
  if (success === true) return 'success'
  if (success === false) return 'danger'
  return 'info'
}

function shortText(value?: string | null, empty = '-') {
  return value && value.trim() ? value : empty
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
        <el-button :icon="Refresh" :loading="loading" @click="loadCurrent">刷新当前页</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <el-tabs v-model="activeTab" class="knowledge-tabs" @tab-change="loadCurrent">
        <el-tab-pane label="触发日志" name="triggers">
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

          <el-table v-if="triggerLogs.length" :data="triggerLogs" class="rank-table">
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
          </el-table>
          <div v-else class="empty-block compact">暂无触发日志。</div>
        </el-tab-pane>

        <el-tab-pane label="管理员操作" name="admin-ops">
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

          <el-table v-if="adminOps.length" :data="adminOps" class="rank-table">
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
          </el-table>
          <div v-else class="empty-block compact">暂无管理员操作日志。</div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>
