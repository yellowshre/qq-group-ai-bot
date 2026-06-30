export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly code?: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface ApiEnvelope<T> {
  success: boolean
  code?: string
  message?: string
  data?: T
  timestamp?: string
}

const ADMIN_TOKEN_STORAGE_KEY = 'qqbot.admin.apiToken'

export function getAdminApiToken() {
  return localStorage.getItem(ADMIN_TOKEN_STORAGE_KEY) ?? ''
}

export function setAdminApiToken(token: string) {
  const normalized = token.trim()
  if (normalized) {
    localStorage.setItem(ADMIN_TOKEN_STORAGE_KEY, normalized)
  } else {
    localStorage.removeItem(ADMIN_TOKEN_STORAGE_KEY)
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  return request<T>(() => fetch(path, {
    headers: requestHeaders(),
  }))
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(() => fetch(path, {
    method: 'POST',
    headers: requestHeaders(true),
    body: JSON.stringify(body),
  }))
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  return request<T>(() => fetch(path, {
    method: 'PUT',
    headers: requestHeaders(true),
    body: JSON.stringify(body),
  }))
}

async function request<T>(factory: () => Promise<Response>): Promise<T> {
  let response: Response
  try {
    response = await factory()
  } catch (error) {
    throw new ApiError(networkErrorMessage(error), undefined, 'NETWORK_ERROR')
  }
  return parseResponse<T>(response)
}

function requestHeaders(json = false): HeadersInit {
  const headers: Record<string, string> = {
    Accept: 'application/json',
  }
  if (json) {
    headers['Content-Type'] = 'application/json'
  }
  const token = getAdminApiToken()
  if (token) {
    headers['X-QQBOT-ADMIN-TOKEN'] = token
  }
  return headers
}

async function parseResponse<T>(response: Response): Promise<T> {
  const body = await parseBody(response)
  const envelope = asEnvelope<T>(body)
  if (!response.ok) {
    throw new ApiError(errorMessage(response, body, envelope), response.status, envelope?.code)
  }
  if (envelope?.success === false) {
    throw new ApiError(envelope.message || `Request failed: ${response.status}`, response.status, envelope.code)
  }
  if (envelope?.success === true && hasOwn(envelope, 'data')) {
    return envelope.data as T
  }
  return body as T
}

async function parseBody(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) {
    return undefined
  }
  try {
    return JSON.parse(text) as unknown
  } catch {
    return text
  }
}

function errorMessage(response: Response, body: unknown, envelope: ApiEnvelope<unknown> | null): string {
  if (envelope?.message) {
    return envelope.message
  }
  if (typeof body === 'string' && body.trim()) {
    return body
  }
  if (response.status === 500) {
    return '后端接口不可用或返回 500，请确认 Spring Boot dev/local profile 是否已启动并查看后端日志'
  }
  return `Request failed: ${response.status}`
}

function networkErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message.trim()) {
    return `后端接口请求失败：${error.message}`
  }
  return '后端接口请求失败，请确认 Spring Boot dev/local profile 是否已启动'
}

function asEnvelope<T>(body: unknown): ApiEnvelope<T> | null {
  if (!body || typeof body !== 'object') {
    return null
  }
  const value = body as Record<string, unknown>
  if (typeof value.success !== 'boolean') {
    return null
  }
  if (!hasOwn(value, 'code') && !hasOwn(value, 'message') && !hasOwn(value, 'data') && !hasOwn(value, 'timestamp')) {
    return null
  }
  return value as unknown as ApiEnvelope<T>
}

function hasOwn(value: object, key: string): boolean {
  return Object.prototype.hasOwnProperty.call(value, key)
}
