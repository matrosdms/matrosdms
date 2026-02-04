<script setup lang="ts">
import { h, computed, ref } from 'vue'
import BasePane from '@/components/ui/BasePane.vue'
import DataTable from '@/components/ui/DataTable.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { useWorkflowStore } from '@/stores/workflow'
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { getTagClassByKey } from '@/utils/tagStyles'
import { Pencil, XCircle, Plus, CheckSquare, Folder, Hash, Tags, Activity, Lock, Unlock } from 'lucide-vue-next'
import { push } from 'notivue'
import { EStage, EStageLabels, type EStageType, ERootCategoryList } from '@/enums'
import { useDragDrop } from '@/composables/useDragDrop'
import type { ColumnDef } from '@tanstack/vue-table'

// ============================================================================
// STORES & COMPOSABLES
// ============================================================================

const dms = useDmsStore()
const ui = useUIStore()
const workflow = useWorkflowStore()
const { startDrag } = useDragDrop()
const { contexts, isLoadingContexts } = useContextQueries()

// ============================================================================
// STATE
// ============================================================================

const searchQuery = ref('')

// ============================================================================
// CONSTANTS
// ============================================================================

const COLUMN_SIZES = {
  NAME: { size: 300, minSize: 150, maxSize: 500 },
  COUNT: { size: 80, minSize: 60, maxSize: 100 },
  TAGS: { size: 400, minSize: 200, maxSize: 600 },
  STAGE: { size: 90, minSize: 80, maxSize: 120 },
} as const

// Define styles for different stages
const STAGE_ROW_CLASSES: Record<string, string> = {
  [EStage.ACTIVE]: 'table-row-active',
  [EStage.CLOSED]: 'table-row-closed opacity-70 italic'
}

// ============================================================================
// ACTIONS
// ============================================================================

const showContextRequiredWarning = () => {
  push.warning("Please select a context first")
}

const actions = {
  edit: () => {
    if (!dms.selectedContext) return showContextRequiredWarning()
    dms.startContextEditing()
  },
  close: () => {
    if (!dms.selectedContext) return showContextRequiredWarning()
    dms.startContextArchiving()
  },
  create: () => dms.startContextCreation(),
  addAction: () => {
    if (!dms.selectedContext) return showContextRequiredWarning()
    workflow.startActionCreation({ contextId: dms.selectedContext.uuid })
  }
}

// ============================================================================
// EVENT HANDLERS
// ============================================================================

const handleRowClick = (ctx: any) => {
  dms.setSelectedContext(ctx)
}

const handleRowDblClick = (ctx: any) => {
  dms.setSelectedContext(ctx)
  ui.triggerForceZoom()
}

const handleRowDragStart = (event: DragEvent, row: any) => {
  const payload = { id: row.uuid, name: row.name }
  startDrag(event, 'dms-context', payload)
}

// ============================================================================
// DATA PROCESSING
// ============================================================================

