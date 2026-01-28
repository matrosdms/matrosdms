import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'
import type { components } from '@/types/schema'
import type { InboxAnalysis } from '@/types/analysis'
import type { InboxFile } from '@/types/events'

type MContext = components['schemas']['MContext'];
type MAction = components['schemas']['MAction'];

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

  // --- NEW: Live Inbox State (Progressive Updates) ---
  const liveInboxFiles = ref<Record<string, InboxFile>>({})
  
  // Legacy Analysis State
  const inboxAnalysis = ref<Record<string, InboxAnalysis>>({})
  const processingInboxItems = reactive(new Set<string>())
  
  // Track real-time progress messages
  const inboxProgress = ref<Record<string, string>>({})
  
  // Track last update timestamp for Watchdog
  const lastInboxUpdate = ref<Record<string, number>>({})
  
  // Drag State
  const isDraggingGlobal = ref(false)
  const currentDragType = ref<string | null>(null)

  // --- ACTIONS ---

  function setDragging(bool: boolean, type: string | null = null) { 
      isDraggingGlobal.value = bool 
      currentDragType.value = bool ? type : null
  }

  // --- NEW: Progressive Update Action ---
  function upsertLiveFile(file: Partial<InboxFile>) {
      if (!file.sha256) return
      
      const hash = file.sha256
      const existing = liveInboxFiles.value[hash] || {}
      
      // Merge Strategy: Overlay new fields onto existing state
      liveInboxFiles.value = {
          ...liveInboxFiles.value,
          [hash]: {
              ...existing,
              ...file,
              // Deep merge key nested objects to avoid overwriting with undefined
              fileInfo: { ...(existing.fileInfo || {}), ...(file.fileInfo || {}) },
              emailInfo: file.emailInfo ? { ...file.emailInfo } : (existing.emailInfo),
              prediction: file.prediction ? { ...file.prediction } : (existing.prediction)
          } as InboxFile
      }
      
      // Update legacy processing set for UI spinners
      if (file.status === 'PROCESSING') {
          addProcessingItem(hash)
          if (file.progressMessage) setInboxProgress(hash, file.progressMessage)
      } else if (file.status === 'READY' || file.status === 'DUPLICATE' || file.status === 'ERROR') {
          removeProcessingItem(hash)
      }
      
      lastInboxUpdate.value = { ...lastInboxUpdate.value, [hash]: Date.now() }
  }

  function removeLiveFile(hash: string) {
      if (liveInboxFiles.value[hash]) {
          const newFiles = { ...liveInboxFiles.value }
          delete newFiles[hash]
          liveInboxFiles.value = newFiles
      }
      removeProcessingItem(hash)
  }

  // Legacy Support Mappers
  function setInboxAnalysis(hash: string, result: InboxAnalysis) { 
      inboxAnalysis.value = { ...inboxAnalysis.value, [hash]: result } 
      if (inboxProgress.value[hash]) {
          const newProgress = { ...inboxProgress.value }
          delete newProgress[hash]
          inboxProgress.value = newProgress
      }
      lastInboxUpdate.value = { ...lastInboxUpdate.value, [hash]: Date.now() }
  }

  function setInboxProgress(hash: string, message: string) {
      inboxProgress.value = { ...inboxProgress.value, [hash]: message }
      lastInboxUpdate.value = { ...lastInboxUpdate.value, [hash]: Date.now() }
  }

  function addProcessingItem(hash: string) { 
      processingInboxItems.add(hash) 
      lastInboxUpdate.value = { ...lastInboxUpdate.value, [hash]: Date.now() }
  }

  function removeProcessingItem(hash: string) { 
      processingInboxItems.delete(hash) 
      if (inboxProgress.value[hash]) {
          const newProgress = { ...inboxProgress.value }
          delete newProgress[hash]
          inboxProgress.value = newProgress
      }
      if (lastInboxUpdate.value[hash]) {
          const newUpdates = { ...lastInboxUpdate.value }
          delete newUpdates[hash]
          lastInboxUpdate.value = newUpdates
      }
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
    
    // REFACTORED: Rely solely on sha256
    const hash = inboxFile.sha256;
    if (hash && liveInboxFiles.value[hash]) {
        // Form will read from liveInboxFiles if necessary
    }
    
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

  const duplicateContentFileHashes = computed(() => {
      const counts: Record<string, number> = {}
      const analysis = inboxAnalysis.value
      
      // Merge Legacy Analysis + Live Files status
      const allHashes = new Set([...Object.keys(analysis), ...Object.keys(liveInboxFiles.value)])
      
      // Calculate Content Hash collisions
      Object.values(analysis).forEach((r) => {
          if(r.contentHash) counts[r.contentHash] = (counts[r.contentHash] || 0) + 1
      })
      
      const contentHashDuplicates = new Set<string>()
      for (const [hash, count] of Object.entries(counts)) {
          if (count > 1) contentHashDuplicates.add(hash)
      }
      
      const result = new Set<string>()
      
      // 1. Check Legacy
      for (const [fHash, r] of Object.entries(analysis)) {
          if ((r.contentHash && contentHashDuplicates.has(r.contentHash)) || r.isDuplicate) {
              result.add(fHash)
          }
      }
      
      // 2. Check Live State
      for (const [fHash, file] of Object.entries(liveInboxFiles.value)) {
          if (file.status === 'DUPLICATE') result.add(fHash)
      }
      
      return result
  })

  return {
    // State
    parentCategoryForCreation, 
    pendingInboxFile, 
    targetContextForDrop,
    itemFormDraft, 
    suspendedView, 
    isDraggingGlobal, 
    currentDragType,
    inboxAnalysis, 
    inboxProgress, 
    processingInboxItems, 
    pendingActionTarget,
    lastInboxUpdate, 
    liveInboxFiles, 
    
    // Computeds
    duplicateContentFileHashes,
    
    // Methods
    setDragging, 
    startCategoryCreation, 
    startContextCreation, 
    startItemCreation, 
    startActionCreation, 
    startActionEditing, 
    startContextEditing, 
    cancelCreation, 
    finishTask,
    setInboxAnalysis, 
    setInboxProgress, 
    addProcessingItem, 
    removeProcessingItem,
    upsertLiveFile, 
    removeLiveFile 
  }
})