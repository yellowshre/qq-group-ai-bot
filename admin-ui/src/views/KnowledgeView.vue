<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import {
  generateCandidates,
  generateEmbeddings,
  importChatHistory,
  listFormalKnowledge,
  listKnowledgeCandidates,
  listMemberCandidates,
  listMemberProfiles,
  previewKnowledgeContext,
  previewRouteKnowledge,
  publishKnowledge,
  publishMemberProfiles,
  reviewKnowledgeCandidate,
  reviewMemberCandidate,
  searchKnowledge,
  setKnowledgeEnabled,
  setMemberProfileEnabled,
  simulateDifyContext,
  type CandidateStatus,
  type ChatHistoryImportResponse,
  type DifyContextSimulateResponse,
  type EmbeddingGenerateResponse,
  type GenerateCandidatesResponse,
  type GroupKnowledge,
  type KnowledgeContextPreviewResponse,
  type KnowledgeCandidate,
  type KnowledgeRoutePreviewResponse,
  type KnowledgeSearchResponse,
  type MemberCandidate,
  type MemberProfile,
  type PublishResponse,
} from '@/api/knowledge'
import PageHeader from '@/components/common/PageHeader.vue'

const activeTab = ref('knowledge-candidates')
const loading = ref(false)
const acting = ref(false)
const filter = ref({
  groupId: '',
  batchId: '',
  filePath: '',
  status: 'PENDING',
  reviewer: 'local-admin',
  comment: '',
})

const knowledgeCandidates = ref<KnowledgeCandidate[]>([])
const memberCandidates = ref<MemberCandidate[]>([])
const formalKnowledge = ref<GroupKnowledge[]>([])
const memberProfiles = ref<MemberProfile[]>([])
const selectedKnowledge = ref<KnowledgeCandidate[]>([])
const selectedMembers = ref<MemberCandidate[]>([])
const importResult = ref<ChatHistoryImportResponse | null>(null)
const generateResult = ref<GenerateCandidatesResponse | null>(null)
const publishResult = ref<PublishResponse | null>(null)
const embeddingResult = ref<EmbeddingGenerateResponse | null>(null)
const searchResult = ref<KnowledgeSearchResponse | null>(null)
const contextPreview = ref<KnowledgeContextPreviewResponse | null>(null)
const routePreview = ref<KnowledgeRoutePreviewResponse | null>(null)
const difyPreview = ref<DifyContextSimulateResponse | null>(null)
const contextForm = ref({
  messageText: '',
  senderUid: '',
  routeType: 'MEME',
  topK: 5,
  targetTypes: ['GROUP_KNOWLEDGE', 'MEMBER_PROFILE'],
  regenerate: false,
})

const statusOptions = [
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已停用', value: 'DISABLED' },
  { label: '全部', value: '' },
]
const routeOptions = [
  { label: '表情包 A', value: 'MEME' },
  { label: '被动聊天 B', value: 'PASSIVE_CHAT' },
  { label: '主动插话 C', value: 'ACTIVE_CHAT' },
]
const targetTypeOptions = [
  { label: '正式知识', value: 'GROUP_KNOWLEDGE' },
  { label: '成员画像', value: 'MEMBER_PROFILE' },
]

const approvedKnowledgeIds = computed(() =>
  selectedKnowledge.value.filter((item) => item.status === 'APPROVED').map((item) => item.id),
)
const approvedMemberIds = computed(() =>
  selectedMembers.value.filter((item) => item.status === 'APPROVED').map((item) => item.id),
)

async function loadAll() {
  loading.value = true
  try {
    const query = {
      groupId: filter.value.groupId.trim() || null,
      batchId: filter.value.batchId.trim() || null,
      status: filter.value.status || null,
    }
    const groupId = filter.value.groupId.trim() || undefined
    const [knowledge, members, formal, profiles] = await Promise.all([
      listKnowledgeCandidates(query),
      listMemberCandidates(query),
      listFormalKnowledge(groupId),
      listMemberProfiles(groupId),
    ])
    knowledgeCandidates.value = knowledge
    memberCandidates.value = members
    formalKnowledge.value = formal
    memberProfiles.value = profiles
    selectedKnowledge.value = []
    selectedMembers.value = []
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '知识库数据加载失败')
  } finally {
    loading.value = false
  }
}

