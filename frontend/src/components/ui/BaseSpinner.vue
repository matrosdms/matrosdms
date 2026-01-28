<script setup lang="ts">
import { Loader2 } from 'lucide-vue-next'
import { cn } from '@/lib/utils'

defineProps<{
  size?: number
  class?: string
}>()
</script>

<template>
  <Loader2 
    :size="size || 16" 
    :class="cn('animate-spin text-current', $props.class)" 
  />
</template>