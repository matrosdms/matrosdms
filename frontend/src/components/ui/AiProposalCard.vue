<script setup lang="ts">
import { computed } from 'vue'
import { Folder, FileType, Calendar, ArrowRight } from 'lucide-vue-next'
import { useQuery } from '@tanstack/vue-query'
import { CategoryService } from '@/services/CategoryService'
import { ERootCategory } from '@/enums'
import type { ClientPrediction } from '@/types/events'

const props = defineProps<{
  prediction: ClientPrediction
  contexts: any[]
}>()

// ─── Kind Tree (cached — same as useItemForm) ──────────────────────────────
const { data: kindTree } = useQuery({
  queryKey: ['category', ERootCategory.KIND, true],
  queryFn: () => CategoryService.getTree(ERootCategory.KIND),
  staleTime: 10 * 60 * 1000
})

const findInTree = (node: any, uuid: string): string | null => {
  if (!node) return null
  if (node.uuid === uuid) return node.name
  for (const child of (node.children || [])) {
    const found = findInTree(child, uuid)
    if (found) return found
  }
  return null
}

// ─── Derived values ────────────────────────────────────────────────────────
const contextName = computed(() => {
  const id = props.prediction.context
  if (!id) return null
  return props.contexts.find((c: any) => c.uuid === id)?.name ?? null
})

const kindName = computed(() => {
  const id = props.prediction.kind
  if (!id) return null
  return findInTree(kindTree.value, id) ?? id
})

const fieldConf = computed<Record<string, number>>(() => props.prediction.fieldConfidences ?? {})
const overall = computed(() => props.prediction.confidence ?? 0)

// ─── Per-field rows ────────────────────────────────────────────────────────
interface FieldRow { key: string; label: string; icon: any; value: string | null; confidence: number }

const fieldRows = computed<FieldRow[]>(() => {
  const rows: FieldRow[] = []
  if (contextName.value) {
    rows.push({ key: 'context', label: 'Folder', icon: Folder, value: contextName.value, confidence: fieldConf.value.context ?? overall.value })
  }
  if (kindName.value) {
    rows.push({ key: 'kind', label: 'Type', icon: FileType, value: kindName.value, confidence: fieldConf.value.kind ?? overall.value })
  }
  if (props.prediction.documentDate) {
    rows.push({ key: 'date', label: 'Date', icon: Calendar, value: props.prediction.documentDate, confidence: fieldConf.value.documentDate ?? overall.value })
  }
  return rows
})

// ─── Colour palettes ────────────────────────────────────────────────────────
const confidenceBarColor = (c: number) => {
  if (c >= 0.80) return 'bg-emerald-500'
  if (c >= 0.60) return 'bg-amber-500'
  return 'bg-red-400'
}
</script>

<template>
  <div class="rounded-lg border border-purple-100 dark:border-purple-900/60 bg-gradient-to-br from-purple-50/60 to-transparent dark:from-purple-900/10 dark:to-transparent p-2.5 flex flex-col gap-2 transition-colors group-hover:border-purple-200 dark:group-hover:border-purple-800">

    <!-- Field rows -->
    <div v-if="fieldRows.length" class="flex flex-col gap-1.5 mt-0.5">
      <div
        v-for="row in fieldRows"
        :key="row.key"
        class="flex items-center gap-2 text-xs"
      >
        <component :is="row.icon" :size="11" class="text-muted-foreground/70 shrink-0" />
        <span class="truncate flex-1 text-foreground">{{ row.value }}</span>
        <!-- Per-field confidence bar -->
        <div class="flex items-center gap-1 shrink-0">
          <div class="w-8 h-1.5 rounded-full overflow-hidden bg-border">
            <div
              class="h-full rounded-full transition-all"
              :class="confidenceBarColor(row.confidence)"
              :style="{ width: Math.round(row.confidence * 100) + '%' }"
            />
          </div>
          <span class="font-mono text-[10px] text-muted-foreground w-7 text-right">{{ Math.round(row.confidence * 100) }}%</span>
        </div>
      </div>
    </div>

    <!-- "File it" CTA row -->
    <div class="flex items-center justify-end pt-1 border-t border-purple-100/60 dark:border-purple-900/30 mt-1">
      <span class="text-[10px] text-purple-600 dark:text-purple-400 font-semibold flex items-center gap-1 group-hover:underline">
        File it <ArrowRight :size="10" />
      </span>
    </div>
  </div>
</template>
