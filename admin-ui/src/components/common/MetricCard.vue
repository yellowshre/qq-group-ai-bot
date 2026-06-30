<script setup lang="ts">
defineProps<{
  label: string
  value: string | number | null | undefined
  hint?: string
}>()
</script>

<template>
  <div class="metric-card">
    <span class="metric-label">{{ label }}</span>
    <strong class="metric-value">{{ value ?? '-' }}</strong>
    <span v-if="hint" class="metric-hint">{{ hint }}</span>
  </div>
</template>
