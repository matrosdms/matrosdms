<script setup lang="ts">
import { computed } from 'vue'
import { Sparkles, Check, X, ArrowRight } from 'lucide-vue-next'
import type { AiFieldProposal } from '@/composables/useItemForm'

const props = defineProps<{
  proposal: AiFieldProposal
  currentValue?: string
  fieldLabel?: string
  /** If true, the current form value has already been changed away from the AI suggestion */
  isOverridden?: boolean
}>()

const emit = defineEmits<{
  (e: 'accept'): void
  (e: 'dismiss'): void
}>()

const confidencePct = computed(() => Math.round((props.proposal.confidence ?? 0) * 100))
const confidenceColor = computed(() => {
  const c = props.proposal.confidence ?? 0
  if (c >= 0.80) return { bar: 'bg-emerald-500', text: 'text-emerald-700 dark:text-emerald-300', badge: 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300' }
  if (c >= 0.60) return { bar: 'bg-amber-500', text: 'text-amber-700 dark:text-amber-300', badge: 'bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300' }
  return { bar: 'bg-red-400', text: 'text-red-600 dark:text-red-400', badge: 'bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300' }
})

const showDiff = computed(() =>
  props.isOverridden && props.currentValue && props.currentValue !== (props.proposal.displayValue ?? props.proposal.value)
)
const displayLabel = computed(() => props.proposal.displayValue ?? props.proposal.value)
</script>

<template>
  <div class="mt-1 rounded-md border border-purple-200/60 dark:border-purple-800/60 bg-purple-50/50 dark:bg-purple-900/10 px-2.5 py-1.5 flex items-center gap-2 text-xs group animate-in fade-in slide-in-from-top-1 duration-200">

    <!-- Icon + strategy label -->
    <Sparkles :size="11" class="shrink-0 text-purple-500 dark:text-purple-400" />

    <!-- Diff: show both values when overridden -->
    <template v-if="showDiff">
      <span class="text-muted-foreground line-through truncate max-w-[90px]">{{ displayLabel }}</span>
      <ArrowRight :size="10" class="shrink-0 opacity-40" />
      <span class="font-medium text-foreground truncate max-w-[110px]">{{ currentValue }}</span>
    </template>
    <template v-else>
      <span class="text-purple-800 dark:text-purple-200 font-medium truncate max-w-[160px]">{{ displayLabel }}</span>
    </template>

    <!-- Confidence pill -->
    <div class="flex items-center gap-1 shrink-0 ml-auto">
      <div class="flex items-center gap-1 rounded-full px-1.5 py-0.5" :class="confidenceColor.badge">
        <!-- Mini confidence bar -->
        <div class="w-10 h-1.5 rounded-full bg-current/20 overflow-hidden">
          <div class="h-full rounded-full transition-all" :class="confidenceColor.bar" :style="{ width: confidencePct + '%' }" />
        </div>
        <span class="font-mono font-bold" :class="confidenceColor.text">{{ confidencePct }}%</span>
      </div>
    </div>

    <!-- Accept / Dismiss (only show when not yet overridden) -->
    <template v-if="!isOverridden">
      <div class="flex gap-0.5 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
        <button
          @click.stop="emit('accept')"
          class="p-0.5 rounded hover:bg-emerald-100 dark:hover:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400 transition-colors"
          title="Accept AI suggestion"
        >
          <Check :size="11" />
        </button>
        <button
          @click.stop="emit('dismiss')"
          class="p-0.5 rounded hover:bg-red-100 dark:hover:bg-red-900/40 text-red-500 dark:text-red-400 transition-colors"
          title="Dismiss AI suggestion"
        >
          <X :size="11" />
        </button>
      </div>
    </template>
    <template v-else>
      <!-- Reset to AI value -->
      <button
        @click.stop="emit('accept')"
        class="shrink-0 text-[10px] text-purple-600 dark:text-purple-400 hover:underline opacity-0 group-hover:opacity-100 transition-opacity"
        title="Reset to AI suggestion"
      >↩ reset</button>
    </template>
  </div>
</template>
