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
  Folder, Loader2, PlusCircle, Pencil, Archive, Box, Calendar,
  Tag, Activity, CheckSquare, FolderOpen, Eye, List, FileText,
  ArrowLeft, Maximize2, Minimize2, Sidebar, GitCommit
} from 'lucide-vue-next'
import { push } from 'notivue'
import { EStage, EStageLabels, type EStageType, ERootCategory, ViewMode } from '@/enums'
import type { Item } from '@/types/models'
import type { ColumnDef } from '@tanstack/vue-table'

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
const { useItemsForContext } = useItemQueries()
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

const { data: items, isLoading, refetch } = useItemsForContext(selectedContextId)
const { data: stores } = useStores()
const { data: kindTree } = useCategoryTree(ERootCategory.KIND)

// ============================================================================
// COMPUTED - MAPS & DATA
// ============================================================================

const isSplitMode = computed(() => ui.isLayoutZoomed)

const storeMap = computed(() => {
  const map = new Map<string, string>()
  if (!stores.value) return map
  
  stores.value.forEach((store: any) => {
    map.set(store.uuid, store.shortname || store.name)
  })
  
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
  
  return raw.filter(item => 
    (item.name || '').toLowerCase().includes(query)
  )
})

// ============================================================================
// AUTO-SELECT LOGIC
// ============================================================================

// Auto-select first item when context changes, view changes, or items load
watch([displayItems, selectedContextId, viewMode], ([list, ctxId, mode], [oldList, oldCtxId, oldMode]) => {
  // Skip if no items or no context
  if (!list || list.length === 0 || !ctxId) return
  
  // Determine if we should auto-select
  const contextChanged = ctxId !== oldCtxId
  const viewModeChanged = mode !== oldMode
  const itemsJustLoaded = (!oldList || oldList.length === 0) && list.length > 0
  
  // Auto-select first item on context switch or when items first load
  if (contextChanged || itemsJustLoaded || viewModeChanged) {
    dms.setSelectedItem(list[0])
  }
}, { immediate: true })

// ============================================================================
// NAVIGATION ACTIONS
// ============================================================================

const goBackToFolders = () => {
  ui.setZoom(false)
}

const toggleZoom = () => {
  ui.setZoom(!ui.isLayoutZoomed)
}

const toggleMaximize = () => {
  isMaximized.value = !isMaximized.value
  if (isMaximized.value) {
    ui.setZoom(true)
  }
}

const toggleProperties = () => {
  showProperties.value = !showProperties.value
}

// ============================================================================
// ITEM ACTIONS
// ============================================================================

const openDocument = async (uuid: string, name: string) => {
  const item = displayItems.value.find(i => i.uuid === uuid)
  if (item) dms.setSelectedItem(item)
  
  if (!isSplitMode.value) {
    ui.setRightPanel(ViewMode.PREVIEW, { id: uuid, name, source: 'item' })
  }
}

const handleRowClick = (item: Item) => {
  dms.setSelectedItem(item)
}

const handleRowDblClick = (item: Item) => {
  if (!item?.uuid) return
  
  ItemService.openDocument(item.uuid).catch(error => {
    push.error(error.message)
  })
}

const handleTimelineSelect = (item: any) => {
  if (item?.uuid) {
    openDocument(item.uuid, item.name)
  }
}

// ============================================================================
// EDIT ACTIONS
// ============================================================================

const editItem = () => {
  if (!dms.selectedItem) {
    push.warning("Please select a document")
    return
  }
  dms.startItemEditing()
}

const archiveItem = () => {
  if (!dms.selectedItem) {
    push.warning("Please select a document")
    return
  }
  dms.startItemArchiving()
}

const addItemAction = () => {
  if (!dms.selectedItem?.uuid) {
    push.warning("Please select a document")
    return
  }
  workflow.startActionCreation({ itemId: dms.selectedItem.uuid })
}

// ============================================================================
// DRAG & DROP
// ============================================================================

