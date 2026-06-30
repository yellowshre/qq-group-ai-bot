import { apiGet } from './client'

export interface DependencyStatus {
  reachable: boolean
  detail: string
}

export interface OneBotStatus {
  wsEnabled: boolean
  selfId: string
  allowedGroupIds: string[]
}

export interface DifyStatus {
  enabled: boolean
  baseUrlConfigured: boolean
  memeSceneWorkflowConfigured: boolean
  passiveChatWorkflowConfigured: boolean
  activeChatWorkflowConfigured: boolean
  memeSceneApiKeyConfigured: boolean
  passiveChatApiKeyConfigured: boolean
  activeChatApiKeyConfigured: boolean
}

export interface AdminUiStatus {
  apiTokenEnabled: boolean
  apiTokenConfigured: boolean
  apiTokenProtected: boolean
}

export interface KnowledgeContextConfig {
  maxItems: number
  maxLength: number
  minScore: number
  memberProfileLimit: number
  maxSearchCandidates: number
  maxItemContentLength: number
}

export interface GroupConfigSnapshot {
  groupId: string
  botOn: boolean
  enableChat: boolean
  enableMeme: boolean
  enableAutoJoin: boolean
  enablePassiveChat: boolean
  activeCooldownSeconds: number
  activeMaxPerHour: number
  activeMaxPerDay: number
  safeWord?: string | null
  safeWordReply?: string | null
  persona?: string
  memoryMode?: string | null
  enableKnowledgeContext: boolean
  enableMemeKnowledge: boolean
  enablePassiveChatKnowledge: boolean
  enableActiveChatKnowledge: boolean
}

export interface FullHealthResponse {
  activeProfiles: string[]
  mysql: DependencyStatus
  redis: DependencyStatus
  difyEnabled: boolean
  memeCachePreheatEnabled: boolean
  messageSenderType: string
  adminUi: AdminUiStatus
  sceneDictCount: number | null
  enabledMemeMaterialCount: number | null
  oneBot: OneBotStatus
  dify: DifyStatus
  memeBaseDir: string
  knowledgeEmbeddingEnabled: boolean
  knowledgeContextEnabled: boolean
  memeKnowledgeEnabled: boolean
  passiveChatKnowledgeEnabled: boolean
  activeChatKnowledgeEnabled: boolean
  knowledgeContextConfig: KnowledgeContextConfig
  groupConfig?: GroupConfigSnapshot | null
}

export function getFullHealth(groupId?: string) {
  const search = groupId ? `?groupId=${encodeURIComponent(groupId)}` : ''
  return apiGet<FullHealthResponse>(`/dev/health/full${search}`)
}
