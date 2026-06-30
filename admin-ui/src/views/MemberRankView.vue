<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { getMemberRank, type MemberRankResponse } from '@/api/memberRank'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = ref(false)
const result = ref<MemberRankResponse | null>(null)
const form = ref({
  groupId: '',
  rankType: 'MESSAGE',
  startDate: '',
  endDate: '',
  topN: 5,
})

const rankOptions = [
  { label: '发言数', value: 'MESSAGE' },
  { label: '原始消息数', value: 'RAW_MESSAGE' },
  { label: '活跃天数', value: 'ACTIVE_DAYS' },
  { label: '提到别人', value: 'MENTION' },
  { label: '回复别人', value: 'REPLY' },
  { label: '被回复', value: 'REPLIED_BY' },
  { label: '参与会话', value: 'SESSION' },
]

const rangeText = computed(() => {
  if (!result.value) return '尚未查询'
  if (!result.value.startDate && !result.value.endDate) return '当前批次全量统计'
  return `${result.value.startDate ?? ''} 至 ${result.value.endDate ?? ''}`
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
      <el-form class="rank-form" :model="form" label-position="top">
        <el-form-item label="群号">
          <el-input v-model="form.groupId" placeholder="例如 251288204" clearable />
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
          <el-button type="primary" :loading="loading" @click="queryRank">查询排行</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title-row">
        <h3>{{ result ? `${result.rankTypeLabel}排行 Top${result.topN}` : '查询结果' }}</h3>
        <span class="panel-subtitle">{{ rangeText }}</span>
      </div>
      <el-table v-if="result?.items.length" :data="result.items" class="rank-table">
        <el-table-column prop="rank" label="#" width="72" />
        <el-table-column label="成员" min-width="180">
          <template #default="{ row }">
            {{ row.senderName || row.senderUin || row.senderUid || 'unknown' }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="110" />
        <el-table-column prop="messageCount" label="发言" width="100" />
        <el-table-column prop="replyCount" label="回复" width="100" />
        <el-table-column prop="repliedByCount" label="被回复" width="100" />
        <el-table-column prop="activeDays" label="活跃天" width="100" />
      </el-table>
      <div v-else class="empty-block">
        输入群号并查询后，这里会显示成员排行。
      </div>
    </section>
  </div>
</template>
