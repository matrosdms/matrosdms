<script setup lang="ts">
defineProps<{
  title?: string
  count?: number
  total?: number
}>()

defineSlots<{
  actions?: (props: {}) => any
}>()
</script>

<template>
  <div class="panel-header bg-[#f0f2f5] dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center h-[35px] select-none shrink-0 px-2 transition-colors">
    <div class="flex items-center gap-2 overflow-hidden">
      <span class="text-[13px] font-semibold text-gray-700 dark:text-gray-200 uppercase tracking-wide truncate" :title="title">
        {{ title }}
      </span>
      <div v-if="count !== undefined || total !== undefined" class="text-[10px] bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full font-mono border border-transparent dark:border-gray-600">
        <span v-if="total !== undefined">{{ count }} / {{ total }}</span>
        <span v-else>{{ count }}</span>
      </div>
    </div>
    <div class="flex items-center gap-1 ml-auto">
      <slot name="actions"></slot>
    </div>
  </div>
</template>