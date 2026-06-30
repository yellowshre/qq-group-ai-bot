<script setup lang="ts">
import { CopyDocument, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'

import {
  generateCandidates,
  generateEmbeddings,
  importChatHistory,
  listImportBatches,
  listKnowledgeEmbeddings,
  listKnowledgeCandidates,
  listKnowledgePublishLogs,
  listMemberCandidates,
  previewRouteKnowledge,
  publishKnowledge,
  publishMemberProfiles,
  simulateDifyContext,
  type ChatHistoryImportResponse,
  type ChatImportBatchSummary,
  type DifyContextSimulateResponse,
  type EmbeddingGenerateResponse,
  type GenerateCandidatesResponse,
  type KnowledgeCandidate,
  type KnowledgeEmbeddingRecord,
  type KnowledgePublishLog,
  type KnowledgeRoutePreviewResponse,
  type MemberCandidate,
  type PublishResponse,
} from '@/api/knowledge'
import { getAdminOverview, type AdminOverviewResponse } from '@/api/overview'
import PageHeader from '@/components/common/PageHeader.vue'
import {
  readLastGroupId,
  readLastOperator,
  rememberLastGroupId,
  rememberLastOperator,
} from '@/composables/useAdminPreferences'

const loading = ref(false)
const acting = ref(false)
const overview = ref<AdminOverviewResponse | null>(null)
const importBatches = ref<ChatImportBatchSummary[]>([])
const approvedKnowledge = ref<KnowledgeCandidate[]>([])
const approvedMembers = ref<MemberCandidate[]>([])
const publishLogs = ref<KnowledgePublishLog[]>([])
const embeddingRecords = ref<KnowledgeEmbeddingRecord[]>([])
const importResult = ref<ChatHistoryImportResponse | null>(null)
const generateResult = ref<GenerateCandidatesResponse | null>(null)
const publishResult = ref<PublishResponse | null>(null)
const embeddingResult = ref<EmbeddingGenerateResponse | null>(null)
const routePreview = ref<KnowledgeRoutePreviewResponse | null>(null)
const difyPreview = ref<DifyContextSimulateResponse | null>(null)

const form = ref({
  groupId: readLastGroupId(),
  filePath: 'data/chat-export/group_251288204_sample_20260628_185926.json',
  batchId: '',
  operator: readLastOperator(),
  comment: '',
  batchStatus: '',
  batchLimit: 20,
  messageText: '这个操作也太经典了',
  senderUid: '',
  routeType: 'MEME',
  topK: 5,
  targetTypes: ['GROUP_KNOWLEDGE', 'MEMBER_PROFILE'],
  regenerateEmbedding: false,
  recordLimit: 50,
  publishAction: '',
  publishTargetType: '',
  embeddingStatus: '',
  embeddingTargetType: '',
})

watch(() => form.value.groupId, rememberLastGroupId)
watch(() => form.value.operator, rememberLastOperator)

const routeOptions = [
  { label: '表情包 A', value: 'MEME' },
  { label: '被动聊天 B', value: 'PASSIVE_CHAT' },
  { label: '主动插话 C', value: 'ACTIVE_CHAT' },
]

const targetTypeOptions = [
  { label: '正式知识', value: 'GROUP_KNOWLEDGE' },
  { label: '成员画像', value: 'MEMBER_PROFILE' },
]

const publishActionOptions = [
  { label: '全部动作', value: '' },
  { label: '发布', value: 'PUBLISH' },
  { label: '启用', value: 'ENABLE' },
  { label: '停用', value: 'DISABLE' },
]

const embeddingStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
]

const batchStatusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '运行中', value: 'RUNNING' },
]

const approvedKnowledgeIds = computed(() => approvedKnowledge.value.map((item) => item.id))
const approvedMemberIds = computed(() => approvedMembers.value.map((item) => item.id))

