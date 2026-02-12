<script setup lang="ts">
import { h, computed, ref, watch } from 'vue'
import BasePane from '@/components/ui/BasePane.vue'
import DataTable from '@/components/ui/DataTable.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { useWorkflowStore } from '@/stores/workflow'
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { getTagClassByKey } from '@/utils/tagStyles'
import { Pencil, Archive, Plus, CheckSquare, Folder, Hash, Tags, Activity, Lock, Unlock } from 'lucide-vue-next'
import { push } from 'notivue'
import { EStage, EStageLabels, type EStageType, ERootCategoryList } from '@/enums'
import { useDragDrop } from '@/composables/useDragDrop'
import type { ColumnDef } from '@tanstack/vue-table'

const dms = useDmsStore()
const ui = useUIStore()
const workflow = useWorkflowStore()
const { startDrag } = useDragDrop()
const { contexts, isLoadingContexts } = useContextQueries()

const searchQuery = ref('')
const tableRef = ref<InstanceType<typeof DataTable> | null>(null)

const COLUMN_SIZES = {
  NAME: { size: 300, minSize: 150, maxSize: 500 },
  COUNT: { size: 80, minSize: 60, maxSize: 100 },
  TAGS: { size: 400, minSize: 200, maxSize: 600 },
  STAGE: { size: 90, minSize: 80, maxSize: 120 },
}

const showContextRequiredWarning = () => push.warning("Please select a context first")

const actions = {
  edit: () => { if (!dms.selectedContext) return showContextRequiredWarning(); dms.startContextEditing() },
  archive: () => { if (!dms.selectedContext) return showContextRequiredWarning(); dms.startContextArchiving() },
  create: () => dms.startContextCreation(),
  addAction: () => { if (!dms.selectedContext) return showContextRequiredWarning(); workflow.startActionCreation({ contextId: dms.selectedContext.uuid }) }
}

const handleRowClick = (ctx: any) => dms.setSelectedContext(ctx)
const handleRowEnter = (ctx: any) => dms.setSelectedContext(ctx)
const handleRowDblClick = (ctx: any) => { dms.setSelectedContext(ctx); ui.triggerForceZoom() }
const handleRowDragStart = (event: DragEvent, row: any) => startDrag(event, 'dms-context', { id: row.uuid, name: row.name })

const handlePaneKeyDown = (e: KeyboardEvent) => {
    // Arrow Right -> Focus Item List (ContextItemList)
    if (e.key === 'ArrowRight') {
        const itemList = document.getElementById('item-list-pane')
        if (itemList) {
            e.preventDefault()
            itemList.focus()
            
            // Dispatch a custom event to tell ItemList to focus its active row
            const event = new CustomEvent('focus-active-row')
            itemList.dispatchEvent(event)
        }
    }
}

const processedContexts = computed(() => {
  if (!contexts.value) return []
  return contexts.value.map((ctx: any) => {
    const categoryIds = new Set<string>()
    if (ctx.dictionary) {
      Object.values(ctx.dictionary).forEach((tagList: any) => {
        if (!Array.isArray(tagList)) return
        tagList.forEach((tag: any) => {
          if (tag.uuid) categoryIds.add(tag.uuid)
          tag.parents?.forEach((parent: any) => { if (parent.uuid) categoryIds.add(parent.uuid) })
        })
      })
    }
    return { ...ctx, _categoryIds: categoryIds }
  })
})

const filteredContexts = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  const hasAnyFilter = ERootCategoryList.some(cat => dms.filters[cat]?.length > 0)
  return processedContexts.value.filter(ctx => {
    if (query && !ctx.name?.toLowerCase().includes(query)) return false
    if (!hasAnyFilter) return true
    return ERootCategoryList.every(category => {
      const activeFilters = dms.filters[category]
      if (!activeFilters?.length) return true
      return activeFilters.some(filter => {
        if (filter.transitiveChildrenAndSelf) {
          return Array.from(ctx._categoryIds as Set<string>).some(tagId => filter.transitiveChildrenAndSelf?.has(tagId))
        }
        return ctx._categoryIds.has(filter.id)
      })
    })
  })
})

watch(filteredContexts, (list) => {
  if (list?.length > 0 && !dms.selectedContext) dms.setSelectedContext(list[0])
}, { immediate: true })

const getRowClass = (row: any) => row.stage === EStage.CLOSED ? 'table-row-closed' : ''

