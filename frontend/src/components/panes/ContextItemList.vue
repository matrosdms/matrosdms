<script setup lang="ts">
import { computed, h, ref, watch } from 'vue'
import { useStorage } from '@vueuse/core'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import DataTable from '@/components/ui/DataTable.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
import StandardDetailPane from '@/components/panes/StandardDetailPane.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ContextFlow from '@/components/visualizations/ContextFlow.vue'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { useWorkflowStore } from '@/stores/workflow'
import { useItemQueries } from '@/composables/queries/useItemQueries'
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { useAdminQueries } from '@/composables/queries/useAdminQueries'
import { useDragDrop } from '@/composables/useDragDrop'
import { ItemService } from '@/services/ItemService'
import {
  Folder, Loader2, PlusCircle, Pencil, Trash2, Box, Calendar,
  Tag, Activity, CheckSquare, FolderOpen, Eye, List, FileText,
  ArrowLeft, Maximize2, Minimize2, Sidebar, GitCommit, RotateCcw
} from 'lucide-vue-next'
import { push } from 'notivue'
import { EStage, EStageLabels, type EStageType, ERootCategory, ViewMode, EArchiveFilter } from '@/enums'
import type { Item } from '@/types/models'
import type { ColumnDef } from '@tanstack/vue-table'
import { useQuery } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'

// ============================================================================
// TYPES
// ============================================================================

type ViewModeType = 'table' | 'timeline'

interface ContextZoomProps {
  layout?: number[]
}

interface ContextZoomEmits {
  (e: 'update:layout', layout: number[]): void
}

interface PaneResize {
  size: number
}

// ============================================================================
// PROPS & EMITS
// ============================================================================

const props = defineProps<ContextZoomProps>()
const emit = defineEmits<ContextZoomEmits>()

// ============================================================================
// STORES & COMPOSABLES
// ============================================================================

const dms = useDmsStore()
const ui = useUIStore()
const workflow = useWorkflowStore()
// We use direct query construction here to handle archiveState reactively
const { useCategoryTree } = useContextQueries()
const { useStores } = useAdminQueries()
const { handleDropOnContext, startDrag } = useDragDrop()

// ============================================================================
// CONSTANTS
// ============================================================================

const COLUMN_SIZES = {
  DATE: { size: 120, minSize: 80, maxSize: 150 },
  TYPE: { size: 150, minSize: 100, maxSize: 200 },
  NAME: { size: 300, minSize: 200, maxSize: 500 },
  STORE: { size: 120, minSize: 80, maxSize: 180 },
  STAGE: { size: 100, minSize: 80, maxSize: 120 },
} as const

const SPLIT_LAYOUT = {
  defaultList: 35,
  defaultDetail: 65,
  minSize: 20,
  maxSize: 100,
} as const

const STAGE_ROW_CLASSES: Record<string, string> = {
  [EStage.ACTIVE]: 'table-row-active',
  [EStage.CLOSED]: 'table-row-closed opacity-70 italic'
}

const REFETCH_DELAY = 500

// ============================================================================
// STATE
// ============================================================================

const searchQuery = ref('')
const isDragOver = ref(false)
const showProperties = ref(false)
const isMaximized = ref(false)
const viewMode = useStorage<ViewModeType>('matros-ctx-view-mode', 'table')

// ============================================================================
// QUERIES
// ============================================================================

const selectedContextId = computed(() => dms.selectedContext?.uuid)
const archiveState = computed(() => dms.archiveViewMode)

// Custom Query Implementation to support reactive archiveState
const { data: items, isLoading, refetch } = useQuery({
    queryKey: computed(() => [
        ...queryKeys.items.byContext(selectedContextId.value || 'null'), 
        archiveState.value
    ]),
    queryFn: async () => {
        if (!selectedContextId.value) return []
        return await ItemService.getByContext(selectedContextId.value, archiveState.value)
    },
    enabled: computed(() => !!selectedContextId.value)
})