const pipelineSteps = computed(() => [
  {
    label: '导入',
    value: overview.value?.latestImport?.status || importResult.value?.status || '-',
    hint: overview.value?.latestImport
      ? `batch ${overview.value.latestImport.batchId}, clean ${overview.value.latestImport.cleanCount}`
      : '把 NapCat-QCE JSON 导入 chat_* 表',
  },
  {
    label: '候选',
    value: `${overview.value?.knowledgeCandidates ?? 0} / ${overview.value?.memberCandidates ?? 0}`,
    hint: `待审 ${overview.value?.pendingKnowledgeCandidates ?? 0} 条群梗，${overview.value?.pendingMemberCandidates ?? 0} 条成员画像`,
  },
  {
    label: '已审批',
    value: `${approvedKnowledge.value.length} / ${approvedMembers.value.length}`,
    hint: '本页会发布当前筛选条件下 APPROVED 的候选',
  },
  {
    label: '正式库',
    value: `${overview.value?.enabledGroupKnowledge ?? 0} / ${overview.value?.enabledMemberProfiles ?? 0}`,
    hint: `ACTIVE ${overview.value?.activeGroupKnowledge ?? 0} 条知识，${overview.value?.activeMemberProfiles ?? 0} 条画像`,
  },
  {
    label: 'Embedding',
    value: `${overview.value?.successfulEmbeddings ?? 0}`,
    hint: embeddingResult.value
      ? `刚刚成功 ${embeddingResult.value.embedded}, 跳过 ${embeddingResult.value.skipped}, 失败 ${embeddingResult.value.failed}`
      : '正式知识发布后再生成向量',
  },
  {
    label: '召回验证',
    value: routePreview.value ? 'READY' : '-',
    hint: routePreview.value
      ? `${routePreview.value.routes.filter((item) => item.knowledgeUsed).length} 条链路使用知识`
      : '预览 A/B/C 实际是否带 knowledgeContext',
  },
])

const latestImportLines = computed(() => {
  const latest = overview.value?.latestImport
  if (!latest) return ['暂无导入批次']
  return [
    `batchId: ${latest.batchId}`,
    `status: ${latest.status}`,
    `raw/clean: ${latest.rawCount}/${latest.cleanCount}`,
    `sessions/members: ${latest.sessionCount}/${latest.memberCount}`,
    `source: ${latest.sourceFile}`,
    `createdAt: ${latest.createdAt}`,
  ]
})

async function refreshPipeline() {
  const groupId = form.value.groupId.trim()
  loading.value = true
  try {
    const [overviewData, batches, knowledge, members, logs, embeddings] = await Promise.all([
      getAdminOverview(groupId || undefined),
      listImportBatches(groupId || null, form.value.batchStatus || null, form.value.batchLimit),
      listKnowledgeCandidates({
        groupId: groupId || null,
        batchId: form.value.batchId.trim() || null,
        status: 'APPROVED',
      }),
      listMemberCandidates({
        groupId: groupId || null,
        batchId: form.value.batchId.trim() || null,
        status: 'APPROVED',
      }),
      listKnowledgePublishLogs({
        groupId: groupId || null,
        targetType: form.value.publishTargetType || null,
        action: form.value.publishAction || null,
        limit: form.value.recordLimit,
      }),
      listKnowledgeEmbeddings({
        groupId: groupId || null,
        targetType: form.value.embeddingTargetType || null,
        status: form.value.embeddingStatus || null,
        limit: form.value.recordLimit,
      }),
    ])
    overview.value = overviewData
    importBatches.value = batches
    approvedKnowledge.value = knowledge
    approvedMembers.value = members
    publishLogs.value = logs
    embeddingRecords.value = embeddings
    if (!form.value.batchId.trim() && overviewData.latestImport?.batchId) {
      form.value.batchId = `${overviewData.latestImport.batchId}`
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '流水线状态加载失败')
  } finally {
    loading.value = false
  }
}

async function importHistoryAction() {
  const groupId = requireGroupId()
  const filePath = form.value.filePath.trim()
  if (!groupId || !filePath) {
    ElMessage.warning('导入需要 groupId 和 JSON 文件路径')
    return
  }
  await runAction(async () => {
    importResult.value = await importChatHistory(groupId, filePath)
    form.value.batchId = `${importResult.value.batchId}`
    ElMessage.success(`导入完成：batchId=${importResult.value.batchId}`)
    await refreshPipeline()
  })
}

async function generateCandidatesAction() {
  const groupId = requireGroupId()
  const batchId = parseBatchId()
  if (!groupId || !batchId) return
  await runAction(async () => {
    generateResult.value = await generateCandidates(batchId, groupId)
    ElMessage.success(
      `候选生成完成：群梗 ${generateResult.value.knowledgeCandidates}，成员画像 ${generateResult.value.memberCandidates}`,
    )
    await refreshPipeline()
  })
}

