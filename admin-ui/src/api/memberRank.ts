import { apiGet, apiPost } from './client'

export interface MemberRankRequest {
  groupId: string
  batchId?: number | null
  rankType: string
  startDate?: string | null
  endDate?: string | null
  topN?: number | null
}

export interface MemberRankItem {
  rank: number
  senderUid?: string | null
  senderUin?: string | null
  senderName?: string | null
  score: number
  rawMessageCount: number
  messageCount: number
  activeDays: number
  mentionCount: number
  replyCount: number
  repliedByCount: number
  sessionCount: number
}

export interface MemberRankResponse {
  groupId: string
  batchId?: number | null
  rankType: string
  rankTypeLabel: string
  startDate?: string | null
  endDate?: string | null
  topN: number
  items: MemberRankItem[]
}

export function getMemberRank(request: MemberRankRequest) {
  const params = new URLSearchParams()
  params.set('groupId', request.groupId)
  params.set('rankType', request.rankType)
  if (request.batchId) params.set('batchId', String(request.batchId))
  if (request.startDate) params.set('startDate', request.startDate)
  if (request.endDate) params.set('endDate', request.endDate)
  if (request.topN) params.set('topN', String(request.topN))
  return apiGet<MemberRankResponse>(`/dev/chat-history/member-rank?${params}`)
}

export function postMemberRank(request: MemberRankRequest) {
  return apiPost<MemberRankResponse>('/dev/chat-history/member-rank', request)
}
