import { apiGet } from './client'

export interface InsightSummary {
  rawMessages: number
  cleanMessages: number
  mentions: number
  replies: number
  repliedBy: number
  sessions: number
  members: number
  activeDays: number
  firstMessageAt?: string | null
  lastMessageAt?: string | null
}

export interface DailyActivity {
  statDate: string
  rawMessages: number
  cleanMessages: number
  mentions: number
  replies: number
  repliedBy: number
  sessions: number
  activeMembers: number
}

export interface InsightMemberDigest {
  senderUid?: string | null
  senderUin?: string | null
  senderName?: string | null
  rawMessages: number
  cleanMessages: number
  mentions: number
  replies: number
  repliedBy: number
  sessions: number
}

export interface ChatHistoryInsightResponse {
  groupId: string
  batchId?: number | null
  startDate?: string | null
  endDate?: string | null
  topN: number
  summary: InsightSummary
  dailyActivities: DailyActivity[]
  topMembers: InsightMemberDigest[]
}

export interface ChatHistoryInsightRequest {
  groupId: string
  batchId?: number | string | null
  startDate?: string | null
  endDate?: string | null
  topN?: number | null
}

export function getChatHistoryInsights(request: ChatHistoryInsightRequest) {
  const params = new URLSearchParams()
  params.set('groupId', request.groupId)
  if (request.batchId !== null && request.batchId !== undefined && `${request.batchId}`.trim()) {
    params.set('batchId', `${request.batchId}`.trim())
  }
  if (request.startDate) params.set('startDate', request.startDate)
  if (request.endDate) params.set('endDate', request.endDate)
  if (request.topN) params.set('topN', `${request.topN}`)
  return apiGet<ChatHistoryInsightResponse>(`/dev/chat-history/insights?${params}`)
}
