<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <section class="page-header">
    <div>
      <p class="eyebrow">
        <slot name="eyebrow">Control</slot>
      </p>
      <h2>{{ title }}</h2>
      <p v-if="description" class="page-description">{{ description }}</p>
    </div>
    <div class="page-header-actions">
      <slot name="actions" />
    </div>
  </section>
</template>
