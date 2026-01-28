<script setup lang="ts">
import { ref, computed } from 'vue'
import { PanelBottomClose, PanelBottomOpen, Layers, Trash2, Bug, Info, CheckCircle, AlertCircle, AlertTriangle } from 'lucide-vue-next'
import { useUIStore } from '@/stores/ui'
import { useDmsStore } from '@/stores/dms'
import { useDragDrop } from '@/composables/useDragDrop'

const ui = useUIStore()
const dms = useDmsStore()
const { startDrag } = useDragDrop()

defineProps({ collapsed: Boolean })
defineEmits(['toggle-minimize'])

const activeTab = ref('itemstack') 
const isDragOverStack = ref(false)

// Added 'warning' to filters
const filters = ref({ info: true, success: true, error: true, debug: true, warning: true })

// Fix: explicit casting or strict check to satisfy TS if needed, but here simple filter works
const filteredLogs = computed(() => ui.logs.filter(log => filters.value[log.type]))
const clearLogs = () => ui.logs = []

const onStackDrop = (event: DragEvent) => {
    isDragOverStack.value = false
    const raw = event.dataTransfer?.getData('application/json')
    if (raw) {
        try {
            const data = JSON.parse(raw)
            if (data.type === 'dms-item') dms.addToStack(data)
        } catch(e) {}
    }
}

const onDragStartStackItem = (event: DragEvent, item: any) => startDrag(event, 'dms-item', item)
</script>