const { data: stores } = useStores()
const { data: kindTree } = useCategoryTree(ERootCategory.KIND)

// ============================================================================
// COMPUTED - MAPS & DATA
// ============================================================================

const isSplitMode = computed(() => ui.isLayoutZoomed)
const isTrashMode = computed(() => dms.archiveViewMode === EArchiveFilter.ARCHIVED_ONLY)

const storeMap = computed(() => {
  const map = new Map<string, string>()
  if (!stores.value) return map
  stores.value.forEach((store: any) => map.set(store.uuid, store.shortname || store.name))
  return map
})

const typeMap = computed(() => {
  const map = new Map<string, string>()
  const root = kindTree.value
  if (!root) return map
  const traverse = (nodes: any[]) => {
    nodes.forEach(node => {
      map.set(node.uuid, node.name)
      if (node.children) traverse(node.children)
    })
  }
  if (root.children) traverse(root.children)
  return map
})

const displayItems = computed(() => {
  const raw = items.value || []
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return raw
  return raw.filter(item => (item.name || '').toLowerCase().includes(query))
})

// ============================================================================
// AUTO-SELECT LOGIC
// ============================================================================

watch([displayItems, selectedContextId, viewMode], ([list, ctxId, mode], [oldList, oldCtxId, oldMode]) => {
  if (!list || list.length === 0 || !ctxId) return
  const contextChanged = ctxId !== oldCtxId
  const viewModeChanged = mode !== oldMode
  const itemsJustLoaded = (!oldList || oldList.length === 0) && list.length > 0
  
  if (contextChanged || itemsJustLoaded || viewModeChanged) {
    dms.setSelectedItem(list[0])
  }
}, { immediate: true })

// ============================================================================
// ACTIONS
// ============================================================================

const goBackToFolders = () => ui.setZoom(false)
const toggleZoom = () => ui.setZoom(!ui.isLayoutZoomed)
const toggleMaximize = () => {
  isMaximized.value = !isMaximized.value
  if (isMaximized.value) ui.setZoom(true)
}
const toggleProperties = () => showProperties.value = !showProperties.value

// Unified Open Logic
const openDocument = async (uuid: string, name: string) => {
  // 1. Select
  const item = displayItems.value.find(i => i.uuid === uuid)
  if (item) dms.setSelectedItem(item)
  
  // 2. Navigate if needed
  if (!isSplitMode.value) {
    ui.setRightPanel(ViewMode.PREVIEW, { id: uuid, name, source: 'item' })
  }
}

const editItem = () => {
  if (!dms.selectedItem) return
  dms.startItemEditing()
}

const deleteItem = () => {
  if (!dms.selectedItem) return
  dms.startItemArchiving() // Standard Soft Delete flow
}

const destroyItem = async () => {
    if (!dms.selectedItem?.uuid) return
    if (!confirm(`PERMANENTLY DESTROY '${dms.selectedItem.name}'? This cannot be undone.`)) return
    
    try {
        await ItemService.destroy(dms.selectedItem.uuid)
        push.success('Item destroyed permanently')
        refetch()
        dms.setSelectedItem(null)
    } catch(e: any) {
        push.error(e.message)
    }
}

const restoreItem = async () => {
    if (!dms.selectedItem?.uuid) return
    try {
        await ItemService.restore(dms.selectedItem.uuid)
        push.success('Item restored to active list')
        refetch()
        dms.setSelectedItem(null)
    } catch(e: any) {
        push.error(e.message)
    }
}

const addItemAction = () => {
  if (!dms.selectedItem?.uuid) return
  workflow.startActionCreation({ itemId: dms.selectedItem.uuid })
}

// ============================================================================
// DRAG & DROP & EVENTS
// ============================================================================