async function generate() {
  const groupId = filter.value.groupId.trim()
  const batchId = Number(filter.value.batchId)
  if (!groupId || !Number.isInteger(batchId) || batchId <= 0) {
    ElMessage.warning('生成候选需要填写 groupId 和有效 batchId')
    return
  }
  acting.value = true
  try {
    generateResult.value = await generateCandidates(batchId, groupId)
    ElMessage.success(
      `候选生成完成：群梗 ${generateResult.value.knowledgeCandidates}，成员画像 ${generateResult.value.memberCandidates}`,
    )
    await loadAll()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '候选生成失败')
  } finally {
    acting.value = false
  }
}

async function importHistory() {
  const groupId = requireGroupId()
  const filePath = filter.value.filePath.trim()
  if (!groupId || !filePath) {
    ElMessage.warning('导入需要填写 groupId 和 JSON 文件路径')
    return
  }
  await runAction(async () => {
    importResult.value = await importChatHistory(groupId, filePath)
    filter.value.batchId = `${importResult.value.batchId}`
    ElMessage.success(
      `导入完成：batchId=${importResult.value.batchId}，clean=${importResult.value.cleanMessages}`,
    )
    await loadAll()
  })
}

async function reviewKnowledge(item: KnowledgeCandidate, status: CandidateStatus) {
  await runAction(async () => {
    await reviewKnowledgeCandidate(item.id, status, reviewer(), comment())
    ElMessage.success(status === 'APPROVED' ? '候选知识已通过' : '候选知识已拒绝')
    await loadAll()
  })
}

async function reviewMember(item: MemberCandidate, status: CandidateStatus) {
  await runAction(async () => {
    await reviewMemberCandidate(item.id, status, reviewer(), comment())
    ElMessage.success(status === 'APPROVED' ? '成员候选已通过' : '成员候选已拒绝')
    await loadAll()
  })
}

async function publishSelectedKnowledge() {
  const groupId = filter.value.groupId.trim()
  if (!groupId || approvedKnowledgeIds.value.length === 0) {
    ElMessage.warning('请先输入 groupId，并选择已通过的候选知识')
    return
  }
  await runAction(async () => {
    publishResult.value = await publishKnowledge(groupId, approvedKnowledgeIds.value, reviewer(), comment())
    ElMessage.success(`发布完成：${publishResult.value.published} 条，跳过 ${publishResult.value.skipped} 条`)
    await loadAll()
  })
}

async function publishSelectedMembers() {
  const groupId = filter.value.groupId.trim()
  if (!groupId || approvedMemberIds.value.length === 0) {
    ElMessage.warning('请先输入 groupId，并选择已通过的成员候选')
    return
  }
  await runAction(async () => {
    publishResult.value = await publishMemberProfiles(groupId, approvedMemberIds.value, reviewer(), comment())
    ElMessage.success(`发布完成：${publishResult.value.published} 条，跳过 ${publishResult.value.skipped} 条`)
    await loadAll()
  })
}

async function toggleKnowledge(item: GroupKnowledge, enabled: boolean) {
  await runAction(async () => {
    await setKnowledgeEnabled(item.id, enabled, reviewer(), comment())
    ElMessage.success(enabled ? '正式知识已启用' : '正式知识已停用')
    await loadAll()
  })
}

async function toggleMemberProfile(item: MemberProfile, enabled: boolean) {
  await runAction(async () => {
    await setMemberProfileEnabled(item.id, enabled, reviewer(), comment())
    ElMessage.success(enabled ? '成员画像已启用' : '成员画像已停用')
    await loadAll()
  })
}

async function generateEmbeddingAction() {
  const groupId = requireGroupId()
  if (!groupId) return
  await runAction(async () => {
    embeddingResult.value = await generateEmbeddings(
      groupId,
      contextForm.value.targetTypes,
      contextForm.value.regenerate,
    )
    ElMessage.success(
      `Embedding 完成：成功 ${embeddingResult.value.embedded}，跳过 ${embeddingResult.value.skipped}，失败 ${embeddingResult.value.failed}`,
    )
  })
}

async function searchFormalKnowledge() {
  const groupId = requireGroupId()
  if (!groupId) return
  const query = contextForm.value.messageText.trim()
  if (!query) {
    ElMessage.warning('请输入搜索文本')
    return
  }
  await runAction(async () => {
    searchResult.value = await searchKnowledge(groupId, query, contextForm.value.topK, contextForm.value.targetTypes)
  })
}

async function previewContext() {
  const groupId = requireGroupId()
  if (!groupId || !requireMessageText()) return
  await runAction(async () => {
    contextPreview.value = await previewKnowledgeContext(
      groupId,
      contextForm.value.messageText.trim(),
      contextForm.value.routeType,
      contextForm.value.topK,
      contextForm.value.senderUid.trim() || undefined,
    )
  })
}