const handleDragEnter = (event: DragEvent) => {
  // 1. External Files: Always allow
  if (event.dataTransfer?.types.includes('Files')) {
    isDragOver.value = true
    return
  }

  // 2. Internal Drags: Check source type via Store
  if (dms.isDraggingGlobal) {
      // Only show overlay if dragging from Inbox or Stack
      if (dms.currentDragType === 'inbox-file' || dms.currentDragType === 'dms-batch') {
          isDragOver.value = true
      }
      // 'dms-item' (internal list drag) will NOT trigger the overlay
  }
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = (event: DragEvent) => {
  isDragOver.value = false
  
  // Process drop even if items are loading/processing
  // This allows recovery from backend errors
  handleDropOnContext(event, dms.selectedContext)
  
  setTimeout(() => {
    refetch()
  }, REFETCH_DELAY)
}

const handleRowDragStart = (event: DragEvent, row: any) => {
  startDrag(event, 'dms-item', row)
}

// ============================================================================
// SPLITPANE RESIZE
// ============================================================================

const handleResize = (panes: PaneResize[]) => {
  emit('update:layout', panes.map(p => p.size))
}

// ============================================================================
// UTILITIES
// ============================================================================

const parseDate = (value: any): Date | null => {
  if (!value) return null

  try {
    if (Array.isArray(value)) {
      return new Date(value[0], value[1] - 1, value[2])
    }
    
    const date = new Date(value)
    return isNaN(date.getTime()) ? null : date
  } catch {
    return null
  }
}

// ============================================================================
// COLUMN HELPERS
// ============================================================================

const createHeaderIcon = (icon: any, label: string) => {
  return h('div', {
    class: 'flex items-center gap-2 text-xs uppercase tracking-wider text-muted-foreground font-bold'
  }, [
    h(icon, { size: 14, class: 'text-primary' }),
    h('span', label)
  ])
}

const createDateCell = (value: any) => {
  const date = parseDate(value)
  
  if (!date) {
    return h('span', { class: 'text-muted-foreground/50' }, '-')
  }
  
  return h('div', {
    class: 'flex items-center gap-1.5 text-xs text-muted-foreground font-mono'
  }, [
    h(Calendar, { size: 12 }),
    date.toLocaleDateString('de-DE')
  ])
}

const createTypeCell = (kindList: any) => {
  if (!kindList?.length) {
    return h('span', { class: 'text-muted-foreground/50' }, '-')
  }
  
  const typeName = typeMap.value.get(kindList[0].uuid) || kindList[0].name
  
  return h('div', { class: 'flex items-center gap-1.5' }, [
    h(Tag, { size: 12, class: 'text-purple-400' }),
    h('span', { class: 'text-xs text-foreground truncate' }, typeName)
  ])
}

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

const createStoreCell = (info: any) => {
  const storeId = info.getValue()
  const storeNumber = info.row.original.storeItemNumber
  
  if (!storeId) {
    return h('span', { class: 'text-muted-foreground/50' }, '-')
  }
  
  const storeName = storeMap.value.get(storeId) || 'Unknown'
  const displayText = `${storeName}${storeNumber ? ' #' + storeNumber : ''}`
  
  return h('div', {
    class: 'text-xs text-muted-foreground flex items-center gap-1.5'
  }, [
    h(Box, { size: 12 }),
    h('span', { class: 'truncate', title: storeName }, displayText)
  ])
}

const createStageCell = (value: any) => {
  const stageValue = (value || EStage.ACTIVE) as EStageType
  const label = EStageLabels[stageValue] || stageValue
  
  return h('div', {
    class: 'flex items-center justify-end gap-1.5 text-xs text-muted-foreground'
  }, [h('span', label)])
}

// ============================================================================
// COLUMN DEFINITIONS
// ============================================================================

const columns: ColumnDef<any>[] = [
  {
    accessorKey: 'issueDate',
    header: () => createHeaderIcon(Calendar, 'Date'),
    ...COLUMN_SIZES.DATE,
    cell: (info) => createDateCell(info.getValue())
  },
  {
    accessorKey: 'kindList',
    header: () => createHeaderIcon(Tag, 'Type'),
    ...COLUMN_SIZES.TYPE,
    cell: (info) => createTypeCell(info.getValue())
  },
  {
    accessorKey: 'name',
    header: () => createHeaderIcon(FileText, 'Name'),
    ...COLUMN_SIZES.NAME,
    cell: (info) => createNameCell(info)
  },
  {
    accessorKey: 'storeIdentifier',
    header: () => createHeaderIcon(Box, 'Store'),
    ...COLUMN_SIZES.STORE,
    cell: (info) => createStoreCell(info)
  },
  {
    accessorKey: 'stage',
    header: () => createHeaderIcon(Activity, 'Stage'),
    ...COLUMN_SIZES.STAGE,
    cell: (info) => createStageCell(info.getValue())
  }
]

// ============================================================================
// ROW STYLING
// ============================================================================

const getRowClass = (row: any): string => {
  return dms.selectedItem?.uuid === row.uuid
    ? '!border-l-primary bg-primary/10 dark:text-foreground'
    : 'bg-background'
}

// ============================================================================
// COMPUTED - LAYOUT
// ============================================================================

const listPaneSize = computed(() => {
  if (!props.layout) {
    return dms.selectedItem ? SPLIT_LAYOUT.defaultList : SPLIT_LAYOUT.maxSize
  }
  return props.layout[0]
})

const detailPaneSize = computed(() => {
  if (isMaximized.value) return SPLIT_LAYOUT.maxSize
  if (!props.layout) return SPLIT_LAYOUT.defaultDetail
  return props.layout[1]
})
</script>

<template>
  <div
    class="h-full flex flex-col relative bg-background transition-colors"
    @dragover.prevent="handleDragEnter"
    @dragleave.prevent="handleDragLeave"
    @drop.prevent="handleDrop"
  >
    <!-- Drag Overlay -->
    <div
      v-if="isDragOver && dms.selectedContext"
      class="absolute inset-0 bg-blue-50/90 dark:bg-blue-900/80 z-50 
             flex items-center justify-center border-4 border-blue-400 
             border-dashed m-2 rounded-lg pointer-events-none"
    >
      <div class="flex flex-col items-center text-blue-600 dark:text-blue-300 animate-pulse">
        <PlusCircle :size="48" />
        <span class="text-lg font-bold mt-2">
          Move to {{ dms.selectedContext.name }}
        </span>
        <span v-if="isLoading" class="text-sm mt-1 opacity-75">
          (Processing in progress - drop allowed for recovery)
        </span>
      </div>
    </div>

    <!-- Toolbar -->
    <div
      class="px-3 py-2 flex justify-between items-center border-b border-border 
             bg-muted/30 h-[40px] select-none shrink-0 transition-colors"
    >
      <div class="flex items-center gap-2 overflow-hidden">
        <!-- View Mode Toggle -->
        <div class="flex bg-background rounded-md p-0.5 border border-border shrink-0 mr-2">
          <button
            class="p-1 rounded transition-all"
            :class="viewMode === 'table' 
              ? 'bg-primary/10 shadow-sm text-primary' 
              : 'text-muted-foreground hover:text-foreground'"
            title="Table View"
            @click="viewMode = 'table'"
          >
            <List :size="14" />
          </button>
          <button
            class="p-1 rounded transition-all"
            :class="viewMode === 'timeline' 
              ? 'bg-primary/10 shadow-sm text-primary' 
              : 'text-muted-foreground hover:text-foreground'"
            title="Timeline Flow"
            @click="viewMode = 'timeline'"
          >
            <GitCommit :size="14" class="rotate-90" />
          </button>
        </div>

        <div class="w-px h-3 bg-border mr-2" />

        <!-- Navigation Buttons -->
        <BaseButton
          v-if="dms.selectedContext && isSplitMode"
          variant="ghost"
          size="iconSm"
          title="Back to Context List"
          @click="goBackToFolders"
        >
          <ArrowLeft :size="16" />
        </BaseButton>

        <BaseButton
          v-if="dms.selectedContext"
          variant="ghost"
          size="iconSm"
          :title="isSplitMode ? 'Restore List' : 'Maximize'"
          @click="toggleZoom"
        >
          <Minimize2 v-if="isSplitMode" :size="16" />
          <Maximize2 v-else :size="16" />
        </BaseButton>

        <!-- Context Info -->
        <div class="flex items-center gap-2 overflow-hidden px-1">
          <span class="text-[13px] font-bold text-foreground truncate" :title="dms.selectedContext?.name">
            {{ dms.selectedContext ? dms.selectedContext.name : 'All Items' }}
          </span>
          <span v-if="dms.selectedContext" class="text-[10px] text-muted-foreground font-mono">
            {{ displayItems.length }}
          </span>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex items-center gap-1 ml-auto">
        <BaseButton
          variant="ghost"
          size="iconSm"
          :disabled="!dms.selectedItem"
          title="Edit"
          @click="editItem"
        >
          <Pencil :size="14" />
        </BaseButton>
        
        <BaseButton
          variant="ghost"
          size="iconSm"
          :disabled="!dms.selectedItem"
          title="Add Task"
          @click="addItemAction"
        >
          <CheckSquare :size="14" />
        </BaseButton>
        
        <BaseButton
          variant="ghost"
          size="iconSm"
          :disabled="!dms.selectedItem"
          class="hover:text-destructive"
          title="Archive"
          @click="archiveItem"
        >
          <Archive :size="14" />
        </BaseButton>
      </div>
    </div>

    <!-- Search Bar -->
    <div v-if="selectedContextId" class="p-2 border-b border-border bg-background">
      <SearchInput v-model="searchQuery" placeholder="Filter items..." />
    </div>

    <!-- Main Content Area -->
    <div class="flex-1 overflow-hidden relative">
      <!-- No Context Selected -->
      <div
        v-if="!selectedContextId"
        class="h-full flex flex-col items-center justify-center text-muted-foreground opacity-60"
      >
        <Folder :size="48" class="mb-2" />
        <span class="text-sm">Select a Context from the left</span>
      </div>

      <!-- Loading State -->
      <div v-else-if="isLoading" class="flex h-full items-center justify-center">
        <Loader2 class="animate-spin text-primary" />
      </div>

      <!-- Empty State -->
      <EmptyState
        v-else-if="displayItems.length === 0"
        :icon="FolderOpen"
        title="Empty Folder"
        description="Drag files from Inbox here."
      />

      <!-- Split View Mode -->
      <splitpanes
        v-else-if="isSplitMode"
        class="default-theme"
        @resized="handleResize"
      >
        <!-- List Pane -->
        <pane
          v-if="!isMaximized"
          :size="listPaneSize"
          :min-size="SPLIT_LAYOUT.minSize"
        >
          <div class="h-full overflow-y-auto custom-scrollbar bg-background">
            <ContextFlow
              v-if="viewMode === 'timeline'"
              :items="displayItems"
              @select="handleTimelineSelect"
            />
            <DataTable
              v-else
              :data="displayItems"
              :columns="columns"
              :row-class-name="getRowClass"
              :selected-id="dms.selectedItem?.uuid"
              @row-click="handleRowClick"
              @row-dblclick="handleRowDblClick"
              @row-dragstart="handleRowDragStart"
            />
          </div>
        </pane>

        <!-- Detail Pane -->
        <pane
          v-if="dms.selectedItem"
          :size="detailPaneSize"
          min-size="30"
          class="bg-muted/10 border-l border-border relative group/pane"
        >
          <div class="flex h-full w-full relative">
            <!-- Document Preview -->
            <div class="flex-1 h-full relative flex flex-col">
              <!-- Pass file-name property to ensure download has correct name -->
              <DocumentPreview
                :identifier="dms.selectedItem?.uuid || ''"
                source="item"
                :file-name="dms.selectedItem?.name"
              />

              <!-- Floating Controls -->
              <div
                class="absolute top-2 right-2 z-20 flex gap-1 transition-opacity 
                       opacity-0 group-hover/pane:opacity-100"
              >
                <button
                  class="p-1.5 bg-background/80 backdrop-blur-sm rounded-md shadow-sm 
                         border border-border text-muted-foreground hover:text-primary 
                         transition-colors"
                  :title="isMaximized ? 'Show List' : 'Maximize Preview'"
                  @click="toggleMaximize"
                >
                  <Minimize2 v-if="isMaximized" :size="16" />
                  <Maximize2 v-else :size="16" />
                </button>
                
                <button
                  class="p-1.5 bg-background/80 backdrop-blur-sm rounded-md shadow-sm 
                         border border-border text-muted-foreground hover:text-primary 
                         transition-colors"
                  :title="showProperties ? 'Hide Properties' : 'Show Properties'"
                  @click="toggleProperties"
                >
                  <Sidebar :size="16" />
                </button>
              </div>
            </div>

            <!-- Properties Sidebar -->
            <div
              v-if="showProperties"
              class="w-80 h-full border-l border-border bg-background flex-shrink-0 
                     animate-in slide-in-from-right duration-200"
            >
              <StandardDetailPane
                :item="dms.selectedItem"
                @edit="editItem"
                @delete="archiveItem"
              />
            </div>
          </div>
        </pane>
      </splitpanes>

      <!-- Standard View Mode -->
      <div v-else class="h-full overflow-y-auto custom-scrollbar bg-background">
        <ContextFlow
          v-if="viewMode === 'timeline'"
          :items="displayItems"
          @select="handleTimelineSelect"
        />
        <DataTable
          v-else
          :data="displayItems"
          :columns="columns"
          :row-class-name="getRowClass"
          :selected-id="dms.selectedItem?.uuid"
          @row-click="handleRowClick"
          @row-dblclick="handleRowDblClick"
          @row-dragstart="handleRowDragStart"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.splitpanes__splitter) {
  @apply bg-transparent hover:bg-blue-500/10 transition-colors w-[6px] !important;
  border-left: 1px solid theme('colors.border');
  margin-left: -1px;
  position: relative;
  z-index: 20;
}

:deep(.splitpanes__splitter:hover) {
  border-left-color: theme('colors.primary.DEFAULT');
}
</style>