const handleDragEnter = (event: DragEvent) => {
  if (event.dataTransfer?.types.includes('Files') || (dms.isDraggingGlobal && dms.currentDragType === 'inbox-file')) {
    isDragOver.value = true
  }
}
const handleDragLeave = () => isDragOver.value = false
const handleDrop = (event: DragEvent) => {
  isDragOver.value = false
  handleDropOnContext(event, dms.selectedContext)
  setTimeout(() => refetch(), REFETCH_DELAY)
}
const handleRowDragStart = (event: DragEvent, row: any) => startDrag(event, 'dms-item', row)
const handleResize = (panes: PaneResize[]) => emit('update:layout', panes.map(p => p.size))

// --- UPDATED ROW HANDLER ---
const handleRowClick = (item: Item) => {
    // Single click now opens the preview/document (unifying behavior with clicking name)
    // If we are in List mode, this switches to Preview mode.
    // If we are in Split mode, this simply selects it (and right pane updates).
    if (item.uuid) openDocument(item.uuid, item.name || 'Document')
}

const handleRowDblClick = (item: Item) => { if (item?.uuid) ItemService.openDocument(item.uuid) }

// --- FIXED: Timeline click now opens document ---
const handleTimelineSelect = (item: any) => { 
    if (item?.uuid) openDocument(item.uuid, item.name || 'Document')
}

// ============================================================================
// RENDER HELPERS
// ============================================================================

const getRowClass = (row: any): string => {
  if (dms.selectedItem?.uuid === row.uuid) return '!border-l-primary bg-primary/10 dark:text-foreground font-medium'
  return STAGE_ROW_CLASSES[row.stage] || STAGE_ROW_CLASSES[EStage.ACTIVE]
}

const createHeaderIcon = (icon: any, label: string) => h('div', { class: 'flex items-center gap-2 text-xs uppercase tracking-wider text-muted-foreground font-bold' }, [h(icon, { size: 14, class: 'text-primary' }), h('span', label)])

const createNameCell = (info: any) => {
  const uuid = info.row.original.uuid
  const name = info.getValue()
  
  const handleClick = (e: Event) => {
    e.stopPropagation()
    openDocument(uuid, name)
  }
  
  return h('div', { class: 'flex items-center gap-2 max-w-full group/cell' }, [
    h('span', {
      class: 'font-medium text-foreground hover:underline text-primary cursor-pointer truncate',
      onClick: handleClick
    }, name),
    h(Eye, {
      size: 14,
      class: 'text-muted-foreground hover:text-primary cursor-pointer opacity-0 group-hover/cell:opacity-100',
      onClick: handleClick
    })
  ])
}

const columns: ColumnDef<any>[] = [
  { accessorKey: 'issueDate', header: () => createHeaderIcon(Calendar, 'Date'), ...COLUMN_SIZES.DATE, cell: (info) => h('span', { class: 'text-xs text-muted-foreground font-mono' }, new Date(info.getValue()).toLocaleDateString()) },
  { accessorKey: 'kindList', header: () => createHeaderIcon(Tag, 'Type'), ...COLUMN_SIZES.TYPE, cell: (info) => h('span', { class: 'text-xs truncate' }, info.getValue()?.[0]?.name || '-') },
  { accessorKey: 'name', header: () => createHeaderIcon(FileText, 'Name'), ...COLUMN_SIZES.NAME, cell: (info) => createNameCell(info) },
  { accessorKey: 'storeIdentifier', header: () => createHeaderIcon(Box, 'Store'), ...COLUMN_SIZES.STORE, cell: (info) => h('span', { class: 'text-xs text-muted-foreground truncate' }, storeMap.value.get(info.getValue()) || '-') },
  { accessorKey: 'stage', header: () => createHeaderIcon(Activity, 'Stage'), ...COLUMN_SIZES.STAGE, cell: (info) => h('span', { class: 'text-xs' }, info.getValue()) }
]

