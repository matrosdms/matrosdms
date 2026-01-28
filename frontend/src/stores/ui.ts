import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ViewMode } from '@/enums'
import { useStorage } from '@vueuse/core'
import { push } from 'notivue'

export interface LogEntry {
    id: number;
    time: string;
    message: string;
    type: 'info' | 'success' | 'error' | 'debug' | 'warning';
}

export const useUIStore = defineStore('ui', () => {
  
  const currentView = ref<'dms' | 'settings' | 'about' | 'timeline' | 'profile' | 'search' | 'ai'>('dms')
  const sidebarMode = ref<'tags' | 'inbox' | 'actions'>('tags')
  
  const rightPanelView = ref<ViewMode | string>(ViewMode.DETAILS)
  const panelData = ref<Record<string, any>>({}) 

  const logs = ref<LogEntry[]>([])

  const isContextListLocked = useStorage('matros-ui-ctx-lock', false)
  const isLayoutZoomed = ref(false)

  function setView(view: 'dms' | 'settings' | 'about' | 'timeline' | 'profile' | 'search' | 'ai') { currentView.value = view }
  function setSidebarMode(mode: 'tags' | 'inbox' | 'actions') { sidebarMode.value = mode }
  
  function setRightPanel(view: ViewMode | string, data: Record<string, any> = {}) { 
      rightPanelView.value = view
      panelData.value = data
  }

  function setRightPanelView(view: ViewMode | string) {
      setRightPanel(view, {})
  }

  function addLog(message: string, type: LogEntry['type'] = 'info') {
    const timestamp = new Date().toLocaleTimeString('de-DE', { hour12: false });
    logs.value.unshift({ id: Date.now() + Math.random(), time: timestamp, message, type });
    if (logs.value.length > 1000) logs.value.pop();

    // IMPROVEMENT: Automatically notify on errors if they weren't caught elsewhere
    // This ensures no error goes completely silent
    if (type === 'error' && !message.includes('backend connection')) {
       // Check if message is critical enough? For now, we trust the caller.
       // We skip backend connection errors because OfflineView handles those visually.
       console.error(`[UI Log Error] ${message}`)
    }
  }

  function toggleContextListLock() {
      isContextListLocked.value = !isContextListLocked.value
      if (!isContextListLocked.value) {
          isLayoutZoomed.value = false
      }
  }

  function setZoom(state: boolean) {
      isLayoutZoomed.value = state
  }

  function triggerForceZoom() {
      isContextListLocked.value = true
      isLayoutZoomed.value = true
  }

  return { 
    currentView, sidebarMode, rightPanelView, panelData, logs,
    isContextListLocked, isLayoutZoomed,
    setView, setSidebarMode, setRightPanel, setRightPanelView, addLog,
    toggleContextListLock, setZoom, triggerForceZoom
  }
})