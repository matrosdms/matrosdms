<script setup lang="ts">
/**
 * The latest documents of the context a new one is about to be filed in, newest issue date first:
 * the predecessors to compare it with ("Bezügemitteilung 2026-05, 2026-04, …"). Shown as the
 * dropdown of the context chip in the add form's header; the form loads the list.
 */
import { Columns, FileText, Loader2 } from 'lucide-vue-next'
import { formatDate } from '@/lib/utils'
import type { Item } from '@/types/models'

defineProps<{
  contextUuid: string | null
  contextName: string
  items: Item[] | undefined
  isLoading: boolean
  isError: boolean
  comparedUuid: string | null
}>()

const emit = defineEmits<{
  (e: 'compare', item: Item): void
}>()

const kindOf = (item: Item) => item.kindList?.[0]?.name || ''
</script>

<template>
  <div class="rounded-lg border border-border bg-background shadow-xl overflow-hidden">
    <div class="px-3 py-2 text-[10px] font-bold uppercase tracking-wider text-muted-foreground border-b border-border/60 truncate">
      Previously filed in {{ contextName }}
    </div>
    <div v-if="!contextUuid" class="px-3 py-3 text-xs text-muted-foreground italic">
      Choose a context to see what was filed there.
    </div>
    <div v-else-if="isLoading" class="px-3 py-3 text-xs text-muted-foreground flex items-center gap-2">
      <Loader2 :size="12" class="animate-spin" /> Loading…
    </div>
    <div v-else-if="isError" class="px-3 py-3 text-xs text-red-600 dark:text-red-400">
      Could not load the documents of this context.
    </div>
    <div v-else-if="!items?.length" class="px-3 py-3 text-xs text-muted-foreground italic">
      Nothing filed here yet.
    </div>
    <ul v-else class="divide-y divide-border/60 max-h-[60vh] overflow-y-auto">
      <li v-for="item in items" :key="item.uuid">
        <button
          type="button"
          class="w-full flex items-center gap-2 px-3 py-1.5 text-left text-xs transition-colors"
          :class="item.uuid === comparedUuid
            ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
            : 'hover:bg-muted/60 text-foreground'"
          :title="item.uuid === comparedUuid ? 'Shown next to the new document' : 'Compare with the new document'"
          @click="emit('compare', item)"
        >
          <FileText :size="12" class="shrink-0 text-muted-foreground" />
          <span class="font-mono text-muted-foreground shrink-0">{{ formatDate(item.issueDate) }}</span>
          <span class="truncate flex-1">{{ item.name }}</span>
          <span v-if="kindOf(item)" class="shrink-0 text-[10px] text-muted-foreground">{{ kindOf(item) }}</span>
          <Columns :size="12" class="shrink-0" :class="item.uuid === comparedUuid ? 'opacity-100' : 'opacity-40'" />
        </button>
      </li>
    </ul>
  </div>
</template>
