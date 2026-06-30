import { apiGet, apiPost, apiPut } from './client'

export interface SceneDict {
  sceneCode: string
  sceneDesc: string
  confidenceThreshold: number | string
}

export interface MemeMaterial {
  memeId: number
  keywords: string
  sceneCode?: string | null
  sceneDesc?: string | null
  weight: number
  enabled: boolean
  filePath: string
  createdAt?: string | null
  updatedAt?: string | null
}

export interface MemeFileCheckItem {
  memeId: number
  sceneCode?: string | null
  enabled?: boolean | null
  filePath: string
  resolvedPath?: string | null
  oneBotFile?: string | null
  exists?: boolean | null
  checkable: boolean
  directReference: boolean
  warning?: string | null
}

export interface SceneDictRequest {
  sceneDesc: string
  confidenceThreshold: number
}

export interface MemeMaterialRequest {
  keywords: string
  sceneCode?: string | null
  sceneDesc?: string | null
  weight: number
  enabled: boolean
  filePath: string
}

export interface MemeMaterialQuery {
  sceneCode?: string | null
  enabled?: boolean | null
  keyword?: string | null
}

function withQuery(path: string, query: MemeMaterialQuery) {
  const params = new URLSearchParams()
  if (query.sceneCode?.trim()) params.set('sceneCode', query.sceneCode.trim())
  if (query.enabled !== null && query.enabled !== undefined) params.set('enabled', `${query.enabled}`)
  if (query.keyword?.trim()) params.set('keyword', query.keyword.trim())
  const suffix = params.toString()
  return suffix ? `${path}?${suffix}` : path
}

export function listScenes() {
  return apiGet<SceneDict[]>('/dev/admin/memes/scenes')
}

export function saveScene(sceneCode: string, request: SceneDictRequest) {
  return apiPut<SceneDict>(`/dev/admin/memes/scenes/${encodeURIComponent(sceneCode)}`, request)
}

export function listMemeMaterials(query: MemeMaterialQuery) {
  return apiGet<MemeMaterial[]>(withQuery('/dev/admin/memes/materials', query))
}

export function createMemeMaterial(request: MemeMaterialRequest) {
  return apiPost<MemeMaterial>('/dev/admin/memes/materials', request)
}

export function updateMemeMaterial(memeId: number, request: MemeMaterialRequest) {
  return apiPut<MemeMaterial>(`/dev/admin/memes/materials/${memeId}`, request)
}

export function preheatMemeCache() {
  return apiPost<{ success: boolean }>('/dev/admin/memes/cache/preheat', {})
}

export function checkMemeFiles(query: Pick<MemeMaterialQuery, 'sceneCode' | 'enabled'>) {
  return apiGet<MemeFileCheckItem[]>(withQuery('/dev/admin/memes/files/check', query))
}
