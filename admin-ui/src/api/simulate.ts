import { apiPost } from './client'

export interface SimulateGroupMessageRequest {
  groupId: string
  userId: string
  messageId?: string
  rawMessage: string
  atBot: boolean
  botNicknameMatched: boolean
}

export interface OutboundMessage {
  text?: string | null
  imagePath?: string | null
}

export interface RouteResult {
  routeType: string
  responseType: string
  shouldSend: boolean
  outboundMessage?: OutboundMessage | null
  reason?: string | null
  dedupPassed: boolean
  adminCommandHit: boolean
  memeHit: boolean
  memeId?: number | null
  durationMs?: number | null
  workflowType?: string | null
  sceneCode?: string | null
  confidence?: number | null
  passiveChatHit: boolean
  replyText?: string | null
  chatConfidence?: number | null
  silentReason?: string | null
  activeChatHit?: boolean | null
  activeShouldReply?: boolean | null
  activeConfidence?: number | null
  activeReason?: string | null
  activePolicyPassed?: boolean | null
  activePolicyRejectReason?: string | null
  cooldownSeconds?: number | null
  hourCount?: number | null
  randomHit?: boolean | null
}

export function simulateGroupMessage(request: SimulateGroupMessageRequest) {
  return apiPost<RouteResult>('/dev/simulate/group-message', request)
}

export interface SimulatePrivateMessageRequest {
  userId: string
  messageId?: string
  rawMessage: string
}

export interface PrivateCommandResult {
  userId: string
  messageId: string
  handled: boolean
  shouldReply: boolean
  operation?: string | null
  detail?: string | null
  outboundMessage?: OutboundMessage | null
}

export function simulatePrivateMessage(request: SimulatePrivateMessageRequest) {
  return apiPost<PrivateCommandResult>('/dev/simulate/private-message', request)
}