const processedContexts = computed(() => {
  if (!contexts.value) return []
  
  return contexts.value.map((ctx: any) => {
    const categoryIds = new Set<string>()
    
    if (ctx.dictionary) {
      Object.values(ctx.dictionary).forEach((tagList: any) => {
        if (!Array.isArray(tagList)) return
        
        tagList.forEach((tag: any) => {
          if (tag.uuid) categoryIds.add(tag.uuid)
          
          tag.parents?.forEach((parent: any) => {
            if (parent.uuid) categoryIds.add(parent.uuid)
          })
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
    // Search filter
    if (query && !ctx.name?.toLowerCase().includes(query)) {
      return false
    }
    
    // No category filters active
    if (!hasAnyFilter) return true

    // Category filters
    return ERootCategoryList.every(category => {
      const activeFilters = dms.filters[category]
      if (!activeFilters?.length) return true

      return activeFilters.some(filter => {
        if (filter.transitiveChildrenAndSelf) {
          return Array.from(ctx._categoryIds as Set<string>).some(tagId => 
            filter.transitiveChildrenAndSelf?.has(tagId)
          )
        }
        return ctx._categoryIds.has(filter.id)
      })
    })
  })
})

// ============================================================================
// ROW STYLING
// ============================================================================

const getRowClass = (row: any): string => {
  // 1. Selection override
  if (dms.selectedContext?.uuid === row.uuid) {
    return '!border-l-blue-600 bg-blue-100 dark:bg-blue-900/40 dark:text-white'
  }
  // 2. Stage-based styling via Global CSS Classes
  return STAGE_ROW_CLASSES[row.stage] || STAGE_ROW_CLASSES[EStage.ACTIVE]
}

// ============================================================================
// COLUMN HELPERS
// ============================================================================

const createHeaderIcon = (icon: any, label: string) => {
  return h('div', {
    class: 'flex items-center gap-2 text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 font-bold'
  }, [
    h(icon, { size: 14, class: 'text-blue-500' }),
    h('span', label)
  ])
}

const createCountBadge = (value: number) => {
  return h('div', {
    class: 'text-center text-xs font-semibold text-gray-500 dark:text-gray-400 bg-white/50 dark:bg-black/20 border border-gray-200 dark:border-gray-700 rounded-md px-1.5 py-0.5 w-fit mx-auto'
  }, value.toString())
}

const createTagBadges = (dictionary: any) => {
  if (!dictionary) {
    return h('span', { 
      class: 'text-[10px] text-gray-400 italic' 
    }, '-')
  }

  const badges: any[] = []
  
  Object.entries(dictionary).forEach(([rootId, tags]) => {
    const className = getTagClassByKey(rootId)
    
    if (Array.isArray(tags)) {
      tags.forEach(tag => {
        badges.push(h('span', {
          class: `matros-tag ${className} border`
        }, tag.name))
      })
    }
  })

  return badges.length > 0
    ? h('div', { class: 'flex flex-wrap gap-1.5' }, badges)
    : h('span', { class: 'text-[10px] text-gray-400 italic' }, '-')
}

const createStageLabel = (stage: EStageType) => {
  const stageValue = stage || EStage.ACTIVE
  const label = EStageLabels[stageValue] || stageValue
  
  return h('div', {
    class: 'flex items-center justify-end gap-1.5 text-xs text-gray-500 dark:text-gray-400'
  }, [h('span', label)])
}

// ============================================================================
// COLUMN DEFINITIONS
// ============================================================================

const columns: ColumnDef<any>[] = [
  {
    accessorKey: 'name',
    header: () => createHeaderIcon(Folder, 'Context Name'),
    ...COLUMN_SIZES.NAME,
    cell: (info) => h('span', {
      class: 'font-medium text-gray-800 dark:text-gray-200'
    }, String(info.getValue()))
  },
  {
    id: 'count',
    header: () => createHeaderIcon(Hash, ''),
    ...COLUMN_SIZES.COUNT,
    accessorFn: (row) => row.itemCount || 0,
    cell: (info) => createCountBadge(info.getValue() as number)
  },
  {
    accessorKey: 'dictionary',
    header: () => createHeaderIcon(Tags, 'Tags'),
    ...COLUMN_SIZES.TAGS,
    cell: (info) => createTagBadges(info.getValue())
  },
  {
    accessorKey: 'stage',
    header: () => createHeaderIcon(Activity, ''),
    ...COLUMN_SIZES.STAGE,
    cell: (info) => createStageLabel(info.getValue() as EStageType)
  }
]
</script>

<template>
  <BasePane 
    title="Context List" 
    :count="filteredContexts.length" 
    :total="contexts?.length || 0"
  >
    <!-- Toolbar Actions -->
    <template #actions>
      <BaseButton
        variant="ghost"
        size="iconSm"
        class="mr-1"
        :class="ui.isContextListLocked 
          ? 'text-blue-600 bg-blue-50 dark:bg-blue-900/20' 
          : 'text-gray-400'"
        :title="ui.isContextListLocked 
          ? 'Locked: Click to select, DblClick to zoom' 
          : 'Auto-Zoom Enabled'"
        @click="ui.toggleContextListLock"
      >
        <Lock v-if="ui.isContextListLocked" :size="14" />
        <Unlock v-else :size="14" />
      </BaseButton>

      <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1" />

      <BaseButton
        variant="ghost"
        size="iconSm"
        :disabled="!dms.selectedContext"
        title="Edit Context"
        @click="actions.edit"
      >
        <Pencil :size="14" />
      </BaseButton>

      <BaseButton
        variant="ghost"
        size="iconSm"
        :disabled="!dms.selectedContext"
        title="Add Task to Context"
        @click="actions.addAction"
      >
        <CheckSquare :size="14" />
      </BaseButton>

      <BaseButton
        variant="ghost"
        size="iconSm"
        :disabled="!dms.selectedContext"
        class="hover:text-destructive hover:bg-destructive/10"
        title="Close Context"
        @click="actions.close"
      >
        <XCircle :size="14" />
      </BaseButton>

      <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1" />

      <BaseButton
        variant="ghost"
        size="iconSm"
        class="text-primary hover:text-primary-hover hover:bg-primary/10"
        title="Create Context"
        @click="actions.create"
      >
        <Plus :size="16" stroke-width="3" />
      </BaseButton>
    </template>

    <!-- Search Filter -->
    <template #filter>
      <SearchInput 
        v-model="searchQuery" 
        placeholder="Filter contexts..." 
      />
    </template>

    <!-- Table Content -->
    <div class="h-full bg-white dark:bg-gray-900">
      <div 
        v-if="isLoadingContexts" 
        class="flex h-full items-center justify-center"
      >
        <div class="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
      </div>

      <DataTable
        v-else
        :data="filteredContexts"
        :columns="columns"
        :row-class-name="getRowClass"
        @row-click="handleRowClick"
        @row-dblclick="handleRowDblClick"
        @row-dragstart="handleRowDragStart"
      />
    </div>
  </BasePane>
</template>