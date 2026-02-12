<script setup lang="ts">
import { computed, h, ref, watch, onMounted, nextTick } from 'vue'
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
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { useAdminQueries } from '@/composables/queries/useAdminQueries'
import { useDragDrop } from '@/composables/useDragDrop'
import { ItemService } from '@/services/ItemService'
import { parseBackendDate } from '@/lib/utils'
import { Folder, Loader2, PlusCircle, Pencil, Trash2, Box, Calendar, Tag, Activity, CheckSquare, FolderOpen, Eye, List, FileText, ArrowLeft, Maximize2, Minimize2, Sidebar, GitCommit, RotateCcw } from 'lucide-vue-next'
import { push } from 'notivue'
import { EStage, ERootCategory, ViewMode, EArchiveFilter } from '@/enums'
import type { Item } from '@/types/models'
import type { ColumnDef } from '@tanstack/vue-table'
import { useQuery } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'

const props = defineProps<{ layout?: number[] }>()
const emit = defineEmits(['update:layout'])

const dms = useDmsStore()
const ui = useUIStore()
const workflow = useWorkflowStore()
const { useCategoryTree } = useContextQueries()
const { useStores } = useAdminQueries()
const { handleDropOnContext, startDrag } = useDragDrop()

const tableRef = ref<InstanceType<typeof DataTable> | null>(null)
const searchQuery = ref('')
const isDragOver = ref(false)
const showProperties = ref(false)
const isMaximized = ref(false)
const viewMode = useStorage('matros-ctx-view-mode', 'table')

const selectedContextId = computed(() => dms.selectedContext?.uuid)
const archiveState = computed(() => dms.archiveViewMode)

const { data: items, isLoading, refetch } = useQuery({
    queryKey: computed(() => [...queryKeys.items.byContext(selectedContextId.value || 'null'), archiveState.value]),
    queryFn: async () => { if (!selectedContextId.value) return []; return await ItemService.getByContext(selectedContextId.value, archiveState.value) },
    enabled: computed(() => !!selectedContextId.value)
})

const { data: stores } = useStores()
const { data: kindTree } = useCategoryTree(ERootCategory.KIND)

const isSplitMode = computed(() => ui.isLayoutZoomed)
const isTrashMode = computed(() => dms.archiveViewMode === EArchiveFilter.ARCHIVED_ONLY)

const storeMap = computed(() => {
  const map = new Map<string, string>()
  if (stores.value) stores.value.forEach((store: any) => map.set(store.uuid, store.shortname || store.name))
  return map
})

const displayItems = computed(() => {
  const raw = items.value || []
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return raw
  return raw.filter(item => (item.name || '').toLowerCase().includes(query))
})

watch([displayItems, selectedContextId, viewMode], ([list, ctxId, mode], [oldList, oldCtxId, oldMode]) => {
  if (!list || list.length === 0 || !ctxId) return
  if (ctxId !== oldCtxId || mode !== oldMode || ((!oldList || oldList.length === 0) && list.length > 0)) {
    if (!dms.selectedItem) dms.setSelectedItem(list[0])
  }
}, { immediate: true })

// Restore focus on mount (e.g. returning from detail)
onMounted(() => {
    if (dms.selectedItem?.uuid) {
        setTimeout(() => tableRef.value?.focusRow(dms.selectedItem!.uuid!), 150)
    }
})

// Listen for focus event from ContextList
const onFocusActiveRow = () => {
    if (dms.selectedItem?.uuid && tableRef.value) {
        tableRef.value.focusRow(dms.selectedItem.uuid)
    }
}

const goBackToFolders = () => ui.setZoom(false)
const toggleZoom = () => ui.setZoom(!ui.isLayoutZoomed)
const toggleMaximize = () => { isMaximized.value = !isMaximized.value; if (isMaximized.value) ui.setZoom(true) }
const toggleProperties = () => showProperties.value = !showProperties.value

const openDocument = async (uuid: string, name: string) => {
  const item = displayItems.value.find(i => i.uuid === uuid)
  if (item) dms.setSelectedItem(item)
  if (!isSplitMode.value) ui.setRightPanel(ViewMode.PREVIEW, { id: uuid, name, source: 'item' })
}

const editItem = () => { if (dms.selectedItem) dms.startItemEditing() }
const deleteItem = () => { if (dms.selectedItem) dms.startItemArchiving() }
const destroyItem = async () => { if (dms.selectedItem?.uuid && confirm(`PERMANENTLY DESTROY '${dms.selectedItem.name}'?`)) { await ItemService.destroy(dms.selectedItem.uuid); push.success('Destroyed'); refetch(); dms.setSelectedItem(null) } }
const restoreItem = async () => { if (dms.selectedItem?.uuid) { await ItemService.restore(dms.selectedItem.uuid); push.success('Restored'); refetch(); dms.setSelectedItem(null) } }
const addItemAction = () => { if (dms.selectedItem?.uuid) workflow.startActionCreation({ itemId: dms.selectedItem.uuid }) }

