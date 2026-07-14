import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'
import type { components } from '@/types/schema'
import type { InboxFile } from '@/types/events'

type MContext = components['schemas']['MContext'];
type MAction = components['schemas']['MAction'];

// NOTE: live inbox file state used to live here (liveInboxFiles + a parallel
// set of processing/progress/analysis maps). It now lives solely in the
// TanStack Query ['inbox'] cache (see composables/queries/useInboxQueries),
// patched from SSE events - one source of truth. This store keeps only the
// creation-flow / drag UI state that has no server representation.
export const useWorkflowStore = defineStore('workflow', () => {
  const ui = useUIStore()

  // Creation State
  const parentCategoryForCreation = ref<string | null>(null)
  const pendingInboxFile = ref<InboxFile | null>(null)
  const targetContextForDrop = ref<MContext | null>(null)
  const itemFormDraft = ref<any>({})
  const suspendedView = ref<string | null>(null)

  // Action Context
  const pendingActionTarget = ref<{ itemId?: string; contextId?: string }>({})

  // Drag State
  const isDraggingGlobal = ref(false)
  const currentDragType = ref<string | null>(null)

  // --- ACTIONS ---

  function setDragging(bool: boolean, type: string | null = null) {
      isDraggingGlobal.value = bool
      currentDragType.value = bool ? type : null
  }

  // --- VIEW SWITCHING ACTIONS ---

  function startCategoryCreation(parentId: string) {
    suspendedView.value = ui.rightPanelView === ViewMode.ADD_ITEM ? ViewMode.ADD_ITEM : null
    parentCategoryForCreation.value = parentId
    ui.setRightPanel(ViewMode.CREATE_CATEGORY)
  }

  function startContextCreation() { 
      ui.setRightPanel(ViewMode.CREATE_CONTEXT) 
  }

  function startItemCreation(contextObj: MContext, inboxFile: any) {
    itemFormDraft.value = {}
    suspendedView.value = null
    targetContextForDrop.value = contextObj
    pendingInboxFile.value = inboxFile
    // The item form reads live inbox data from the ['inbox'] query cache by sha256.
    ui.setRightPanel(ViewMode.ADD_ITEM)
  }
  
  function startActionCreation(target: { itemId?: string; contextId?: string } = {}) {
      pendingActionTarget.value = target
      ui.setRightPanel(ViewMode.CREATE_ACTION, { initialData: null, ...target })
  }

  function startActionEditing(action: MAction) {
      ui.setRightPanel(ViewMode.CREATE_ACTION, { initialData: action })
  }

  function startContextEditing(context: MContext | null) { 
      if (!context) return
      ui.setRightPanel(ViewMode.EDIT_CONTEXT) 
  }
  
  function cancelCreation() {
    ui.setRightPanel(ViewMode.DETAILS)
    pendingInboxFile.value = null
    targetContextForDrop.value = null
    pendingActionTarget.value = {} 
    itemFormDraft.value = {} 
    suspendedView.value = null
  }
  
  function finishTask() {
      if (suspendedView.value) {
          ui.setRightPanel(suspendedView.value)
          suspendedView.value = null
      } else {
          cancelCreation()
      }
  }

  return {
    // State
    parentCategoryForCreation,
    pendingInboxFile,
    targetContextForDrop,
    itemFormDraft,
    suspendedView,
    isDraggingGlobal,
    currentDragType,
    pendingActionTarget,

    // Methods
    setDragging,
    startCategoryCreation,
    startContextCreation,
    startItemCreation,
    startActionCreation,
    startActionEditing,
    startContextEditing,
    cancelCreation,
    finishTask
  }
})