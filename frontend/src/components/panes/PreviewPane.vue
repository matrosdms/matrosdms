<script setup lang="ts">
import { computed } from 'vue'
import { useUIStore } from '@/stores/ui'
import { useDmsStore } from '@/stores/dms'
import { ViewMode } from '@/enums'
import { ArrowLeft, FileText, Info, Columns, Eye } from 'lucide-vue-next'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
const ui = useUIStore()
const dms = useDmsStore()
// Data passed via ui.setRightPanel
const identifier = computed(() => ui.panelData.id)
const source = computed(() => ui.panelData.source || 'item')
const title = computed(() => ui.panelData.name || 'Document Preview')
const goBack = () => {
// If it was an item, ensure selection is kept so list scrolls to it
if (source.value === 'item' && identifier.value) {
dms.setSelectedItem(dms.selectedItem) // Re-trigger selection logic if needed
}
// Return to default view (List)
ui.setRightPanelView(ViewMode.DETAILS)
}
const switchToSplit = () => {
ui.triggerForceZoom() // Force Split Mode
ui.setRightPanelView(ViewMode.DETAILS) // Go to List/Split View
}
const switchToMetadata = () => {
// Switch to Edit Mode (which defaults to Split or Metadata tab)
ui.setRightPanelView(ViewMode.EDIT_ITEM)
}
</script>
<template>
<div class="h-full flex flex-col bg-gray-100 dark:bg-gray-900 animate-in fade-in duration-200 transition-colors">
<!-- Header -->
  <div class="flex items-center justify-between px-4 py-2 bg-white dark:bg-gray-800 border-b border-gray-300 dark:border-gray-700 shadow-sm shrink-0 h-[45px]">
      <div class="flex items-center gap-3 overflow-hidden">
          <button @click="goBack" class="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full text-gray-600 dark:text-gray-300 transition-colors" title="Back to List">
              <ArrowLeft :size="20" />
          </button>
          <div class="flex items-center gap-2 overflow-hidden">
              <FileText :size="18" class="text-blue-500 shrink-0" />
              <span class="font-bold text-gray-800 dark:text-gray-200 text-sm truncate">{{ title }}</span>
              <span v-if="source === 'inbox'" class="text-[10px] bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 px-1.5 py-0.5 rounded border border-purple-100 dark:border-purple-800 uppercase font-bold">Inbox</span>
          </div>
      </div>
      
      <div class="flex gap-2">
          <!-- View Toggles (Only for stored items) -->
          <div v-if="source === 'item'" class="flex bg-gray-100 dark:bg-gray-700 rounded-md p-0.5 border border-gray-200 dark:border-gray-600 transition-colors">
              <button @click="switchToSplit" class="p-1.5 rounded transition-all text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" title="Split View"><Columns :size="14" /></button>
              <button class="p-1.5 rounded transition-all bg-white dark:bg-gray-600 shadow-sm text-blue-600 dark:text-blue-400" title="Document Only"><Eye :size="14" /></button>
              <button @click="switchToMetadata" class="p-1.5 rounded transition-all text-gray-400 hover:text-gray-600 dark:hover:text-gray-300" title="Metadata / Form"><FileText :size="14" /></button>
          </div>
      </div>
  </div>

  <!-- Content -->
  <div class="flex-1 overflow-hidden relative">
      <DocumentPreview 
        :identifier="identifier" 
        :source="source" 
        :file-name="title"
      />
  </div>
</div>
</template>