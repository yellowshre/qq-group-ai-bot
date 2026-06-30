import { apiGet } from './client'

export interface TriggerLogItem {
  id: number
  groupId: number
  userId: number
  messageId: string
  originalMsg?: string | null
  responseType: string
  responseText?: string | null
  memeId?: number | null
  workflowType?: string | null
  tokenUsed?: number | null
  cost?: string | null
  durationMs?: number | null
  success?: boolean | null
  errorMsg?: string | null
  createdAt?: string | null
}

export interface AdminOpLogItem {
  id: number
  groupId: number
  operatorUid: number
  operation: string
  detail?: string | null
  createdAt?: string | null
}

export interface TriggerLogQuery {
  groupId?: string | null
  userId?: string | null
  messageId?: string | null
  responseType?: string | null
  workflowType?: string | null
  success?: string | null
  limit?: number
}

export interface AdminOpLogQuery {
  groupId?: string | null
  operatorUid?: string | null
  operation?: string | null
  limit?: number
}

function queryString(query: object) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || `${value}`.trim() === '') continue
    params.set(key, `${value}`.trim())
  }
  const suffix = params.toString()
  return suffix ? `?${suffix}` : ''
}

export function listTriggerLogs(query: TriggerLogQuery) {
  return apiGet<TriggerLogItem[]>(`/dev/admin/logs/triggers${queryString(query)}`)
}

export function listAdminOpLogs(query: AdminOpLogQuery) {
  return apiGet<AdminOpLogItem[]>(`/dev/admin/logs/admin-ops${queryString(query)}`)
}