const handleDragEnter = (event: DragEvent) => { if (event.dataTransfer?.types.includes('Files') || (dms.isDraggingGlobal && dms.currentDragType === 'inbox-file')) isDragOver.value = true }
const handleDragLeave = () => isDragOver.value = false
const handleDrop = (event: DragEvent) => { isDragOver.value = false; handleDropOnContext(event, dms.selectedContext); setTimeout(refetch, 500) }
const handleRowDragStart = (event: DragEvent, row: any) => startDrag(event, 'dms-item', row)
const handleResize = (panes: any[]) => emit('update:layout', panes.map(p => p.size))

const handleRowClick = (item: Item) => { if (item.uuid) dms.setSelectedItem(item) }
const handleRowEnter = (item: Item) => { if (item.uuid) openDocument(item.uuid, item.name || 'Document') }
const handleRowDblClick = (item: Item) => { if (item.uuid) ItemService.openDocument(item.uuid) }
const handleTimelineSelect = (item: any) => { if (item.uuid) openDocument(item.uuid, item.name || 'Document') }

const handlePaneKeyDown = (e: KeyboardEvent) => {
    // Left arrow always goes back to Context list
    if (e.key === 'ArrowLeft') {
        const ctxList = document.getElementById('context-list-pane')
        if (ctxList) {
            e.preventDefault()
            ctxList.focus()
            // Dispatch event to context list to focus its active row
            ctxList.dispatchEvent(new CustomEvent('focus-active-row'))
        }
        return
    }

    // Timeline-specific Navigation
    if (viewMode.value === 'timeline') {
        // Build a list sorted by issueDate ASC (matching the visualization)
        const visualOrder = [...displayItems.value].sort((a, b) => {
             const da = parseBackendDate(a.issueDate)?.getTime() || 0
             const db = parseBackendDate(b.issueDate)?.getTime() || 0
             return da - db
        })
        
        const currentIndex = visualOrder.findIndex(i => i.uuid === dms.selectedItem?.uuid)
        
        if (e.key === 'ArrowDown') {
             e.preventDefault()
             const next = visualOrder[currentIndex + 1]
             if (next) dms.setSelectedItem(next)
        } else if (e.key === 'ArrowUp') {
             e.preventDefault()
             const prev = visualOrder[currentIndex - 1]
             if (prev) dms.setSelectedItem(prev)
        } else if (e.key === 'Enter') {
             e.preventDefault()
             if (dms.selectedItem?.uuid) openDocument(dms.selectedItem.uuid, dms.selectedItem.name || 'Document')
        }
    }
}

const getRowClass = (row: any) => row.stage === 'CLOSED' ? 'table-row-closed' : ''
const createHeaderIcon = (icon: any, label: string) => h('div', { class: 'flex items-center gap-2 text-xs uppercase tracking-wider text-muted-foreground font-bold' }, [h(icon, { size: 14, class: 'text-primary' }), h('span', label)])
const createNameCell = (info: any) => {
  const uuid = info.row.original.uuid
  const name = info.getValue()
  const handleClick = (e: Event) => { e.stopPropagation(); openDocument(uuid, name) }
  // Make whole cell clickable and full size
  return h('div', { class: 'flex items-center gap-2 w-full h-full cursor-pointer group/cell', onClick: handleClick }, [
    h('span', { class: 'font-medium text-foreground hover:underline text-primary truncate' }, name),
    h(Eye, { size: 14, class: 'text-muted-foreground hover:text-primary opacity-0 group-hover/cell:opacity-100' })
  ])
}

const columns: ColumnDef<any>[] = [
  { accessorKey: 'issueDate', header: () => createHeaderIcon(Calendar, 'Date'), size: 120, cell: (info) => h('span', { class: 'text-xs text-muted-foreground font-mono' }, parseBackendDate(info.getValue())?.toLocaleDateString() || '-') },
  { accessorKey: 'kindList', header: () => createHeaderIcon(Tag, 'Type'), size: 150, cell: (info) => h('span', { class: 'text-xs truncate' }, (info.getValue() as any[])?.[0]?.name || '-') },
  { accessorKey: 'name', header: () => createHeaderIcon(FileText, 'Name'), size: 300, cell: createNameCell },
  { accessorKey: 'storeIdentifier', header: () => createHeaderIcon(Box, 'Store'), size: 120, cell: (info) => h('span', { class: 'text-xs text-muted-foreground truncate' }, storeMap.value.get(info.getValue() as string) || '-') },
  { accessorKey: 'stage', header: () => createHeaderIcon(Activity, 'Stage'), size: 100, cell: (info) => h('span', { class: 'text-xs' }, String(info.getValue())) }
]

