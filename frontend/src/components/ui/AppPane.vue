<script setup lang="ts">
import PaneHeader from '@/components/ui/PaneHeader.vue'

defineProps<{
  title: string
  count?: number
  total?: number
}>()

defineSlots<{
  default?: (props: {}) => any
  actions?: (props: {}) => any
  filter?: (props: {}) => any
}>()
</script>

<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 transition-colors">
    <div class="shrink-0">
        <PaneHeader :title="title" :count="count" :total="total">
            <template #actions>
                <slot name="actions"></slot>
            </template>
        </PaneHeader>
        <div v-if="$slots.filter" class="panel-filter bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800">
            <slot name="filter"></slot>
        </div>
    </div>
    
    <div class="flex-1 overflow-auto relative custom-scrollbar bg-gray-50/50 dark:bg-gray-900">
        <slot></slot>
    </div>
  </div>
</template>