async function previewRoutes() {
  const groupId = requireGroupId()
  if (!groupId || !requireMessageText()) return
  await runAction(async () => {
    routePreview.value = await previewRouteKnowledge(
      groupId,
      contextForm.value.messageText.trim(),
      contextForm.value.topK,
      contextForm.value.senderUid.trim() || undefined,
    )
  })
}

async function simulateDifyInputs() {
  const groupId = requireGroupId()
  if (!groupId || !requireMessageText()) return
  await runAction(async () => {
    difyPreview.value = await simulateDifyContext(
      groupId,
      contextForm.value.messageText.trim(),
      contextForm.value.routeType,
      contextForm.value.topK,
      contextForm.value.senderUid.trim() || undefined,
    )
  })
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
  const groupId = filter.value.groupId.trim()
  if (!groupId) {
    ElMessage.warning('请先输入 groupId')
    return null
  }
  return groupId
}

function requireMessageText() {
  if (!contextForm.value.messageText.trim()) {
    ElMessage.warning('请输入消息文本')
    return false
  }
  return true
}

function reviewer() {
  return filter.value.reviewer.trim() || 'local-admin'
}

function comment() {
  return filter.value.comment.trim() || undefined
}

function shortText(value?: string | null, max = 120) {
  if (!value) return '-'
  return value.length > max ? `${value.slice(0, max)}...` : value
}

function statusType(status?: string | null) {
  if (status === 'APPROVED' || status === 'ACTIVE') return 'success'
  if (status === 'REJECTED' || status === 'DISABLED') return 'danger'
  return 'warning'
}

function jsonText(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2)
}

