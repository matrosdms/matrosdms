import { defineStore, storeToRefs } from 'pinia'
import { ref, computed } from 'vue'
import { useSelectionStore } from './selection'
import { useFilterStore } from './filter'
import { useWorkflowStore } from './workflow'
import { useClipboardStore } from './clipboard'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'
import type { Item, Context } from '@/types/models'

/**
 * DMS Facade Store
 * Orchestrates Selection, Filter, Workflow, and Clipboard.
 */
export const useDmsStore = defineStore('dms', () => {
  const selection = useSelectionStore()
  const filter = useFilterStore()
  const workflow = useWorkflowStore()
  const clipboard = useClipboardStore()
  const ui = useUIStore()

  // --- 1. Selection Coordination ---
  function setSelectedContext(contextObj: Context | null) {
    selection.setSelectedContext(contextObj)
    
    // Workflow Interception: If dropping a file, updating context updates the target
    if (ui.rightPanelView === ViewMode.ADD_ITEM) {
        workflow.targetContextForDrop = contextObj
    }
  }

  function setSelectedItem(item: Item | null) {
      selection.setSelectedItem(item)
      // UI Interception: Force detail view when item is clicked
      if (item) ui.setRightPanelView(ViewMode.DETAILS)
  }

  function setActiveContext(ctx: string) {
      filter.setActiveContext(ctx as any)
      // Clear category selection when changing main dimension, but preserve context selection
      selection.clearSelectedCategory()
  }

  // --- 2. Facade Exports ---
  return {
    // State (Read-Only via storeToRefs is preferred in components)
    ...storeToRefs(selection),
    ...storeToRefs(filter),
    ...storeToRefs(workflow),
    
    // Export Stack from Clipboard Store
    itemStack: computed(() => clipboard.stack),
    currentDragType: computed(() => workflow.currentDragType),

    // Actions (Proxied)
    setSelectedCategory: selection.setSelectedCategory,
    clearSelectedCategory: selection.clearSelectedCategory,
    setSelectedItem,
    setSelectedContext,
    
    setActiveContext,
    addFilter: filter.addFilter,
    removeFilter: filter.removeFilter,
    clearAllFilters: filter.clearAllFilters,

    setDragging: workflow.setDragging,
    
    // Workflow Triggers
    startCategoryCreation: workflow.startCategoryCreation,
    startContextCreation: workflow.startContextCreation,
    startItemCreation: workflow.startItemCreation,
    startActionCreation: workflow.startActionCreation,
    
    // Context-Aware Editing Wrappers
    startContextEditing: () => { 
        if(selection.selectedContext) workflow.startContextEditing(selection.selectedContext) 
    },
    startItemEditing: () => { 
        if(selection.selectedItem) ui.setRightPanelView(ViewMode.EDIT_ITEM)
    },
    startCategoryEditing: () => { 
        if(selection.selectedCategoryId) ui.setRightPanelView(ViewMode.EDIT_CATEGORY) 
    },
    
    startContextArchiving: () => { 
        if(selection.selectedContext) ui.setRightPanelView(ViewMode.ARCHIVE_CONTEXT) 
    },
    startItemArchiving: () => { 
        if(selection.selectedItem) ui.setRightPanelView(ViewMode.ARCHIVE_ITEM) 
    },
    
    // Flow Control
    cancelCreation: workflow.cancelCreation,
    finishTask: workflow.finishTask,
    
    // Stack Management (Delegated to Clipboard)
    addToStack: clipboard.addToStack,
    removeFromStack: clipboard.removeFromStack,
    clearStack: clipboard.clearStack
  }
})