import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'
import type { Item, Context } from '@/types/models'

export const useSelectionStore = defineStore('selection', () => {
  const ui = useUIStore()

  const selectedCategoryId = ref<string | null>(null)
  const selectedCategoryLabel = ref('')
  const selectedContext = ref<Context | null>(null) 
  const selectedItem = ref<Item | null>(null)

  function setSelectedCategory(id: string | null, label = '') {
    selectedCategoryId.value = id
    selectedCategoryLabel.value = label
    selectedItem.value = null
    selectedContext.value = null
    // Reset view to avoid showing stale forms
    ui.setRightPanelView(ViewMode.DETAILS)
  }

  function setSelectedContext(contextObj: Context | null) {
    selectedContext.value = contextObj
    
    // If we are in the middle of creating an item (Drag & Drop), don't reset
    if (ui.rightPanelView === ViewMode.ADD_ITEM) {
        // workflow store handles the target update via watcher/logic
    } else {
        selectedItem.value = null
        ui.setRightPanelView(ViewMode.DETAILS)
    }
  }

  function setSelectedItem(item: Item | null) {
    selectedItem.value = item
    if (item) {
        // Ensure we switch to Details view when an item is selected
        ui.setRightPanelView(ViewMode.DETAILS)
    }
  }

  return { 
    selectedCategoryId, selectedCategoryLabel, selectedContext, selectedItem,
    setSelectedCategory, setSelectedContext, setSelectedItem
  }
})