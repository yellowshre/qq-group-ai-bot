import { apiGet, apiPost } from './client'

export type CandidateStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'DISABLED'

export interface KnowledgeCandidate {
  id: number
  batchId: number
  groupId: string
  candidateType: string
  title: string
  content: string
  sourceSessionId?: number | null
  sourceMessageIds?: string | null
  evidenceText?: string | null
  hitCount?: number | null
  memberCount?: number | null
  confidence?: number | string | null
  status: CandidateStatus
  reviewer?: string | null
  reviewComment?: string | null
  reviewedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MemberCandidate {
  id: number
  batchId: number
  groupId: string
  senderUid?: string | null
  senderUin?: string | null
  senderName?: string | null
  messageCount?: number | null
  rawMessageCount?: number | null
  activeDays?: number | null
  mentionCount?: number | null
  replyCount?: number | null
  repliedByCount?: number | null
  sessionCount?: number | null
  score?: number | null
  candidateReason?: string | null
  status: CandidateStatus
  reviewer?: string | null
  reviewComment?: string | null
  reviewedAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GroupKnowledge {
  id: number
  groupId: string
  sourceCandidateId?: number | null
  knowledgeType?: string | null
  title?: string | null
  content?: string | null
  evidenceText?: string | null
  status?: string | null
  enabled?: boolean | null
  version?: number | null
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MemberProfile {
  id: number
  groupId: string
  sourceMemberCandidateId?: number | null
  senderUid?: string | null
  senderUin?: string | null
  senderName?: string | null
  profileText?: string | null
  messageCount?: number | null
  activeDays?: number | null
  status?: string | null
  enabled?: boolean | null
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface GenerateCandidatesResponse {
  batchId: number
  knowledgeCandidates: number
  memberCandidates: number
  status: string
}

export interface ManualKnowledgeCandidateRequest {
  batchId: number
  groupId: string
  candidateType: string
  title: string
  content: string
  evidenceText?: string | null
  reviewer?: string | null
  reviewComment?: string | null
}

export interface ManualKnowledgeCandidateResponse {
  candidate: KnowledgeCandidate
  duplicate: boolean
}

export interface ReviewLogItem {
  id: number
  targetType: string
  targetId: number
  oldStatus?: string | null
  newStatus?: string | null
  reviewer?: string | null
  reviewComment?: string | null
  createdAt?: string | null
}

export interface ChatHistoryImportResponse {
  batchId: number
  rawMessages: number
  cleanMessages: number
  mentions: number
  replies: number
  sessions: number
  members: number
  status: string
  duplicateImport: boolean
}

export interface PublishResponse {
  published: number
  skipped: number
  status: string
}

export interface EmbeddingGenerateResponse {
  embedded: number
  skipped: number
  failed: number
  status: string
}

export interface KnowledgeSearchResult {
  targetType: string
  targetId: number
  score: number
  title?: string | null
  content?: string | null
}

export interface KnowledgeSearchResponse {
  query: string
  results: KnowledgeSearchResult[]
}

export interface KnowledgeContextItem {
  targetType: string
  targetId: number
  type?: string | null
  title?: string | null
  content?: string | null
  score: number
  usageHint?: string | null
}

export interface KnowledgeContextPreviewResponse {
  routeType: string
  query: string
  knowledgeUsed: boolean
  knowledgeContext?: string | null
  items: KnowledgeContextItem[]
  silentReason?: string | null
}

export interface RouteKnowledgePreview {
  routeType: string
  routeKnowledgeEnabled: boolean
  knowledgeUsed: boolean
  itemCount: number
  maxScore: number
  knowledgeContext?: string | null
  items: KnowledgeContextItem[]
  silentReason?: string | null
}

export interface KnowledgeRoutePreviewResponse {
  groupId: string
  query: string
  routes: RouteKnowledgePreview[]
}

export interface DifyContextSimulateResponse {
  routeType: string
  workflow: string
  query: string
  knowledgeUsed: boolean
  knowledgeContext?: string | null
  items: KnowledgeContextItem[]
  inputs: Record<string, unknown>
}

export interface CandidateQuery {
  batchId?: string | number | null
  groupId?: string | null
  status?: string | null
}

function withQuery(path: string, query: CandidateQuery & { enabled?: boolean | null }) {
  const params = new URLSearchParams()
  if (query.batchId !== null && query.batchId !== undefined && `${query.batchId}`.trim()) {
    params.set('batchId', `${query.batchId}`.trim())
  }
  if (query.groupId?.trim()) params.set('groupId', query.groupId.trim())
  if (query.status?.trim()) params.set('status', query.status.trim())
  if (query.enabled !== null && query.enabled !== undefined) params.set('enabled', `${query.enabled}`)
  const suffix = params.toString()
  return suffix ? `${path}?${suffix}` : path
}

export function generateCandidates(batchId: number, groupId: string) {
  return apiPost<GenerateCandidatesResponse>('/dev/chat-history/candidates/generate', {
    batchId,
    groupId,
  })
}

export function createManualKnowledgeCandidate(request: ManualKnowledgeCandidateRequest) {
  return apiPost<ManualKnowledgeCandidateResponse>('/dev/chat-history/candidates/manual', request)
}

export function listReviewLogs(targetType?: string | null, targetId?: number | string | null) {
  const params = new URLSearchParams()
  if (targetType?.trim()) params.set('targetType', targetType.trim())
  if (targetId !== null && targetId !== undefined && `${targetId}`.trim()) {
    params.set('targetId', `${targetId}`.trim())
  }
  const suffix = params.toString()
  return apiGet<ReviewLogItem[]>(`/dev/chat-history/review-logs${suffix ? `?${suffix}` : ''}`)
}

export function importChatHistory(groupId: string, filePath: string) {
  return apiPost<ChatHistoryImportResponse>('/dev/chat-history/import', {
    groupId,
    filePath,
  })
}

export function listKnowledgeCandidates(query: CandidateQuery) {
  return apiGet<KnowledgeCandidate[]>(withQuery('/dev/chat-history/candidates', query))
}

export function listMemberCandidates(query: CandidateQuery) {
  return apiGet<MemberCandidate[]>(withQuery('/dev/chat-history/member-candidates', query))
}

export function reviewKnowledgeCandidate(
  id: number,
  status: CandidateStatus,
  reviewer: string,
  reviewComment?: string,
) {
  return apiPost<KnowledgeCandidate>(`/dev/chat-history/candidates/${id}/review`, {
    status,
    reviewer,
    reviewComment,
  })
}

export function reviewMemberCandidate(
  id: number,
  status: CandidateStatus,
  reviewer: string,
  reviewComment?: string,
) {
  return apiPost<MemberCandidate>(`/dev/chat-history/member-candidates/${id}/review`, {
    status,
    reviewer,
    reviewComment,
  })
}

export function publishKnowledge(groupId: string, candidateIds: number[], operator: string, comment?: string) {
  return apiPost<PublishResponse>('/dev/chat-history/knowledge/publish', {
    groupId,
    candidateIds,
    operator,
    comment,
  })
}

export function publishMemberProfiles(groupId: string, candidateIds: number[], operator: string, comment?: string) {
  return apiPost<PublishResponse>('/dev/chat-history/member-profiles/publish', {
    groupId,
    candidateIds,
    operator,
    comment,
  })
}

export function listFormalKnowledge(groupId?: string, enabled?: boolean | null) {
  return apiGet<GroupKnowledge[]>(withQuery('/dev/chat-history/knowledge', { groupId, enabled }))
}

export function listMemberProfiles(groupId?: string, enabled?: boolean | null) {
  return apiGet<MemberProfile[]>(withQuery('/dev/chat-history/member-profiles', { groupId, enabled }))
}

export function setKnowledgeEnabled(id: number, enabled: boolean, operator: string, comment?: string) {
  return apiPost<GroupKnowledge>(`/dev/chat-history/knowledge/${id}/${enabled ? 'enable' : 'disable'}`, {
    operator,
    comment,
  })
}

export function setMemberProfileEnabled(id: number, enabled: boolean, operator: string, comment?: string) {
  return apiPost<MemberProfile>(`/dev/chat-history/member-profiles/${id}/${enabled ? 'enable' : 'disable'}`, {
    operator,
    comment,
  })
}

export function generateEmbeddings(groupId: string, targetTypes: string[], regenerate: boolean) {
  return apiPost<EmbeddingGenerateResponse>('/dev/chat-history/knowledge/embeddings/generate', {
    groupId,
    targetTypes,
    regenerate,
  })
}

export function searchKnowledge(groupId: string, query: string, topK: number, targetTypes: string[]) {
  return apiPost<KnowledgeSearchResponse>('/dev/chat-history/knowledge/search', {
    groupId,
    query,
    topK,
    targetTypes,
  })
}

export function previewKnowledgeContext(
  groupId: string,
  messageText: string,
  routeType: string,
  topK: number,
  senderUid?: string,
) {
  return apiPost<KnowledgeContextPreviewResponse>('/dev/chat-history/knowledge/context/preview', {
    groupId,
    messageText,
    senderUid,
    routeType,
    topK,
  })
}

export function previewRouteKnowledge(groupId: string, messageText: string, topK: number, senderUid?: string) {
  return apiPost<KnowledgeRoutePreviewResponse>('/dev/chat-history/knowledge/context/route-preview', {
    groupId,
    messageText,
    senderUid,
    topK,
  })
}

export function simulateDifyContext(
  groupId: string,
  messageText: string,
  routeType: string,
  topK: number,
  senderUid?: string,
) {
  return apiPost<DifyContextSimulateResponse>('/dev/chat-history/dify-context/simulate', {
    groupId,
    messageText,
    senderUid,
    routeType,
    topK,
  })
}
