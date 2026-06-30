import { ref, watch, type Ref } from 'vue'

const LAST_GROUP_ID_KEY = 'qqbot.admin.lastGroupId'
const LAST_OPERATOR_KEY = 'qqbot.admin.lastOperator'
const DEFAULT_OPERATOR = 'local-admin'

export function useLastGroupId(): Ref<string> {
  return useStoredText(LAST_GROUP_ID_KEY, '')
}

export function useLastOperator(): Ref<string> {
  return useStoredText(LAST_OPERATOR_KEY, DEFAULT_OPERATOR)
}

export function readLastGroupId() {
  return readStoredText(LAST_GROUP_ID_KEY, '')
}

export function readLastOperator() {
  return readStoredText(LAST_OPERATOR_KEY, DEFAULT_OPERATOR)
}

export function rememberLastGroupId(value?: string | null) {
  writeStoredText(LAST_GROUP_ID_KEY, value)
}

export function rememberLastOperator(value?: string | null) {
  writeStoredText(LAST_OPERATOR_KEY, value || DEFAULT_OPERATOR)
}

function useStoredText(key: string, fallback: string): Ref<string> {
  const value = ref(readStoredText(key, fallback))
  watch(value, (next) => writeStoredText(key, next))
  return value
}

function readStoredText(key: string, fallback: string) {
  if (!canUseLocalStorage()) {
    return fallback
  }
  return localStorage.getItem(key) || fallback
}

function writeStoredText(key: string, value?: string | null) {
  if (!canUseLocalStorage()) {
    return
  }
  const normalized = value?.trim() ?? ''
  if (normalized) {
    localStorage.setItem(key, normalized)
  } else {
    localStorage.removeItem(key)
  }
}

function canUseLocalStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}