async function publishApprovedKnowledgeAction() {
  const groupId = requireGroupId()
  if (!groupId) return
  if (approvedKnowledgeIds.value.length === 0) {
    ElMessage.warning('当前没有 APPROVED 群梗候选可发布')
    return
  }
  await runAction(async () => {
    publishResult.value = await publishKnowledge(
      groupId,
      approvedKnowledgeIds.value,
      operator(),
      comment(),
    )
    ElMessage.success(`群梗发布完成：${publishResult.value.published} 条，跳过 ${publishResult.value.skipped} 条`)
    await refreshPipeline()
  })
}

async function publishApprovedMembersAction() {
  const groupId = requireGroupId()
  if (!groupId) return
  if (approvedMemberIds.value.length === 0) {
    ElMessage.warning('当前没有 APPROVED 成员画像候选可发布')
    return
  }
  await runAction(async () => {
    publishResult.value = await publishMemberProfiles(
      groupId,
      approvedMemberIds.value,
      operator(),
      comment(),
    )
    ElMessage.success(`成员画像发布完成：${publishResult.value.published} 条，跳过 ${publishResult.value.skipped} 条`)
    await refreshPipeline()
  })
}

async function generateEmbeddingAction() {
  const groupId = requireGroupId()
  if (!groupId) return
  await runAction(async () => {
    embeddingResult.value = await generateEmbeddings(
      groupId,
      form.value.targetTypes,
      form.value.regenerateEmbedding,
    )
    ElMessage.success(
      `Embedding 完成：成功 ${embeddingResult.value.embedded}，跳过 ${embeddingResult.value.skipped}，失败 ${embeddingResult.value.failed}`,
    )
    await refreshPipeline()
  })
}

async function previewRoutesAction() {
  const groupId = requireGroupId()
  const messageText = form.value.messageText.trim()
  if (!groupId || !messageText) {
    ElMessage.warning('召回预览需要 groupId 和消息文本')
    return
  }
  await runAction(async () => {
    routePreview.value = await previewRouteKnowledge(
      groupId,
      messageText,
      form.value.topK,
      form.value.senderUid.trim() || undefined,
    )
  })
}

async function simulateDifyInputsAction() {
  const groupId = requireGroupId()
  const messageText = form.value.messageText.trim()
  if (!groupId || !messageText) {
    ElMessage.warning('Dify inputs 预览需要 groupId 和消息文本')
    return
  }
  await runAction(async () => {
    difyPreview.value = await simulateDifyContext(
      groupId,
      messageText,
      form.value.routeType,
      form.value.topK,
      form.value.senderUid.trim() || undefined,
    )
  })
}

async function copyAcceptance() {
  const lines = [
    `groupId: ${form.value.groupId.trim() || '-'}`,
    ...pipelineSteps.value.map((item) => `${item.label}: ${item.value} (${item.hint})`),
    `approvedKnowledgeIds: ${approvedKnowledgeIds.value.join(',') || '-'}`,
    `approvedMemberIds: ${approvedMemberIds.value.join(',') || '-'}`,
  ]
  await copyText(lines.join('\n'), '流水线验收摘要已复制')
}

async function copyText(text: string, successMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.error('复制失败，可以手动选中内容')
  }
}

async function runAction(action: () => Promise<void>) {
  acting.value = true
  try {
    await action()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    acting.value = false
  }
}

function requireGroupId() {
  const groupId = form.value.groupId.trim()
  if (!groupId) {
    ElMessage.warning('请先输入 groupId')
    return null
  }
  return groupId
}

function parseBatchId() {
  const parsed = Number(form.value.batchId)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    ElMessage.warning('请填写有效 batchId')
    return null
  }
  return parsed
}

function operator() {
  return form.value.operator.trim() || 'local-admin'
}

function comment() {
  return form.value.comment.trim() || undefined
}

function formatNumber(value?: number | null) {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

function setBatch(batch: ChatImportBatchSummary) {
  form.value.groupId = batch.groupId
  form.value.batchId = `${batch.batchId}`
  if (batch.sourceFile) {
    form.value.filePath = batch.sourceFile
  }
}

function statusType(status?: string | null) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PUBLISH' || status === 'ENABLE') return 'success'
  if (status === 'DISABLE') return 'warning'
  return 'warning'
}

function shortText(value?: string | null, max = 120) {
  if (!value) return '-'
  return value.length > max ? `${value.slice(0, max)}...` : value
}