const createHeaderIcon = (icon: any, label: string) => h('div', { class: 'flex items-center gap-2 text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold' }, [h(icon, { size: 14, class: 'text-blue-500' }), h('span', label)])
const createCountBadge = (value: number) => h('div', { class: 'text-center text-xs font-semibold text-gray-500 dark:text-gray-400 bg-white/50 dark:bg-black/20 border border-gray-200 dark:border-gray-700 rounded-md px-1.5 py-0.5 w-fit mx-auto' }, value.toString())
const createTagBadges = (dictionary: any) => {
  if (!dictionary) return h('span', { class: 'text-[10px] text-gray-400 italic' }, '-')
  const badges: any[] = []
  Object.entries(dictionary).forEach(([rootId, tags]) => {
    if (Array.isArray(tags)) tags.forEach(tag => badges.push(h('span', { class: `matros-tag ${getTagClassByKey(rootId)} border` }, tag.name)))
  })
  return badges.length > 0 ? h('div', { class: 'flex flex-wrap gap-1.5' }, badges) : h('span', { class: 'text-[10px] text-gray-400 italic' }, '-')
}
const createStageLabel = (stage: EStageType) => h('div', { class: 'flex items-center justify-end gap-1.5 text-xs text-gray-500 dark:text-gray-400' }, [h('span', EStageLabels[stage || EStage.ACTIVE])])

const columns: ColumnDef<any>[] = [
  { accessorKey: 'name', header: () => createHeaderIcon(Folder, 'Context Name'), ...COLUMN_SIZES.NAME, cell: (info) => h('span', { class: 'font-medium text-gray-800 dark:text-gray-200' }, String(info.getValue())) },
  { id: 'count', header: () => createHeaderIcon(Hash, ''), ...COLUMN_SIZES.COUNT, accessorFn: (row) => row.itemCount || 0, cell: (info) => createCountBadge(info.getValue() as number) },
  { accessorKey: 'dictionary', header: () => createHeaderIcon(Tags, 'Tags'), ...COLUMN_SIZES.TAGS, cell: (info) => createTagBadges(info.getValue()) },
  { accessorKey: 'stage', header: () => createHeaderIcon(Activity, ''), ...COLUMN_SIZES.STAGE, cell: (info) => createStageLabel(info.getValue() as EStageType) }
]

// Listen for focus requests from ItemList
const onFocusActiveRow = () => {
    if (dms.selectedContext?.uuid && tableRef.value) {
        tableRef.value.focusRow(dms.selectedContext.uuid)
    }
}
</script>

<template>
  <div 
    id="context-list-pane" 
    class="h-full flex flex-col bg-white dark:bg-gray-900 outline-none focus:pane-focused transition-all" 
    tabindex="-1"
    @keydown="handlePaneKeyDown"
    @focus-active-row="onFocusActiveRow"
  >
    <BasePane title="Context List" :count="filteredContexts.length" :total="contexts?.length || 0">
        <template #actions>
            <BaseButton variant="ghost" size="iconSm" class="mr-1" :class="ui.isContextListLocked ? 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' : 'text-gray-400'" @click="ui.toggleContextListLock"><component :is="ui.isContextListLocked ? Lock : Unlock" :size="14" /></BaseButton>
            <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1" />
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedContext" @click="actions.edit"><Pencil :size="14" /></BaseButton>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedContext" @click="actions.addAction"><CheckSquare :size="14" /></BaseButton>
            <BaseButton variant="ghost" size="iconSm" :disabled="!dms.selectedContext" class="hover:text-destructive" @click="actions.archive"><Archive :size="14" /></BaseButton>
            <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1" />
            <BaseButton variant="ghost" size="iconSm" class="text-primary hover:bg-primary/10" @click="actions.create"><Plus :size="16" stroke-width="3" /></BaseButton>
        </template>
        <template #filter><SearchInput v-model="searchQuery" placeholder="Filter contexts..." /></template>
        <div class="h-full bg-white dark:bg-gray-900">
            <div v-if="isLoadingContexts" class="flex h-full items-center justify-center"><div class="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" /></div>
            <DataTable 
                v-else 
                ref="tableRef" 
                :data="filteredContexts" 
                :columns="columns" 
                :row-class-name="getRowClass" 
                :selected-id="dms.selectedContext?.uuid" 
                @row-click="handleRowClick" 
                @row-enter="handleRowEnter" 
                @row-dblclick="handleRowDblClick" 
                @row-dragstart="handleRowDragStart" 
            />
        </div>
    </BasePane>
  </div>
</template>