<template>
  <div class="h-full flex flex-col border-t bg-background dark:bg-gray-900 dark:border-gray-800 overflow-hidden transition-colors">
    <div class="flex justify-between items-center border-b border-gray-200 dark:border-gray-800 bg-gray-100/50 dark:bg-gray-900 pr-2 flex-shrink-0 h-[35px]" :class="{ 'border-b-0': collapsed }">
      <div class="flex h-full items-center">
          <button @click="activeTab = 'itemstack'" class="px-4 h-full text-xs font-bold border-r dark:border-gray-800 flex items-center gap-2 transition-colors focus:outline-none uppercase" :class="activeTab === 'itemstack' ? 'bg-white dark:bg-gray-800 text-blue-600 dark:text-blue-400 border-b-2 border-b-blue-600 dark:border-b-blue-400' : 'text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800/50'">
            <Layers :size="14" /> Item Stack <span v-if="dms.itemStack?.length" class="bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 px-1.5 rounded-full text-[10px] ml-1">{{ dms.itemStack.length }}</span>
          </button>
          <button @click="activeTab = 'logview'" class="px-4 h-full text-xs font-bold border-r dark:border-gray-800 flex items-center transition-colors focus:outline-none uppercase" :class="activeTab === 'logview' ? 'bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-200 border-b-2 border-b-gray-800 dark:border-b-gray-400' : 'text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-800/50'">
            Console
          </button>
      </div>
      <button @click="$emit('toggle-minimize')" class="p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-500 dark:text-gray-400 transition-colors"><PanelBottomOpen v-if="collapsed" :size="18" /><PanelBottomClose v-else :size="18" /></button>
    </div>
    
    <div v-if="!collapsed" class="flex-1 overflow-hidden relative">
      
      <!-- ITEM STACK -->
      <div v-if="activeTab === 'itemstack'" class="h-full flex flex-col bg-gray-50 dark:bg-black/20 relative" @dragover.prevent="isDragOverStack = true" @dragleave.prevent="isDragOverStack = false" @drop.prevent="onStackDrop">
          
          <div v-if="isDragOverStack" class="absolute inset-0 bg-blue-100/50 dark:bg-blue-900/50 z-50 border-2 border-blue-400 border-dashed m-2 rounded flex items-center justify-center text-blue-600 dark:text-blue-300 font-bold pointer-events-none">
              Drop here to stack
          </div>

          <div class="p-2 flex justify-between items-center border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900">
              <span class="text-[11px] text-gray-400 uppercase tracking-wider">Drag items here to move them between folders</span>
              <button v-if="dms.itemStack?.length" @click="dms.clearStack" class="text-xs text-red-500 hover:underline flex items-center gap-1"><Trash2 :size="12"/> Clear All</button>
          </div>

          <div class="flex-1 overflow-auto p-2 flex flex-wrap gap-2 content-start">
              <div v-if="!dms.itemStack?.length" class="w-full h-full flex flex-col items-center justify-center text-gray-300 dark:text-gray-700">
                  <Layers :size="32" class="mb-2 opacity-20" />
                  <span class="text-sm">Stack is empty</span>
              </div>
              
              <div 
                v-for="item in dms.itemStack" 
                :key="item.uuid" 
                class="w-[200px] bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded shadow-sm p-2 cursor-grab active:cursor-grabbing hover:border-blue-300 dark:hover:border-blue-700 hover:shadow-md transition-all group relative"
                draggable="true"
                @dragstart="onDragStartStackItem($event, item)"
              >
                 <div class="font-medium text-xs text-gray-800 dark:text-gray-200 truncate mb-1">{{ item.name }}</div>
                 <div class="flex justify-between items-center">
                    <span class="text-[10px] text-gray-400 dark:text-gray-500">{{ new Date(item.issueDate || '').toLocaleDateString() }}</span>
                    <button v-if="item.uuid" @click="dms.removeFromStack(item.uuid)" class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-red-500 transition-opacity p-1"><Trash2 :size="12"/></button>
                 </div>
              </div>
          </div>
      </div>

      <!-- CONSOLE LOG -->
      <div v-else-if="activeTab === 'logview'" class="h-full flex flex-col bg-[#1e1e1e] font-mono text-xs overflow-hidden text-gray-300">
        
        <div class="flex items-center gap-2 px-2 py-1 border-b border-[#333] bg-[#252526] select-none shrink-0 h-[30px]">
            <button @click="filters.error = !filters.error" class="log-filter-btn text-red-400 border-red-900/30" :class="filters.error ? 'bg-red-400/10 border-red-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                <AlertCircle :size="11" /> {{ ui.logs.filter(l => l.type === 'error').length }} Errors
            </button>
            
            <button @click="filters.warning = !filters.warning" class="log-filter-btn text-yellow-400 border-yellow-900/30" :class="filters.warning ? 'bg-yellow-400/10 border-yellow-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                <AlertTriangle :size="11" /> Warnings
            </button>

            <button @click="filters.success = !filters.success" class="log-filter-btn text-green-400 border-green-900/30" :class="filters.success ? 'bg-green-400/10 border-green-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                <CheckCircle :size="11" /> Success
            </button>
            <button @click="filters.info = !filters.info" class="log-filter-btn text-blue-400 border-blue-900/30" :class="filters.info ? 'bg-blue-400/10 border-blue-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                <Info :size="11" /> Info
            </button>
            <button @click="filters.debug = !filters.debug" class="log-filter-btn text-gray-400 border-gray-700" :class="filters.debug ? 'bg-gray-400/10 border-gray-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                <Bug :size="11" /> Debug
            </button>
            
            <div class="flex-1"></div>
            <button @click="clearLogs" class="flex items-center gap-1.5 px-2 py-0.5 rounded border border-red-900 text-red-400 hover:text-white hover:bg-red-900/50 transition-colors font-bold" title="Clear Console">
                <Trash2 :size="12" /> Clear
            </button>
        </div>

        <div class="flex-1 overflow-auto p-2">
            <div v-for="log in filteredLogs" :key="log.id" class="flex gap-2 mb-0.5 hover:bg-white/5 px-1 py-0.5 rounded leading-snug group">
                <span class="text-gray-500 shrink-0 select-none w-[60px] text-[11px] opacity-70 group-hover:opacity-100">{{ log.time }}</span>
                <span class="font-bold shrink-0 w-[55px] uppercase text-[10px] py-0.5 tracking-wider select-none" 
                      :class="{
                        'text-red-500': log.type === 'error',
                        'text-yellow-500': log.type === 'warning',
                        'text-green-500': log.type === 'success',
                        'text-blue-500': log.type === 'info',
                        'text-gray-600': log.type === 'debug'
                      }">
                    {{ log.type }}
                </span>
                <span class="break-all whitespace-pre-wrap flex-1"
                      :class="{
                        'text-red-300': log.type === 'error',
                        'text-yellow-300': log.type === 'warning',
                        'text-green-300': log.type === 'success',
                        'text-gray-300': log.type === 'info',
                        'text-gray-500': log.type === 'debug'
                      }">{{ log.message }}</span>
            </div>
            
            <div v-if="filteredLogs.length === 0" class="h-full flex flex-col items-center justify-center text-gray-600 italic">
                <span v-if="ui.logs.length > 0">No logs match current filters</span>
                <span v-else>Console is empty</span>
            </div>
        </div>
      </div>
      
    </div>
  </div>
</template>

<style scoped>
.log-filter-btn { @apply flex items-center gap-1.5 px-2 py-0.5 rounded border text-[10px] font-medium transition-all; }
</style>