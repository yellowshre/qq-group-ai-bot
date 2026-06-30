<script setup lang="ts">
import { CopyDocument, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'

import { getChatHistoryInsights, type ChatHistoryInsightResponse, type DailyActivity } from '@/api/insights'
import { getMemberRank, type MemberRankResponse } from '@/api/memberRank'
import PageHeader from '@/components/common/PageHeader.vue'
import { readLastGroupId, rememberLastGroupId } from '@/composables/useAdminPreferences'

const loading = ref(false)
const result = ref<ChatHistoryInsightResponse | null>(null)
const rankResults = ref<MemberRankResponse[]>([])
const form = ref({
  groupId: readLastGroupId(),
  batchId: '',
  startDate: '',
  endDate: '',
  topN: 5,
})

watch(() => form.value.groupId, rememberLastGroupId)

const rankTypes = ['MESSAGE', 'REPLY', 'REPLIED_BY', 'MENTION', 'SESSION']

const summaryCards = computed(() => {
  const summary = result.value?.summary
  return [
    { label: 'Clean messages', value: summary?.cleanMessages ?? 0, hint: '清洗后可用消息' },
    { label: 'Raw messages', value: summary?.rawMessages ?? 0, hint: '原始导入消息' },
    { label: 'Members', value: summary?.members ?? 0, hint: '有统计记录的成员' },
    { label: 'Active days', value: summary?.activeDays ?? 0, hint: '有发言统计的日期' },
    { label: 'Sessions', value: summary?.sessions ?? 0, hint: '会话切分数量' },
    { label: 'Mentions / replies', value: `${summary?.mentions ?? 0} / ${summary?.replies ?? 0}`, hint: '@ 与回复行为' },
  ]
})

const maxDailyMessages = computed(() => {
  const values = result.value?.dailyActivities.map((item) => item.cleanMessages) ?? []
  return Math.max(1, ...values)
})

const rangeText = computed(() => {
  if (!result.value) return '尚未查询'
  if (!result.value.startDate && !result.value.endDate) return '当前批次全量'
  return `${result.value.startDate || ''} 至 ${result.value.endDate || ''}`
})

async function loadInsights() {
  const groupId = form.value.groupId.trim()
  if (!groupId) {
    ElMessage.warning('请先输入 groupId')
    return
  }
  loading.value = true
  try {
    const [insights, ...ranks] = await Promise.all([
      getChatHistoryInsights({
        groupId,
        batchId: form.value.batchId.trim() || null,
        startDate: form.value.startDate || null,
        endDate: form.value.endDate || null,
        topN: form.value.topN,
      }),
      ...rankTypes.map((rankType) =>
        getMemberRank({
          groupId,
          batchId: parseBatchId(),
          rankType,
          startDate: form.value.startDate || null,
          endDate: form.value.endDate || null,
          topN: form.value.topN,
        }),
      ),
    ])
    result.value = insights
    rankResults.value = ranks
    if (!form.value.batchId.trim() && insights.batchId) {
      form.value.batchId = `${insights.batchId}`
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '聊天洞察加载失败')
  } finally {
    loading.value = false
  }
}

function parseBatchId() {
  const value = Number(form.value.batchId)
  return Number.isInteger(value) && value > 0 ? value : null
}

function setQuickRange(value: string) {
  if (value === 'all') {
    form.value.startDate = ''
    form.value.endDate = ''
    return
  }
  const today = new Date()
  const end = formatDate(today)
  const start = new Date(today)
  if (value === '7d') {
    start.setDate(today.getDate() - 6)
  } else if (value === '30d') {
    start.setDate(today.getDate() - 29)
  } else {
    start.setDate(1)
  }
  form.value.startDate = formatDate(start)
  form.value.endDate = end
}

async function copySummary() {
  if (!result.value) {
    ElMessage.warning('当前没有可复制的洞察结果')
    return
  }
  const lines = [
    `群号：${result.value.groupId}`,
    `批次：${result.value.batchId ?? '-'}`,
    `范围：${rangeText.value}`,
    ...summaryCards.value.map((item) => `${item.label}: ${item.value}`),
  ]
  try {
    await navigator.clipboard.writeText(lines.join('\n'))
    ElMessage.success('洞察摘要已复制')
  } catch {
    ElMessage.error('复制失败，可以手动选中内容')
  }
}

function barWidth(item: DailyActivity) {
  return `${Math.max(4, Math.round((item.cleanMessages / maxDailyMessages.value) * 100))}%`
}

function displayName(item: { senderName?: string | null; senderUin?: string | null; senderUid?: string | null }) {
  return item.senderName || item.senderUin || item.senderUid || 'unknown'
}

function formatNumber(value?: number | string | null) {
  if (typeof value === 'string') return value
  return new Intl.NumberFormat('zh-CN').format(value ?? 0)
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="聊天洞察"
      description="基于导入后的统计表做只读可视化，观察群聊活跃、日期趋势、回复和成员排行，不展示完整聊天正文。"
    >
      <template #eyebrow>Chat Insights</template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadInsights">刷新</el-button>
        <el-button :icon="CopyDocument" :disabled="!result" @click="copySummary">复制摘要</el-button>
      </template>
    </PageHeader>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>查询条件</h3>
          <span class="panel-subtitle">日期为空时使用当前批次全量统计；批次为空时后端会取最新成功批次。</span>
        </div>
      </div>
      <el-form class="rank-form" :model="form" label-position="top">
        <el-form-item label="群号">
          <el-input v-model="form.groupId" placeholder="例如 251288204" clearable />
        </el-form-item>
        <el-form-item label="批次 ID">
          <el-input v-model="form.batchId" placeholder="可选，留空取最新成功批次" clearable />
        </el-form-item>
        <el-form-item label="快捷范围">
          <div class="preset-row">
            <el-button size="small" @click="setQuickRange('all')">全量</el-button>
            <el-button size="small" @click="setQuickRange('7d')">最近 7 天</el-button>
            <el-button size="small" @click="setQuickRange('30d')">最近 30 天</el-button>
            <el-button size="small" @click="setQuickRange('month')">本月</el-button>
          </div>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" value-format="YYYY-MM-DD" type="date" />
        </el-form-item>
        <el-form-item label="Top N">
          <el-input-number v-model="form.topN" :min="1" :max="50" />
        </el-form-item>
        <el-form-item class="rank-submit">
          <el-button type="primary" :icon="Search" :loading="loading" @click="loadInsights">查询洞察</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="status-grid">
      <div v-for="item in summaryCards" :key="item.label" class="metric-card">
        <span class="metric-label">{{ item.label }}</span>
        <strong class="metric-value">{{ formatNumber(item.value) }}</strong>
        <span class="metric-hint">{{ item.hint }}</span>
      </div>
    </section>

    <section class="panel-grid two">
      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>日期趋势</h3>
            <span class="panel-subtitle">{{ rangeText }}，按 clean message 画条形趋势。</span>
          </div>
        </div>
        <div v-if="result?.dailyActivities.length" class="activity-list">
          <div v-for="item in result.dailyActivities" :key="item.statDate" class="activity-row">
            <span class="activity-date">{{ item.statDate }}</span>
            <div class="activity-bar-track">
              <div class="activity-bar" :style="{ width: barWidth(item) }" />
            </div>
            <strong>{{ formatNumber(item.cleanMessages) }}</strong>
            <small>{{ formatNumber(item.activeMembers) }} 人</small>
          </div>
        </div>
        <div v-else class="empty-block compact">暂无日期趋势。请输入群号并查询。</div>
      </div>

      <div class="panel">
        <div class="panel-title-row">
          <div>
            <h3>活跃成员摘要</h3>
            <span class="panel-subtitle">按发言数排序，只展示统计摘要。</span>
          </div>
        </div>
        <el-table v-if="result?.topMembers.length" :data="result.topMembers" class="rank-table">
          <el-table-column label="成员" min-width="160">
            <template #default="{ row }">
              <div class="member-cell">
                <strong>{{ displayName(row) }}</strong>
                <span>{{ row.senderUid || row.senderUin || 'uid unknown' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="发言" width="82">
            <template #default="{ row }">{{ formatNumber(row.cleanMessages) }}</template>
          </el-table-column>
          <el-table-column label="@ / 回复" width="110">
            <template #default="{ row }">{{ formatNumber(row.mentions) }} / {{ formatNumber(row.replies) }}</template>
          </el-table-column>
          <el-table-column label="会话" width="82">
            <template #default="{ row }">{{ formatNumber(row.sessions) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-block compact">暂无成员摘要。</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>多维排行</h3>
          <span class="panel-subtitle">复用成员排行接口，一次查看发言、回复、被回复、@ 和会话参与。</span>
        </div>
      </div>
      <div class="rank-board-grid">
        <section v-for="rank in rankResults" :key="rank.rankType" class="tool-box">
          <h4>{{ rank.rankTypeLabel }} Top{{ rank.topN }}</h4>
          <div v-if="rank.items.length" class="compact-rank-list">
            <div v-for="item in rank.items" :key="`${rank.rankType}-${item.rank}`" class="compact-rank-item">
              <span>{{ item.rank }}</span>
              <strong>{{ displayName(item) }}</strong>
              <em>{{ formatNumber(item.score) }}</em>
            </div>
          </div>
          <div v-else class="empty-block compact">暂无排行数据</div>
        </section>
      </div>
    </section>
  </div>
</template>
