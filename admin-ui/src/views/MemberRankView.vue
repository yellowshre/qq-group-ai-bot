<script setup lang="ts">
import { CopyDocument, Download, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { getMemberRank, type MemberRankResponse } from '@/api/memberRank'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const result = ref<MemberRankResponse | null>(null)
const form = ref({
  groupId: '',
  batchId: '',
  rankType: 'MESSAGE',
  startDate: '',
  endDate: '',
  topN: 5,
})

const rankOptions = [
  { label: '发言数', value: 'MESSAGE', hint: 'clean message 统计，最适合看群内活跃度' },
  { label: '原始消息数', value: 'RAW_MESSAGE', hint: '导入原始消息统计，用于排查清洗前数据' },
  { label: '活跃天数', value: 'ACTIVE_DAYS', hint: '按有发言的日期计数，适合看稳定在线的人' },
  { label: '提到别人', value: 'MENTION', hint: '统计 @ / mention 行为' },
  { label: '回复别人', value: 'REPLY', hint: '统计主动回复次数' },
  { label: '被回复', value: 'REPLIED_BY', hint: '统计被别人回复的次数' },
  { label: '参与会话', value: 'SESSION', hint: '按会话切分后的参与次数' },
]

const quickRanges = [
  { label: '全量', value: 'all' },
  { label: '最近 7 天', value: '7d' },
  { label: '最近 30 天', value: '30d' },
  { label: '本月', value: 'month' },
]

const rangeText = computed(() => {
  if (!result.value) return '尚未查询'
  if (!result.value.startDate && !result.value.endDate) return '当前批次全量统计'
  return `${result.value.startDate ?? ''} 至 ${result.value.endDate ?? ''}`
})

const selectedRankOption = computed(() =>
  rankOptions.find((item) => item.value === form.value.rankType) ?? rankOptions[0],
)

const resultTitle = computed(() => {
  if (!result.value) return '查询结果'
  return `${result.value.rankTypeLabel}排行 Top${result.value.topN}`
})

const resultSummary = computed(() => {
  if (!result.value) return []
  return [
    `groupId ${result.value.groupId}`,
    result.value.batchId ? `batchId ${result.value.batchId}` : '全部批次',
    result.value.rankType,
    rangeText.value,
    `${result.value.items.length} 条结果`,
  ]
})

async function queryRank() {
  if (!form.value.groupId.trim()) {
    ElMessage.warning('请先输入 groupId')
    return
  }
  loading.value = true
  try {
    result.value = await getMemberRank({
      groupId: form.value.groupId.trim(),
      batchId: parsePositiveInteger(form.value.batchId),
      rankType: form.value.rankType,
      startDate: form.value.startDate || null,
      endDate: form.value.endDate || null,
      topN: form.value.topN,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '排行查询失败')
  } finally {
    loading.value = false
  }
}

function setRankType(rankType: string) {
  form.value.rankType = rankType
}

function resetForm() {
  form.value.rankType = 'MESSAGE'
  form.value.batchId = ''
  form.value.startDate = ''
  form.value.endDate = ''
  form.value.topN = 5
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
  } else if (value === 'month') {
    start.setDate(1)
  }
  form.value.startDate = formatDate(start)
  form.value.endDate = end
}

async function copyResult() {
  if (!result.value?.items.length) {
    ElMessage.warning('当前没有可复制的排行结果')
    return
  }
  const lines = [
    `${result.value.rankTypeLabel}排行 Top${result.value.topN}`,
    `群号：${result.value.groupId}`,
    `范围：${rangeText.value}`,
    ...result.value.items.map((item) => `${item.rank}. ${displayName(item)} - ${item.score}`),
  ]
  try {
    await navigator.clipboard.writeText(lines.join('\n'))
    ElMessage.success('排行文本已复制')
  } catch {
    ElMessage.error('复制失败，可以手动选中表格内容')
  }
}

function downloadCsv() {
  if (!result.value?.items.length) {
    ElMessage.warning('当前没有可导出的排行结果')
    return
  }
  const rows = [
    [
      'rank',
      'senderUid',
      'senderUin',
      'senderName',
      'score',
      'rawMessageCount',
      'messageCount',
      'activeDays',
      'mentionCount',
      'replyCount',
      'repliedByCount',
      'sessionCount',
    ],
    ...result.value.items.map((item) => [
      item.rank,
      item.senderUid ?? '',
      item.senderUin ?? '',
      item.senderName ?? '',
      item.score,
      item.rawMessageCount,
      item.messageCount,
      item.activeDays,
      item.mentionCount,
      item.replyCount,
      item.repliedByCount,
      item.sessionCount,
    ]),
  ]
  const csv = rows.map((row) => row.map(escapeCsv).join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `member_rank_${result.value.groupId}_${result.value.rankType}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

function displayName(item: MemberRankResponse['items'][number]) {
  return item.senderName || item.senderUin || item.senderUid || 'unknown'
}

function parsePositiveInteger(value: string) {
  const normalized = value.trim()
  if (!normalized) return null
  const parsed = Number(normalized)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function escapeCsv(value: unknown) {
  const text = String(value ?? '')
  if (/[",\n\r]/.test(text)) {
    return `"${text.replaceAll('"', '""')}"`
  }
  return text
}
</script>

<template>
  <div class="page-stack">
    <PageHeader
      title="成员排行"
      description="基于 chat_member_stat 与 chat_member_stat_daily 查询统计排行，可按日期范围过滤。"
    >
      <template #eyebrow>Member Rank</template>
    </PageHeader>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>查询条件</h3>
          <span class="panel-subtitle">只读统计表，不返回聊天正文。日期留空时使用当前批次全量统计。</span>
        </div>
        <el-button :icon="Refresh" @click="resetForm">重置筛选</el-button>
      </div>

      <div class="rank-preset-grid">
        <button
          v-for="item in rankOptions"
          :key="item.value"
          class="rank-preset-card"
          :class="{ active: form.rankType === item.value }"
          type="button"
          @click="setRankType(item.value)"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.hint }}</small>
        </button>
      </div>

      <el-form class="rank-form" :model="form" label-position="top">
        <el-form-item label="群号">
          <el-input v-model="form.groupId" placeholder="例如 251288204" clearable />
        </el-form-item>
        <el-form-item label="批次 ID（可选）">
          <el-input v-model="form.batchId" placeholder="留空查询全部批次" clearable />
        </el-form-item>
        <el-form-item label="排行类型">
          <el-select v-model="form.rankType">
            <el-option
              v-for="item in rankOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="快捷范围">
          <div class="preset-row">
            <el-button
              v-for="item in quickRanges"
              :key="item.value"
              size="small"
              @click="setQuickRange(item.value)"
            >
              {{ item.label }}
            </el-button>
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
          <el-button type="primary" :icon="Search" :loading="loading" @click="queryRank">查询排行</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <div>
          <h3>{{ resultTitle }}</h3>
          <span class="panel-subtitle">{{ selectedRankOption.hint }}</span>
        </div>
        <div class="table-actions">
          <el-button :icon="CopyDocument" :disabled="!result?.items.length" @click="copyResult">复制文本</el-button>
          <el-button :icon="Download" :disabled="!result?.items.length" @click="downloadCsv">导出 CSV</el-button>
        </div>
      </div>

      <div v-if="result" class="result-strip rank-result-strip">
        <span v-for="item in resultSummary" :key="item">{{ item }}</span>
      </div>

      <el-table v-if="result?.items.length" :data="result.items" class="rank-table">
        <el-table-column prop="rank" label="#" width="72" />
        <el-table-column label="成员" min-width="180">
          <template #default="{ row }">
            <div class="member-cell">
              <strong>{{ displayName(row) }}</strong>
              <span>{{ row.senderUid || row.senderUin || 'uid unknown' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="当前分数" width="118">
          <template #default="{ row }">
            <strong class="rank-score">{{ formatNumber(row.score) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="发言" width="96">
          <template #default="{ row }">{{ formatNumber(row.messageCount) }}</template>
        </el-table-column>
        <el-table-column label="原始" width="96">
          <template #default="{ row }">{{ formatNumber(row.rawMessageCount) }}</template>
        </el-table-column>
        <el-table-column label="提到" width="96">
          <template #default="{ row }">{{ formatNumber(row.mentionCount) }}</template>
        </el-table-column>
        <el-table-column label="回复" width="96">
          <template #default="{ row }">{{ formatNumber(row.replyCount) }}</template>
        </el-table-column>
        <el-table-column label="被回复" width="96">
          <template #default="{ row }">{{ formatNumber(row.repliedByCount) }}</template>
        </el-table-column>
        <el-table-column label="会话" width="96">
          <template #default="{ row }">{{ formatNumber(row.sessionCount) }}</template>
        </el-table-column>
        <el-table-column label="活跃天" width="96">
          <template #default="{ row }">{{ formatNumber(row.activeDays) }}</template>
        </el-table-column>
      </el-table>
      <div v-else class="empty-block">
        输入群号并查询后，这里会显示成员排行。
      </div>
    </section>
  </div>
</template>
