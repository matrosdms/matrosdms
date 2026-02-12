<script setup lang="ts">
import { useVueTable, getCoreRowModel, FlexRender, type ColumnDef } from '@tanstack/vue-table'
import { watch, nextTick, ref, computed } from 'vue'
import { useVirtualizer } from '@tanstack/vue-virtual'
import TableHeader from '@/components/ui/TableHeader.vue'

// ============================================================================
// TYPES
// ============================================================================

interface DataTableProps {
  data: any[]
  columns: ColumnDef<any>[]
  rowClassName?: (row: any) => string
  selectedId?: string | null
}

interface DataTableEmits {
  (e: 'row-click', row: any): void
  (e: 'row-enter', row: any): void
  (e: 'row-dblclick', row: any): void
  (e: 'row-dragstart', event: DragEvent, row: any): void
}

const props = withDefaults(defineProps<DataTableProps>(), {
  rowClassName: () => '',
  selectedId: null
})

const emit = defineEmits<DataTableEmits>()

// ============================================================================
// CONSTANTS
// ============================================================================

const ROW_HEIGHT = 36
const VIRTUALIZER_OVERSCAN = 10

const KEYBOARD_ACTIONS = {
  ArrowDown: 'next',
  ArrowUp: 'prev',
  Home: 'first',
  End: 'last',
  Enter: 'enter',
  ' ': 'enter'
} as const

// ============================================================================
// TABLE CONFIGURATION
// ============================================================================

const table = useVueTable({
  get data() { return props.data },
  get columns() { return props.columns },
  getCoreRowModel: getCoreRowModel(),
  enableColumnResizing: true,
  columnResizeMode: 'onChange',
  defaultColumn: {
    minSize: 60,
    maxSize: 800,
  },
})

// ============================================================================
// REFS & COMPUTED
// ============================================================================

const containerRef = ref<HTMLElement | null>(null)
const parentRef = ref<HTMLElement | null>(null)

const rows = computed(() => table.getRowModel().rows)

const virtualizerOptions = computed(() => ({
  count: rows.value.length,
  getScrollElement: () => parentRef.value,
  estimateSize: () => ROW_HEIGHT,
  overscan: VIRTUALIZER_OVERSCAN,
}))

const rowVirtualizer = useVirtualizer(virtualizerOptions)
const virtualRows = computed(() => rowVirtualizer.value.getVirtualItems())
const totalSize = computed(() => rowVirtualizer.value.getTotalSize())

// ============================================================================
// NAVIGATION
// ============================================================================

const getRowId = (row: any): string => row.uuid || row.id

const findRowIndex = (id: string | null): number => {
  if (!id) return -1
  return rows.value.findIndex(r => getRowId(r.original) === id)
}

const scrollToRow = (id: string) => {
  nextTick(() => {
    const index = findRowIndex(id)
    if (index !== -1) {
      rowVirtualizer.value.scrollToIndex(index, { align: 'center' })
    }
  })
}

// Ensure the row is visible and receives focus (for keyboard nav continuity)
const focusRow = (id: string) => {
    nextTick(() => {
        // First scroll to it to ensure DOM existence
        scrollToRow(id)
        
        // Wait for virtualizer to render
        setTimeout(() => {
            const el = document.getElementById(`row-${id}`)
            if (el) el.focus()
        }, 50) 
    })
}

const getNextIndex = (currentIndex: number, action: string): number => {
  const lastIndex = rows.value.length - 1
  switch (action) {
    case 'next': return currentIndex === -1 ? 0 : Math.min(currentIndex + 1, lastIndex)
    case 'prev': return currentIndex === -1 ? lastIndex : Math.max(currentIndex - 1, 0)
    case 'first': return 0
    case 'last': return lastIndex
    default: return currentIndex
  }
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (!props.data.length || e.key === 'Tab') return

  const action = KEYBOARD_ACTIONS[e.key as keyof typeof KEYBOARD_ACTIONS]
  if (!action) return 

  e.preventDefault()

  const currentIndex = findRowIndex(props.selectedId)

  if (action === 'enter') {
    if (currentIndex !== -1) emit('row-enter', rows.value[currentIndex].original)
    return
  }

  const nextIndex = getNextIndex(currentIndex, action)
  if (nextIndex !== -1 && nextIndex !== currentIndex) {
    emit('row-click', rows.value[nextIndex].original)
    // Manually focus the new row DOM element to keep focus ring consistent
    const nextId = getRowId(rows.value[nextIndex].original)
    focusRow(nextId)
  }
}

const handleRowClick = (row: any) => {
  emit('row-click', row)
}

const handleRowDrop = async (event: DragEvent, rowData: any) => {
  const rawData = event.dataTransfer?.getData('application/json')
  if (!rawData) return
  try {
    const payload = JSON.parse(rawData)
    if (payload.type === 'inbox-file') {
      const { useDmsStore } = await import('@/stores/dms')
      const dms = useDmsStore()
      dms.startItemCreation(rowData, payload)
    }
  } catch (error) { console.error(error) }
}

watch(() => props.selectedId, (newId) => { if (newId) scrollToRow(newId) })

defineExpose({ focusRow, scrollToRow })
</script>

<template>
  <div 
    ref="containerRef"
    class="w-full h-full flex flex-col bg-background text-sm outline-none group/table focus:pane-focused transition-all" 
    tabindex="0"
    @keydown="handleKeyDown"
  >
    <TableHeader :header-groups="table.getHeaderGroups()" />
    
    <div ref="parentRef" class="flex-1 w-full relative overflow-y-auto custom-scrollbar bg-background">
      <div v-if="rows.length === 0" class="absolute inset-0 p-8 text-center text-muted-foreground italic">No data available</div>

      <div v-else :style="{ height: `${totalSize}px`, width: '100%', position: 'relative' }">
        <div
          v-for="virtualRow in virtualRows"
          :key="String(virtualRow.key)"
          :data-index="virtualRow.index"
          :ref="(el) => rowVirtualizer.measureElement(el as Element)"
          :style="{ position: 'absolute', top: 0, left: 0, width: '100%', transform: `translateY(${virtualRow.start}px)` }"
        >
          <div
            :id="`row-${getRowId(rows[virtualRow.index].original)}`"
            tabindex="-1"
            draggable="true"
            class="flex w-full border-b border-border last:border-b-0 cursor-pointer 
                   transition-colors relative overflow-hidden outline-none
                   focus:z-10 focus:ring-1 focus:ring-inset focus:ring-blue-400 dark:focus:ring-blue-600
                   hover:bg-muted/50 text-foreground"
            :class="[
                rowClassName(rows[virtualRow.index].original), 
                getRowId(rows[virtualRow.index].original) === selectedId ? 'table-row-selected' : ''
            ]"
            :style="{ height: `${ROW_HEIGHT}px` }"
            @dragstart="emit('row-dragstart', $event, rows[virtualRow.index].original)"
            @click.stop="handleRowClick(rows[virtualRow.index].original)"
            @dblclick.stop="emit('row-dblclick', rows[virtualRow.index].original)"
            @dragover.prevent
            @drop.stop="handleRowDrop($event, rows[virtualRow.index].original)"
          >
            <div 
              v-for="cell in rows[virtualRow.index].getVisibleCells()" 
              :key="cell.id" 
              class="px-3 py-2 border-r border-border last:border-r-0 overflow-hidden flex items-start truncate" 
              :style="{ width: `${cell.column.getSize()}px` }"
            >
              <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>