onMounted(loadAll)
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="知识库工作台"
      description="候选群梗、候选成员画像、人工审批和正式知识发布的本地运维入口。"
    >
      <template #eyebrow>Knowledge</template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
        <el-button type="primary" :loading="acting" @click="generate">生成候选</el-button>
      </template>
    </PageHeader>

    <section class="workflow-strip">
      <span>导入</span>
      <span>清洗</span>
      <span>候选</span>
      <span>审批</span>
      <span>发布</span>
      <span>Embedding</span>
      <span>召回预览</span>
    </section>

    <section class="panel">
      <el-form class="knowledge-filter" :model="filter" label-position="top">
        <el-form-item label="群号">
          <el-input v-model="filter.groupId" placeholder="例如 251288204" clearable />
        </el-form-item>
        <el-form-item label="批次 ID">
          <el-input v-model="filter.batchId" placeholder="生成候选时必填" clearable />
        </el-form-item>
        <el-form-item label="导入 JSON 文件">
          <el-input
            v-model="filter.filePath"
            placeholder="data/chat-export/group_251288204_sample.json"
            clearable
          />
        </el-form-item>
        <el-form-item label="候选状态">
          <el-select v-model="filter.status">
            <el-option
              v-for="item in statusOptions"
              :key="item.value || 'ALL'"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="filter.reviewer" placeholder="local-admin" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="filter.comment" placeholder="可选" clearable />
        </el-form-item>
        <el-form-item class="rank-submit">
          <div class="table-actions">
            <el-button :loading="acting" @click="importHistory">导入 JSON</el-button>
            <el-button :icon="Search" type="primary" :loading="loading" @click="loadAll">查询</el-button>
          </div>
        </el-form-item>
      </el-form>
      <div v-if="importResult" class="result-strip import-result">
        <span>batch {{ importResult.batchId }}</span>
        <span>raw {{ importResult.rawMessages }}</span>
        <span>clean {{ importResult.cleanMessages }}</span>
        <span>mentions {{ importResult.mentions }}</span>
        <span>sessions {{ importResult.sessions }}</span>
        <span>{{ importResult.duplicateImport ? 'duplicate import' : importResult.status }}</span>
      </div>
    </section>

    <section class="panel">
      <el-tabs v-model="activeTab" class="knowledge-tabs">
        <el-tab-pane label="候选群梗" name="knowledge-candidates">
          <div class="table-toolbar">
            <span class="panel-subtitle">
              已选 {{ selectedKnowledge.length }} 条，通过后才可发布为正式知识。
            </span>
            <el-button
              type="primary"
              :disabled="approvedKnowledgeIds.length === 0"
              :loading="acting"
              @click="publishSelectedKnowledge"
            >
              发布已通过候选
            </el-button>
          </div>
          <el-table
            v-if="knowledgeCandidates.length"
            :data="knowledgeCandidates"
            class="rank-table"
            @selection-change="selectedKnowledge = $event"
          >
            <el-table-column type="selection" width="44" />
            <el-table-column prop="id" label="ID" width="76" />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="plain">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="candidateType" label="类型" width="126" />
            <el-table-column label="标题 / 内容" min-width="280">
              <template #default="{ row }">
                <div class="table-title">{{ row.title || '-' }}</div>
                <div class="table-snippet">{{ shortText(row.content, 160) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="证据摘要" min-width="220">
              <template #default="{ row }">
                <div class="table-snippet">{{ shortText(row.evidenceText, 110) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="hitCount" label="命中" width="84" />
            <el-table-column prop="memberCount" label="成员" width="84" />
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button size="small" type="success" plain :loading="acting" @click="reviewKnowledge(row, 'APPROVED')">
                    通过
                  </el-button>
                  <el-button size="small" type="danger" plain :loading="acting" @click="reviewKnowledge(row, 'REJECTED')">
                    拒绝
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block">暂无候选群梗，输入 batchId 后可以生成候选。</div>
        </el-tab-pane>

        <el-tab-pane label="候选成员画像" name="member-candidates">
          <div class="table-toolbar">
            <span class="panel-subtitle">
              已选 {{ selectedMembers.length }} 条，通过后可发布为正式成员画像。
            </span>
            <el-button
              type="primary"
              :disabled="approvedMemberIds.length === 0"
              :loading="acting"
              @click="publishSelectedMembers"
            >
              发布已通过画像
            </el-button>
          </div>
          <el-table
            v-if="memberCandidates.length"
            :data="memberCandidates"
            class="rank-table"
            @selection-change="selectedMembers = $event"
          >
            <el-table-column type="selection" width="44" />
            <el-table-column prop="id" label="ID" width="76" />
            <el-table-column label="状态" width="96">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="plain">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成员" min-width="170">
              <template #default="{ row }">
                <div class="table-title">{{ row.senderName || row.senderUin || row.senderUid || 'unknown' }}</div>
                <div class="table-snippet">{{ row.senderUid || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="分数" width="86" />
            <el-table-column prop="messageCount" label="发言" width="86" />
            <el-table-column prop="activeDays" label="活跃天" width="92" />
            <el-table-column prop="replyCount" label="回复" width="86" />
            <el-table-column label="候选原因" min-width="250">
              <template #default="{ row }">
                <div class="table-snippet">{{ shortText(row.candidateReason, 150) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button size="small" type="success" plain :loading="acting" @click="reviewMember(row, 'APPROVED')">
                    通过
                  </el-button>
                  <el-button size="small" type="danger" plain :loading="acting" @click="reviewMember(row, 'REJECTED')">
                    拒绝
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block">暂无候选成员画像，输入 batchId 后可以生成候选。</div>
        </el-tab-pane>

        <el-tab-pane label="正式知识" name="formal-knowledge">
          <el-table v-if="formalKnowledge.length" :data="formalKnowledge" class="rank-table">
            <el-table-column prop="id" label="ID" width="76" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="plain">
                  {{ row.status }} / {{ row.enabled ? 'enabled' : 'disabled' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="knowledgeType" label="类型" width="130" />
            <el-table-column label="标题 / 内容" min-width="340">
              <template #default="{ row }">
                <div class="table-title">{{ row.title || '-' }}</div>
                <div class="table-snippet">{{ shortText(row.content, 180) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="创建人" width="120" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  :type="row.enabled ? 'danger' : 'success'"
                  plain
                  :loading="acting"
                  @click="toggleKnowledge(row, !row.enabled)"
                >
                  {{ row.enabled ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block">暂无正式知识。通过候选并发布后会出现在这里。</div>
        </el-tab-pane>

        <el-tab-pane label="正式成员画像" name="formal-members">
          <el-table v-if="memberProfiles.length" :data="memberProfiles" class="rank-table">
            <el-table-column prop="id" label="ID" width="76" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" effect="plain">
                  {{ row.status }} / {{ row.enabled ? 'enabled' : 'disabled' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成员" min-width="170">
              <template #default="{ row }">
                {{ row.senderName || row.senderUin || row.senderUid || 'unknown' }}
              </template>
            </el-table-column>
            <el-table-column label="画像摘要" min-width="360">
              <template #default="{ row }">
                <div class="table-snippet">{{ shortText(row.profileText, 180) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="createdBy" label="创建人" width="120" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  :type="row.enabled ? 'danger' : 'success'"
                  plain
                  :loading="acting"
                  @click="toggleMemberProfile(row, !row.enabled)"
                >
                  {{ row.enabled ? '停用' : '启用' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block">暂无正式成员画像。通过候选并发布后会出现在这里。</div>
        </el-tab-pane>

        <el-tab-pane label="Embedding / 搜索" name="embedding-search">
          <div class="knowledge-tool-grid">
            <section class="tool-box">
              <h4>Embedding 生成</h4>
              <p>对当前群的正式知识和成员画像生成向量；默认跳过已有 embedding。</p>
              <el-checkbox-group v-model="contextForm.targetTypes" class="inline-checks">
                <el-checkbox
                  v-for="item in targetTypeOptions"
                  :key="item.value"
                  :label="item.value"
                >
                  {{ item.label }}
                </el-checkbox>
              </el-checkbox-group>
              <el-checkbox v-model="contextForm.regenerate">重新生成已有 embedding</el-checkbox>
              <el-button type="primary" :loading="acting" @click="generateEmbeddingAction">
                生成 Embedding
              </el-button>
              <div v-if="embeddingResult" class="result-strip">
                <span>embedded {{ embeddingResult.embedded }}</span>
                <span>skipped {{ embeddingResult.skipped }}</span>
                <span>failed {{ embeddingResult.failed }}</span>
              </div>
            </section>

            <section class="tool-box">
              <h4>正式知识搜索</h4>
              <p>用一条消息搜索已发布、可召回的正式知识。</p>
              <el-input
                v-model="contextForm.messageText"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 5 }"
                placeholder="例如：这个操作也太经典了"
              />
              <div class="tool-row">
                <el-input-number v-model="contextForm.topK" :min="1" :max="10" />
                <el-button :icon="Search" type="primary" :loading="acting" @click="searchFormalKnowledge">
                  搜索
                </el-button>
              </div>
            </section>
          </div>

          <el-table v-if="searchResult?.results.length" :data="searchResult.results" class="rank-table">
            <el-table-column prop="targetType" label="来源" width="150" />
            <el-table-column prop="targetId" label="ID" width="90" />
            <el-table-column label="分数" width="100">
              <template #default="{ row }">{{ row.score.toFixed(3) }}</template>
            </el-table-column>
            <el-table-column label="标题 / 内容" min-width="420">
              <template #default="{ row }">
                <div class="table-title">{{ row.title || '-' }}</div>
                <div class="table-snippet">{{ shortText(row.content, 200) }}</div>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="empty-block compact">搜索结果会显示在这里。</div>
        </el-tab-pane>

        <el-tab-pane label="Context 预览" name="context-preview">
          <div class="context-preview-grid">
            <section class="tool-box">
              <h4>预览输入</h4>
              <el-form label-position="top">
                <el-form-item label="消息文本">
                  <el-input
                    v-model="contextForm.messageText"
                    type="textarea"
                    :autosize="{ minRows: 4, maxRows: 8 }"
                    placeholder="输入一条模拟群消息"
                  />
                </el-form-item>
                <div class="number-form-grid">
                  <el-form-item label="发送者 UID">
                    <el-input v-model="contextForm.senderUid" placeholder="可选" clearable />
                  </el-form-item>
                  <el-form-item label="链路">
                    <el-select v-model="contextForm.routeType">
                      <el-option
                        v-for="item in routeOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="Top K">
                    <el-input-number v-model="contextForm.topK" :min="1" :max="10" />
                  </el-form-item>
                </div>
              </el-form>
              <div class="table-actions">
                <el-button type="primary" :loading="acting" @click="previewContext">单链路 Context</el-button>
                <el-button :loading="acting" @click="previewRoutes">A/B/C 灰度预览</el-button>
                <el-button :loading="acting" @click="simulateDifyInputs">Dify Inputs</el-button>
              </div>
            </section>

            <section class="tool-box">
              <h4>当前单链路结果</h4>
              <div v-if="contextPreview" class="settings-list">
                <el-tag effect="plain">{{ contextPreview.routeType }}</el-tag>
                <el-tag :type="contextPreview.knowledgeUsed ? 'success' : 'info'" effect="plain">
                  {{ contextPreview.knowledgeUsed ? 'knowledge used' : 'knowledge empty' }}
                </el-tag>
                <el-tag v-if="contextPreview.silentReason" type="warning" effect="plain">
                  {{ contextPreview.silentReason }}
                </el-tag>
              </div>
              <pre class="json-preview">{{ contextPreview?.knowledgeContext || '暂无 knowledgeContext' }}</pre>
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

          <section v-if="difyPreview" class="tool-box">
            <h4>Dify inputs 模拟</h4>
            <pre class="json-preview">{{ jsonText(difyPreview.inputs) }}</pre>
          </section>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>
