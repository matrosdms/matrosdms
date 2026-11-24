<script setup lang="ts">
/**
 * The latest documents of the context a new one is about to be filed in, newest issue date first:
 * the predecessors to compare it with ("Bezügemitteilung 2026-05, 2026-04, …"). Follows the target
 * context, so after picking another context the list shows what lies there. Collapsible; whether it
 * is open is remembered per browser.
 */
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { useStorage } from '@vueuse/core'
import { ChevronDown, ChevronRight, Columns, FileText, Loader2 } from 'lucide-vue-next'
import { ItemService } from '@/services/ItemService'
import { parseBackendDate } from '@/lib/utils'
import type { Item } from '@/types/models'

const COUNT = 6

const props = defineProps<{
  contextUuid: string | null
  contextName: string
  comparedUuid: string | null
}>()

const emit = defineEmits<{
  (e: 'compare', item: Item): void
}>()

const open = useStorage('matros-add-form-previously-filed-open', true)

const { data: items, isLoading, isError } = useQuery({
  queryKey: computed(() => ['context-latest', props.contextUuid, COUNT]),
  queryFn: () => ItemService.getLatestInContext(props.contextUuid as string, COUNT),
  enabled: computed(() => !!props.contextUuid),
  staleTime: 30_000,
})

const formatDate = (val: unknown) => {
  const d = parseBackendDate(val)
  return d ? d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' }) : '—'
}
const kindOf = (item: Item) => item.kindList?.[0]?.name || ''
</script>

<template>
  <section class="rounded-lg border border-border bg-muted/20">
    <button
      type="button"
      class="w-full flex items-center gap-2 px-3 py-2 text-left text-xs font-bold uppercase tracking-wider text-muted-foreground hover:text-foreground transition-colors"
      :aria-expanded="open"
      @click="open = !open"
    >
      <ChevronDown v-if="open" :size="14" class="shrink-0" />
      <ChevronRight v-else :size="14" class="shrink-0" />
      <span class="truncate">Previously filed in {{ contextName }}</span>
      <span v-if="items?.length" class="ml-auto font-mono font-normal normal-case">{{ items.length }}</span>
    </button>

    <div v-if="open" class="border-t border-border/60">
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
      <ul v-else class="divide-y divide-border/60">
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
  </section>
</template>
