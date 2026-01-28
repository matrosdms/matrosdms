<script setup lang="ts">
import BaseSpinner from '@/components/ui/BaseSpinner.vue'

defineProps<{
  message?: string
  subtext?: string
}>()
</script>

<template>
  <div class="flex items-center gap-3 p-3 bg-blue-50/50 dark:bg-blue-900/10 border border-blue-100 dark:border-blue-800 rounded-lg">
    <div class="p-2 bg-white dark:bg-blue-900/30 rounded-full shadow-sm">
      <BaseSpinner class="text-blue-600 dark:text-blue-400" :size="20" />
    </div>
    <div class="flex-col flex">
      <span class="text-xs font-bold text-blue-700 dark:text-blue-300 animate-pulse">
        {{ message || 'Processing...' }}
      </span>
      <span v-if="subtext" class="text-[10px] text-blue-500/80 dark:text-blue-400/70">
        {{ subtext }}
      </span>
    </div>
  </div>
</template>