const listPaneSize = computed(() => !props.layout ? (dms.selectedItem ? 35 : 100) : props.layout[0])
const detailPaneSize = computed(() => isMaximized.value ? 100 : (!props.layout ? 65 : props.layout[1]))
</script>

<template>
  <div id="item-list-pane" class="h-full flex flex-col relative bg-background transition-colors outline-none focus:pane-focused" 
    @dragover.prevent="handleDragEnter" @dragleave.prevent="handleDragLeave" @drop.prevent="handleDrop" @keydown="handlePaneKeyDown" tabindex="-1" @focus-active-row="onFocusActiveRow">
    
    <div v-if="isDragOver && dms.selectedContext" class="absolute inset-0 bg-blue-50/90 dark:bg-blue-900/80 z-50 flex items-center justify-center border-4 border-blue-400 border-dashed m-2 rounded-lg pointer-events-none">
      <div class="flex flex-col items-center text-blue-600 dark:text-blue-300 animate-pulse"><PlusCircle :size="48" /><span class="text-lg font-bold mt-2">Move to {{ dms.selectedContext.name }}</span></div>
    </div>

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
        <BaseButton variant="ghost" size="iconSm" @click="dms.toggleArchiveView" :class="isTrashMode ? 'text-red-600 bg-red-100 dark:bg-red-900/30' : 'text-muted-foreground hover:text-red-600'" :title="isTrashMode ? 'Show Active' : 'Show Trash'"><Trash2 :size="14" /></BaseButton>
        <div class="w-px h-3 bg-border mx-1"></div>
        <template v-if="isTrashMode">
             <BaseButton variant="ghost" size="sm" class="text-green-600 hover:bg-green-50" :disabled="!dms.selectedItem" @click="restoreItem"><RotateCcw :size="14" class="mr-1"/> Restore</BaseButton>
             <BaseButton variant="ghost" size="sm" class="text-red-600 hover:bg-red-50" :disabled="!dms.selectedItem" @click="destroyItem"><Trash2 :size="14" class="mr-1"/> Destroy</BaseButton>
        </template>
        <template v-else>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedItem" @click="editItem"><Pencil :size="14" /></BaseButton>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedItem" @click="addItemAction"><CheckSquare :size="14" /></BaseButton>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedItem" class="hover:text-destructive" @click="deleteItem"><Trash2 :size="14" /></BaseButton>
        </template>
      </div>
    </div>

    <div v-if="selectedContextId" class="p-2 border-b border-border bg-background">
      <SearchInput v-model="searchQuery" placeholder="Filter items..." />
    </div>

    <div class="flex-1 overflow-hidden relative">
      <div v-if="!selectedContextId" class="h-full flex flex-col items-center justify-center text-muted-foreground opacity-60"><Folder :size="48" class="mb-2" /><span class="text-sm">Select a Context from the left</span></div>
      <div v-else-if="isLoading" class="flex h-full items-center justify-center"><Loader2 class="animate-spin text-primary" /></div>
      <EmptyState v-else-if="displayItems.length === 0" :icon="FolderOpen" title="Empty Folder" description="No items found." />
      
      <splitpanes v-else-if="isSplitMode" class="default-theme" @resized="handleResize">
        <pane v-if="!isMaximized" :size="listPaneSize" :min-size="20">
          <div class="h-full overflow-y-auto custom-scrollbar bg-background">
            <ContextFlow v-if="viewMode === 'timeline'" :items="displayItems" @select="handleTimelineSelect" />
            <DataTable v-else ref="tableRef" :data="displayItems" :columns="columns" :row-class-name="getRowClass" :selected-id="dms.selectedItem?.uuid" @row-click="handleRowClick" @row-enter="handleRowEnter" @row-dblclick="handleRowDblClick" @row-dragstart="handleRowDragStart" />
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
        <DataTable v-else ref="tableRef" :data="displayItems" :columns="columns" :row-class-name="getRowClass" :selected-id="dms.selectedItem?.uuid" @row-click="handleRowClick" @row-enter="handleRowEnter" @row-dblclick="handleRowDblClick" @row-dragstart="handleRowDragStart" />
      </div>
    </div>
  </div>
</template>