const listPaneSize = computed(() => !props.layout ? (dms.selectedItem ? SPLIT_LAYOUT.defaultList : SPLIT_LAYOUT.maxSize) : props.layout[0])
const detailPaneSize = computed(() => isMaximized.value ? SPLIT_LAYOUT.maxSize : (!props.layout ? SPLIT_LAYOUT.defaultDetail : props.layout[1]))
</script>

<template>
  <div class="h-full flex flex-col relative bg-background transition-colors" @dragover.prevent="handleDragEnter" @dragleave.prevent="handleDragLeave" @drop.prevent="handleDrop">
    
    <!-- Drag Overlay -->
    <div v-if="isDragOver && dms.selectedContext" class="absolute inset-0 bg-blue-50/90 dark:bg-blue-900/80 z-50 flex items-center justify-center border-4 border-blue-400 border-dashed m-2 rounded-lg pointer-events-none">
      <div class="flex flex-col items-center text-blue-600 dark:text-blue-300 animate-pulse"><PlusCircle :size="48" /><span class="text-lg font-bold mt-2">Move to {{ dms.selectedContext.name }}</span></div>
    </div>

    <!-- Toolbar -->
    <div class="px-3 py-2 flex justify-between items-center border-b border-border h-[40px] select-none shrink-0 transition-colors" :class="isTrashMode ? 'bg-red-50 dark:bg-red-900/10' : 'bg-muted/30'">
      <div class="flex items-center gap-2 overflow-hidden">
        <div class="flex bg-background rounded-md p-0.5 border border-border shrink-0 mr-2">
          <button class="p-1 rounded transition-all" :class="viewMode === 'table' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" @click="viewMode = 'table'"><List :size="14" /></button>
          <button class="p-1 rounded transition-all" :class="viewMode === 'timeline' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" @click="viewMode = 'timeline'"><GitCommit :size="14" class="rotate-90" /></button>
        </div>
        <div class="w-px h-3 bg-border mr-2" />
        <BaseButton v-if="dms.selectedContext && isSplitMode" variant="ghost" size="iconSm" @click="goBackToFolders"><ArrowLeft :size="16" /></BaseButton>
        <BaseButton v-if="dms.selectedContext" variant="ghost" size="iconSm" @click="toggleZoom"><component :is="isSplitMode ? Minimize2 : Maximize2" :size="16" /></BaseButton>
        
        <div class="flex items-center gap-2 overflow-hidden px-1">
          <span class="text-[13px] font-bold text-foreground truncate">{{ dms.selectedContext ? dms.selectedContext.name : 'All Items' }}</span>
          <span v-if="isTrashMode" class="text-[10px] bg-red-100 text-red-700 px-1.5 py-0.5 rounded font-bold uppercase">Trash Bin</span>
        </div>
      </div>

      <div class="flex items-center gap-1 ml-auto">
        <!-- Trash Toggle -->
        <BaseButton 
            variant="ghost" 
            size="iconSm" 
            @click="dms.toggleArchiveView" 
            :class="isTrashMode ? 'text-red-600 bg-red-100 dark:bg-red-900/30' : 'text-muted-foreground hover:text-red-600'" 
            :title="isTrashMode ? 'Show Active Items' : 'Show Trash / Archived Items'"
        >
            <Trash2 :size="14" />
        </BaseButton>

        <div class="w-px h-3 bg-border mx-1"></div>

        <template v-if="isTrashMode">
             <BaseButton variant="ghost" size="sm" class="text-green-600 hover:bg-green-50" :disabled="!dms.selectedItem" @click="restoreItem">
                <RotateCcw :size="14" class="mr-1"/> Restore
             </BaseButton>
             <BaseButton variant="ghost" size="sm" class="text-red-600 hover:bg-red-50" :disabled="!dms.selectedItem" @click="destroyItem">
                <Trash2 :size="14" class="mr-1"/> Destroy
             </BaseButton>
        </template>
        
        <template v-else>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedItem" @click="editItem"><Pencil :size="14" /></BaseButton>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedItem" @click="addItemAction"><CheckSquare :size="14" /></BaseButton>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedItem" class="hover:text-destructive" @click="deleteItem"><Trash2 :size="14" /></BaseButton>
        </template>
      </div>
    </div>

    <!-- Search Bar -->
    <div v-if="selectedContextId" class="p-2 border-b border-border bg-background">
      <SearchInput v-model="searchQuery" placeholder="Filter items..." />
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-hidden relative">
      <div v-if="!selectedContextId" class="h-full flex flex-col items-center justify-center text-muted-foreground opacity-60">
        <Folder :size="48" class="mb-2" />
        <span class="text-sm">Select a Context from the left</span>
      </div>
      <div v-else-if="isLoading" class="flex h-full items-center justify-center"><Loader2 class="animate-spin text-primary" /></div>
      <EmptyState v-else-if="displayItems.length === 0" :icon="FolderOpen" title="Empty Folder" description="No items found." />
      
      <splitpanes v-else-if="isSplitMode" class="default-theme" @resized="handleResize">
        <pane v-if="!isMaximized" :size="listPaneSize" :min-size="SPLIT_LAYOUT.minSize">
          <div class="h-full overflow-y-auto custom-scrollbar bg-background">
            <ContextFlow v-if="viewMode === 'timeline'" :items="displayItems" @select="handleTimelineSelect" />
            <DataTable v-else :data="displayItems" :columns="columns" :row-class-name="getRowClass" :selected-id="dms.selectedItem?.uuid" @row-click="handleRowClick" @row-dblclick="handleRowDblClick" @row-dragstart="handleRowDragStart" />
          </div>
        </pane>
        <pane v-if="dms.selectedItem" :size="detailPaneSize" min-size="30" class="bg-muted/10 border-l border-border relative group/pane">
          <div class="flex h-full w-full relative">
            <div class="flex-1 h-full relative flex flex-col">
              <DocumentPreview :identifier="dms.selectedItem?.uuid || ''" source="item" :file-name="dms.selectedItem?.name" />
              <div class="absolute top-2 right-2 z-20 flex gap-1 transition-opacity opacity-0 group-hover/pane:opacity-100">
                <button class="p-1.5 bg-background/80 backdrop-blur-sm rounded-md shadow-sm border border-border hover:text-primary transition-colors" @click="toggleMaximize"><component :is="isMaximized ? Minimize2 : Maximize2" :size="16" /></button>
                <button class="p-1.5 bg-background/80 backdrop-blur-sm rounded-md shadow-sm border border-border hover:text-primary transition-colors" @click="toggleProperties"><Sidebar :size="16" /></button>
              </div>
            </div>
            <div v-if="showProperties" class="w-80 h-full border-l border-border bg-background flex-shrink-0 animate-in slide-in-from-right duration-200">
              <StandardDetailPane :item="dms.selectedItem" @edit="editItem" @delete="isTrashMode ? destroyItem() : deleteItem()" />
            </div>
          </div>
        </pane>
      </splitpanes>

      <div v-else class="h-full overflow-y-auto custom-scrollbar bg-background">
        <ContextFlow v-if="viewMode === 'timeline'" :items="displayItems" @select="handleTimelineSelect" />
        <DataTable v-else :data="displayItems" :columns="columns" :row-class-name="getRowClass" :selected-id="dms.selectedItem?.uuid" @row-click="handleRowClick" @row-dblclick="handleRowDblClick" @row-dragstart="handleRowDragStart" />
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.splitpanes__splitter) { @apply bg-transparent hover:bg-blue-500/10 transition-colors w-[6px] !important; border-left: 1px solid theme('colors.border'); margin-left: -1px; position: relative; z-index: 20; }
:deep(.splitpanes__splitter:hover) { border-left-color: theme('colors.primary.DEFAULT'); }
</style>