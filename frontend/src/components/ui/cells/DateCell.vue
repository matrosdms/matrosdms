<script setup lang="ts">
import { computed } from 'vue'
import { parseBackendDate } from '@/lib/utils'

const props = defineProps<{
  value: any
}>()

const displayDate = computed(() => {
    const d = parseBackendDate(props.value)
    return d ? d.toLocaleString() : '-'
})
</script>

<template>
  <span class="text-xs text-gray-500 dark:text-gray-400 font-mono truncate">
    {{ displayDate }}
  </span>
</template>