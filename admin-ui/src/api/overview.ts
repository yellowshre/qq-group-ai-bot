import { apiGet } from './client'

export interface LatestImport {
  batchId: number
  status: string
  rawCount: number
  cleanCount: number
  sessionCount: number
  memberCount: number
  sourceFile: string
  createdAt: string
}

export interface AdminOverviewResponse {
  groupId?: string | null
  generatedAt: string
  importBatches?: number | null
  rawMessages?: number | null
  cleanMessages?: number | null
  sessions?: number | null
  memberStats?: number | null
  knowledgeCandidates?: number | null
  pendingKnowledgeCandidates?: number | null
  memberCandidates?: number | null
  pendingMemberCandidates?: number | null
  activeGroupKnowledge?: number | null
  enabledGroupKnowledge?: number | null
  activeMemberProfiles?: number | null
  enabledMemberProfiles?: number | null
  successfulEmbeddings?: number | null
  triggerLogsToday?: number | null
  adminOpsToday?: number | null
  latestImport?: LatestImport | null
}

export function getAdminOverview(groupId?: string) {
  const search = groupId ? `?groupId=${encodeURIComponent(groupId)}` : ''
  return apiGet<AdminOverviewResponse>(`/dev/admin/overview${search}`)
}