function jsonText(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2)
}

onMounted(refreshPipeline)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="聊天数据流水线"
      description="把导入、候选生成、审批后的发布、Embedding 和 knowledgeContext 验证集中到一个可回滚的本地操作面板。"
    >
      <template #eyebrow>Chat Pipeline</template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="refreshPipeline">刷新状态</el-button>
        <el-button :icon="CopyDocument" @click="copyAcceptance">复制验收摘要</el-button>
      </template>
    </PageHeader>

    <section class="workflow-strip">
      <span>导入 JSON</span>
      <span>清洗统计</span>
      <span>生成候选</span>
      <span>人工审批</span>
      <span>发布正式库</span>
      <span>生成 Embedding</span>
      <span>召回验证</span>
    </section>

    <section class="pipeline-grid">
      <div v-for="step in pipelineSteps" :key="step.label" class="pipeline-step">
        <span>{{ step.label }}</span>
        <strong>{{ step.value }}</strong>
        <small>{{ step.hint }}</small>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>基础参数</h3>
          <span class="panel-subtitle">所有操作都只走 dev/local 管理接口，不会直接触发真实群消息。</span>
        </div>
      </div>
      <el-form class="knowledge-filter" :model="form" label-position="top">
        <el-form-item label="群号">
          <el-input v-model="form.groupId" placeholder="例如 251288204" clearable />
        </el-form-item>
        <el-form-item label="批次 ID">
          <el-input v-model="form.batchId" placeholder="导入后自动填入，也可手动填写" clearable />
        </el-form-item>
        <el-form-item label="导入 JSON 文件">
          <el-input
            v-model="form.filePath"
            placeholder="data/chat-export/group_251288204_sample_20260628_185926.json"
            clearable
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="form.operator" placeholder="local-admin" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.comment" placeholder="可选，会进入发布/审核相关日志" clearable />
        </el-form-item>
        <el-form-item label="批次状态">
          <el-select v-model="form.batchStatus">
            <el-option
              v-for="item in batchStatusOptions"
              :key="item.value || 'ALL'"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次数量">
          <el-input-number v-model="form.batchLimit" :min="1" :max="100" />
        </el-form-item>
        <el-form-item class="rank-submit">
          <el-button :icon="Search" type="primary" :loading="loading" @click="refreshPipeline">查询</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>导入批次历史</h3>
          <span class="panel-subtitle">只读展示 chat_import_batch 摘要，不展示聊天正文和 sourceHash。</span>
        </div>
      </div>
      <el-table v-if="importBatches.length" :data="importBatches" class="rank-table">
        <el-table-column prop="batchId" label="Batch" width="86" />
        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="groupId" label="群号" width="128" />
        <el-table-column label="消息" width="132">
          <template #default="{ row }">
            {{ formatNumber(row.rawCount) }} / {{ formatNumber(row.cleanCount) }}
          </template>
        </el-table-column>
        <el-table-column label="结构" width="132">
          <template #default="{ row }">
            {{ formatNumber(row.sessionCount) }} 会话 / {{ formatNumber(row.memberCount) }} 人
          </template>
        </el-table-column>
        <el-table-column prop="sourceFile" label="文件" min-width="260" />
        <el-table-column prop="createdAt" label="创建时间" width="172" />
        <el-table-column label="错误" min-width="220">
          <template #default="{ row }">
            <span class="table-snippet">{{ row.errorMessage || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" plain @click="setBatch(row)">使用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-block compact">暂无导入批次。导入 JSON 成功或失败后都会显示在这里。</div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>1. 导入与候选生成</h3>
            <span class="panel-subtitle">文件名建议使用 ASCII，路径限定在 data/chat-export/ 下。</span>
          </div>
        </div>
        <div class="table-actions">
          <el-button type="primary" :loading="acting" @click="importHistoryAction">导入 JSON</el-button>
          <el-button :loading="acting" @click="generateCandidatesAction">生成候选</el-button>
        </div>
        <div v-if="importResult" class="result-strip import-result">
          <span>batch {{ importResult.batchId }}</span>
          <span>raw {{ importResult.rawMessages }}</span>
          <span>clean {{ importResult.cleanMessages }}</span>
          <span>mentions {{ importResult.mentions }}</span>
          <span>sessions {{ importResult.sessions }}</span>
          <span>{{ importResult.duplicateImport ? 'duplicate import' : importResult.status }}</span>
        </div>
        <div v-if="generateResult" class="result-strip import-result">
          <span>knowledge {{ generateResult.knowledgeCandidates }}</span>
          <span>members {{ generateResult.memberCandidates }}</span>
          <span>{{ generateResult.status }}</span>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>最近导入批次</h3>
            <span class="panel-subtitle">来自 /dev/admin/overview，不展示聊天正文。</span>
          </div>
        </div>
        <div class="code-lines">
          <code v-for="line in latestImportLines" :key="line">{{ line }}</code>
        </div>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>2. 发布已审批候选</h3>
            <span class="panel-subtitle">本页只发布 APPROVED 候选；PENDING / REJECTED 不会进入正式库。</span>
          </div>
        </div>
        <div class="pipeline-mini-grid">
          <div class="pipeline-mini-card">
            <span>已审批群梗</span>
            <strong>{{ approvedKnowledge.length }}</strong>
            <small>发布到 chat_group_knowledge</small>
          </div>
          <div class="pipeline-mini-card">
            <span>已审批画像</span>
            <strong>{{ approvedMembers.length }}</strong>
            <small>发布到 chat_member_profile</small>
          </div>
        </div>
        <div class="table-actions">
          <el-button
            type="primary"
            :disabled="approvedKnowledgeIds.length === 0"
            :loading="acting"
            @click="publishApprovedKnowledgeAction"
          >
            发布群梗知识
          </el-button>
          <el-button
            :disabled="approvedMemberIds.length === 0"
            :loading="acting"
            @click="publishApprovedMembersAction"
          >
            发布成员画像
          </el-button>
        </div>
        <div v-if="publishResult" class="result-strip import-result">
          <span>published {{ publishResult.published }}</span>
          <span>skipped {{ publishResult.skipped }}</span>
          <span>{{ publishResult.status }}</span>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>3. Embedding</h3>
            <span class="panel-subtitle">正式知识发布后再生成向量；Ollama 不可用时后端会记录失败。</span>
          </div>
        </div>
        <el-checkbox-group v-model="form.targetTypes" class="inline-checks">
          <el-checkbox
            v-for="item in targetTypeOptions"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
        <el-checkbox v-model="form.regenerateEmbedding">重新生成已有 embedding</el-checkbox>
        <div class="table-actions">
          <el-button type="primary" :loading="acting" @click="generateEmbeddingAction">生成 Embedding</el-button>
        </div>
        <div v-if="embeddingResult" class="result-strip import-result">
          <span>embedded {{ embeddingResult.embedded }}</span>
          <span>skipped {{ embeddingResult.skipped }}</span>
          <span>failed {{ embeddingResult.failed }}</span>
          <span>{{ embeddingResult.status }}</span>
        </div>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>发布操作记录</h3>
            <span class="panel-subtitle">来自 chat_knowledge_publish_log，只展示动作和目标，不展示知识全文。</span>
          </div>
        </div>
        <el-form label-position="top">
          <div class="number-form-grid">
            <el-form-item label="目标类型">
              <el-select v-model="form.publishTargetType" clearable>
                <el-option
                  v-for="item in targetTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="动作">
              <el-select v-model="form.publishAction" clearable>
                <el-option
                  v-for="item in publishActionOptions"
                  :key="item.value || 'ALL'"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="form.recordLimit" :min="1" :max="200" />
            </el-form-item>
          </div>
        </el-form>
        <div class="table-actions">
          <el-button :icon="Search" :loading="loading" @click="refreshPipeline">刷新记录</el-button>
        </div>
        <el-table v-if="publishLogs.length" :data="publishLogs" class="rank-table">
          <el-table-column prop="id" label="ID" width="78" />
          <el-table-column prop="createdAt" label="时间" width="172" />
          <el-table-column label="动作" width="92">
            <template #default="{ row }">
              <el-tag :type="statusType(row.action)" effect="plain">{{ row.action || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetType" label="目标类型" width="150" />
          <el-table-column prop="targetId" label="目标 ID" width="100" />
          <el-table-column prop="operator" label="操作人" width="128" />
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">{{ shortText(row.comment, 100) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-block compact">暂无发布操作记录。</div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>Embedding 明细</h3>
            <span class="panel-subtitle">来自 chat_knowledge_embedding，不返回 vector 和 embeddingText。</span>
          </div>
        </div>
        <el-form label-position="top">
          <div class="number-form-grid">
            <el-form-item label="目标类型">
              <el-select v-model="form.embeddingTargetType" clearable>
                <el-option
                  v-for="item in targetTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="form.embeddingStatus" clearable>
                <el-option
                  v-for="item in embeddingStatusOptions"
                  :key="item.value || 'ALL'"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-input-number v-model="form.recordLimit" :min="1" :max="200" />
            </el-form-item>
          </div>
        </el-form>
        <div class="table-actions">
          <el-button :icon="Search" :loading="loading" @click="refreshPipeline">刷新记录</el-button>
        </div>
        <el-table v-if="embeddingRecords.length" :data="embeddingRecords" class="rank-table">
          <el-table-column prop="id" label="ID" width="78" />
          <el-table-column label="状态" width="92">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="plain">{{ row.status || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetType" label="目标类型" width="150" />
          <el-table-column prop="targetId" label="目标 ID" width="100" />
          <el-table-column prop="embeddingModel" label="模型" min-width="150" />
          <el-table-column prop="embeddingDim" label="维度" width="80" />
          <el-table-column label="错误" min-width="200">
            <template #default="{ row }">{{ shortText(row.errorMessage, 120) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-block compact">暂无 embedding 记录。</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>4. 召回与 Dify 输入验证</h3>
          <span class="panel-subtitle">验证 knowledgeContext 是否短、准、可控；不直接修改 MessageRouterService 开关。</span>
        </div>
      </div>
      <div class="context-preview-grid">
        <section class="tool-box">
          <el-form label-position="top">
            <el-form-item label="模拟消息">
              <el-input
                v-model="form.messageText"
                type="textarea"
                :autosize="{ minRows: 4, maxRows: 8 }"
                placeholder="输入一条群消息，用于预览 A/B/C 知识召回"
              />
            </el-form-item>
            <div class="number-form-grid">
              <el-form-item label="发送者 UID">
                <el-input v-model="form.senderUid" placeholder="可选" clearable />
              </el-form-item>
              <el-form-item label="Dify 链路">
                <el-select v-model="form.routeType">
                  <el-option
                    v-for="item in routeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="Top K">
                <el-input-number v-model="form.topK" :min="1" :max="10" />
              </el-form-item>
            </div>
          </el-form>
          <div class="table-actions">
            <el-button type="primary" :loading="acting" @click="previewRoutesAction">A/B/C 灰度预览</el-button>
            <el-button :loading="acting" @click="simulateDifyInputsAction">Dify Inputs</el-button>
          </div>
        </section>

        <section class="tool-box">
          <h4>当前汇总</h4>
          <div class="settings-kv">
            <span>raw / clean</span>
            <strong>{{ formatNumber(overview?.rawMessages) }} / {{ formatNumber(overview?.cleanMessages) }}</strong>
            <span>sessions / members</span>
            <strong>{{ formatNumber(overview?.sessions) }} / {{ formatNumber(overview?.memberStats) }}</strong>
            <span>pending candidates</span>
            <strong>
              {{ formatNumber(overview?.pendingKnowledgeCandidates) }} /
              {{ formatNumber(overview?.pendingMemberCandidates) }}
            </strong>
            <span>successful embeddings</span>
            <strong>{{ formatNumber(overview?.successfulEmbeddings) }}</strong>
          </div>
        </section>
      </div>

      <el-table v-if="routePreview?.routes.length" :data="routePreview.routes" class="rank-table">
        <el-table-column prop="routeType" label="链路" width="140" />
        <el-table-column label="开关" width="110">
          <template #default="{ row }">
            <el-tag :type="row.routeKnowledgeEnabled ? 'success' : 'info'" effect="plain">
              {{ row.routeKnowledgeEnabled ? 'enabled' : 'disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用知识" width="120">
          <template #default="{ row }">{{ row.knowledgeUsed ? 'yes' : 'no' }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="条数" width="90" />
        <el-table-column label="最高分" width="100">
          <template #default="{ row }">{{ row.maxScore.toFixed(3) }}</template>
        </el-table-column>
        <el-table-column prop="silentReason" label="原因" min-width="180" />
      </el-table>

      <section v-if="difyPreview" class="tool-box import-result">
        <h4>Dify inputs 模拟</h4>
        <pre class="json-preview">{{ jsonText(difyPreview.inputs) }}</pre>
      </section>
    </section>
  </div>
</template>
