<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    active?: boolean
    label?: string
    activeText?: string
    inactiveText?: string
  }>(),
  {
    active: false,
    label: '',
    activeText: 'ON',
    inactiveText: 'OFF',
  },
)
</script>

<template>
  <span class="status-badge" :class="{ active: props.active }">
    <span class="status-dot" />
    <span v-if="label" class="status-label">{{ label }}</span>
    <span>{{ props.active ? props.activeText : props.inactiveText }}</span>
  </span>
</template>
