<script setup lang="ts">
import { computed } from 'vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import { EActionStatus, EJobStatus, EStage, EActionPriority } from '@/enums'

const props = defineProps<{
  status: string | undefined | null
  type?: 'action' | 'job' | 'stage' | 'priority' // Context hint
}>()

const variant = computed(() => {
  const s = props.status
  if (!s) return 'secondary'

  // Job Statuses
  if (s === EJobStatus.COMPLETED) return 'success'
  if (s === EJobStatus.FAILED) return 'destructive'
  if (s === EJobStatus.RUNNING) return 'default'
  if (s === EJobStatus.QUEUED) return 'warning'

  // Action Statuses
  if (s === EActionStatus.DONE) return 'success'
  if (s === EActionStatus.REJECTED) return 'destructive'
  if (s === EActionStatus.IN_PROGRESS) return 'default'
  if (s === EActionStatus.ON_HOLD) return 'warning'

  // Stage (Lifecycle II: Informational)
  if (s === EStage.ACTIVE) return 'success' // or default
  if (s === EStage.CLOSED) return 'secondary' // Grey/Dimmed

  // Priority
  if (s === EActionPriority.HIGH) return 'destructive'
  if (s === EActionPriority.LOW) return 'secondary'

  return 'secondary'
})
</script>

<template>
  <BaseBadge :variant="variant" class="uppercase tracking-wider text-[10px]">
    {{ status || 'UNKNOWN' }}
  </BaseBadge>
</template>