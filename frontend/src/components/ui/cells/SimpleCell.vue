<script setup lang="ts">
defineProps<{
  value: any
  bold?: boolean
  mono?: boolean
  class?: string
}>()
</script>

<template>
  <span 
    class="truncate text-xs text-gray-800 dark:text-gray-200 block"
    :class="[
        bold ? 'font-bold' : 'font-medium',
        mono ? 'font-mono text-gray-500 dark:text-gray-400' : '',
        $props.class
    ]"
    :title="String(value)"
  >
    {{ value }}
  </span>
</template>