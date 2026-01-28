<script setup lang="ts">
import { cn } from '@/lib/utils'

defineProps<{
  class?: string
  title?: string
  description?: string
  noPadding?: boolean
}>()
</script>

<template>
  <div :class="cn('rounded-xl border bg-card text-card-foreground shadow-sm bg-white dark:bg-gray-900 border-gray-200 dark:border-gray-800', $props.class)">
    
    <!-- Header -->
    <div v-if="title || $slots.header || $slots.actions" class="flex flex-col space-y-1.5 p-6 border-b border-gray-100 dark:border-gray-800">
      <div class="flex items-center justify-between">
          <div v-if="title || description">
            <h3 v-if="title" class="font-semibold leading-none tracking-tight">{{ title }}</h3>
            <p v-if="description" class="text-sm text-muted-foreground mt-1">{{ description }}</p>
          </div>
          <slot name="header" />
          <div v-if="$slots.actions" class="flex items-center gap-2">
             <slot name="actions" />
          </div>
      </div>
    </div>

    <!-- Body -->
    <div :class="noPadding ? 'p-0' : 'p-6'">
      <slot />
    </div>

    <!-- Footer -->
    <div v-if="$slots.footer" class="flex items-center p-4 pt-0 border-t border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/50 rounded-b-xl">
      <slot name="footer" />
    </div>
  </div>
</template>