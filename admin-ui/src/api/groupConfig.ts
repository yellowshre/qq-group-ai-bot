import type { GroupConfigSnapshot } from './health'
import { apiGet, apiPut } from './client'

export interface GroupConfigListResponse {
  allowedGroupIds: string[]
  configuredGroups: GroupConfigSnapshot[]
}

export interface GroupConfigUpdateRequest {
  botOn: boolean
  enableChat: boolean
  enableMeme: boolean
  enablePassiveChat: boolean
  enableAutoJoin: boolean
  activeCooldownSeconds: number
  activeMaxPerHour: number
  activeMaxPerDay: number
  safeWord?: string | null
  safeWordReply?: string | null
  persona?: string | null
  memoryMode?: string | null
  enableKnowledgeContext: boolean
  enableMemeKnowledge: boolean
  enablePassiveChatKnowledge: boolean
  enableActiveChatKnowledge: boolean
}

export function listGroupConfigs() {
  return apiGet<GroupConfigListResponse>('/dev/admin/groups')
}

export function getGroupConfig(groupId: string) {
  return apiGet<GroupConfigSnapshot>(`/dev/admin/groups/${encodeURIComponent(groupId)}`)
}

export function updateGroupConfig(groupId: string, request: GroupConfigUpdateRequest) {
  return apiPut<GroupConfigSnapshot>(`/dev/admin/groups/${encodeURIComponent(groupId)}`, request)
}
