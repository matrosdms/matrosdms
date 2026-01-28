<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value: string
  colors?: Record<string, string> // Map value to color classes
  defaultColor?: string
}>()

const badgeClass = computed(() => {
  if (props.colors && props.colors[props.value]) {
    return props.colors[props.value]
  }
  return props.defaultColor || 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300'
})
</script>

<template>
  <span 
    class="px-1.5 py-0.5 rounded text-[10px] font-bold uppercase border border-transparent select-none whitespace-nowrap"
    :class="badgeClass"
  >
    {{ value }}
  </span>
</template>