<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Layers, X, Trash2, Maximize2, Minimize2, GripHorizontal, CheckSquare, Square, Folder } from 'lucide-vue-next'
import { useDmsStore } from '@/stores/dms'
import { useDragDrop } from '@/composables/useDragDrop'
import { useDraggable } from '@vueuse/core'

const dms = useDmsStore()
const { startDrag } = useDragDrop()

const isOpen = ref(false)
const isExpanded = ref(false)
const dragWindowRef = ref<HTMLElement | null>(null)
const dragHandleRef = ref<HTMLElement | null>(null)
const isDragOver = ref(false)

// Selection State
const selectedIds = ref(new Set<string>())

// Draggable Window Logic
const { style } = useDraggable(dragWindowRef, {
  initialValue: { x: 90, y: window.innerHeight - 300 },
  preventDefault: true,
  handle: dragHandleRef 
})

const toggle = () => isOpen.value = !isOpen.value

const safeStack = computed(() => dms.itemStack || [])

const isAllSelected = computed(() => {
    return safeStack.value.length > 0 && selectedIds.value.size === safeStack.value.length
})

const isIndeterminate = computed(() => {
    return selectedIds.value.size > 0 && selectedIds.value.size < safeStack.value.length
})

const toggleSelectAll = () => {
    if (isAllSelected.value) {
        selectedIds.value.clear()
    } else {
        safeStack.value.forEach(i => { if (i.uuid) selectedIds.value.add(i.uuid) })
    }
}

const toggleSelection = (uuid: string) => {
    if (selectedIds.value.has(uuid)) selectedIds.value.delete(uuid)
    else selectedIds.value.add(uuid)
}

const onDrop = (event: DragEvent) => {
    isDragOver.value = false
    const raw = event.dataTransfer?.getData('application/json')
    if (raw) {
        try {
            const data = JSON.parse(raw)
            if (data.type === 'dms-item') {
                dms.addToStack(data)
                if (!isOpen.value) isOpen.value = true
            }
        } catch(e) {}
    }
}

const onDragStartStackItem = (event: DragEvent, item: any) => {
    event.stopPropagation()
    if (item.uuid && selectedIds.value.has(item.uuid)) {
        const payload = {
            itemUuids: Array.from(selectedIds.value),
            count: selectedIds.value.size
        }
        startDrag(event, 'dms-batch', payload)
    } else {
        startDrag(event, 'dms-item', item)
    }
}

const onDragStartHeader = (event: DragEvent) => {
    if (!safeStack.value.length) {
        event.preventDefault()
        return
    }
    event.stopPropagation()
    const allIds = safeStack.value.map(i => i.uuid).filter(Boolean) as string[]
    const payload = { itemUuids: allIds, count: allIds.length }
    startDrag(event, 'dms-batch', payload)
}

// FIX: Added safety check (dms.itemStack || []) to prevent undefined map error
watch(() => dms.itemStack?.length, () => {
    const stack = dms.itemStack || []
    
    if (stack.length === 0) {
        selectedIds.value.clear()
    } else {
        const currentIds = new Set(stack.map(i => i.uuid))
        for (const id of selectedIds.value) {
            if (!currentIds.has(id)) selectedIds.value.delete(id)
        }
    }
})
</script>

<template>
  <Teleport to="body">
    <!-- Floating Trigger -->
    <div 
        v-if="!isOpen" 
        class="fixed bottom-6 left-20 z-[990] flex items-end gap-2"
    >
        <button 
            @click="toggle" 
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="onDrop"
            class="p-3 rounded-full shadow-lg hover:scale-110 transition-transform active:scale-95 animate-in fade-in border border-white/10 relative"
            :class="isDragOver ? 'bg-green-600 text-white scale-110' : 'bg-white dark:bg-gray-700 text-gray-700 dark:text-white border-gray-300 dark:border-gray-600 shadow-md'"
            title="Item Stack / Clipboard"
        >
            <Layers :size="24" />
            <span v-if="safeStack.length" class="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold w-5 h-5 flex items-center justify-center rounded-full border-2 border-background">
                {{ safeStack.length }}
            </span>
        </button>
    </div>

    <!-- Stack Window -->
    <div 
        v-if="isOpen"
        ref="dragWindowRef"
        class="fixed z-[1000] bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 rounded-xl shadow-2xl overflow-hidden flex flex-col transition-all duration-200"
        :class="isExpanded ? 'w-[600px] h-[500px]' : 'w-[320px] h-[450px]'"
        :style="style"
    >
        <!-- Header -->
        <div 
            ref="dragHandleRef"
            class="p-3 bg-gray-100 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center cursor-move text-gray-700 dark:text-gray-200 shrink-0 select-none"
        >
            <div class="flex items-center gap-2">
                <div 
                    class="p-1.5 rounded bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 shadow-sm cursor-grab active:cursor-grabbing hover:border-blue-400 hover:text-blue-500 transition-colors"
                    draggable="true"
                    @dragstart="onDragStartHeader"
                    @mousedown.stop
                    title="Drag to move ALL items"
                >
                    <Layers :size="16" />
                </div>
                
                <button 
                    @click="toggleSelectAll" 
                    @mousedown.stop
                    class="flex items-center gap-1 text-xs font-bold hover:text-primary transition-colors"
                    :class="selectedIds.size > 0 ? 'text-primary' : 'text-muted-foreground'"
                >
                    <div class="relative">
                        <CheckSquare v-if="isAllSelected" :size="16" />
                        <div v-else-if="isIndeterminate" class="w-4 h-4 border border-current rounded flex items-center justify-center">
                            <div class="w-2 h-2 bg-current rounded-sm"></div>
                        </div>
                        <Square v-else :size="16" />
                    </div>
                    <span>{{ selectedIds.size > 0 ? `${selectedIds.size} Selected` : 'Select All' }}</span>
                </button>
            </div>

            <div class="flex items-center gap-1">
                <button v-if="safeStack.length" @click="dms.clearStack" class="p-1 hover:bg-red-100 dark:hover:bg-red-900/30 text-gray-400 hover:text-red-500 rounded transition-colors mr-1" @mousedown.stop title="Clear List"><Trash2 :size="14"/></button>
                <div class="flex bg-gray-200 dark:bg-gray-700 rounded-md" @mousedown.stop>
                    <button @click="isExpanded = !isExpanded" class="p-1 hover:bg-gray-300 dark:hover:bg-gray-600 rounded"><component :is="isExpanded ? Minimize2 : Maximize2" :size="14" /></button>
                    <button @click="toggle" class="p-1 hover:bg-gray-300 dark:hover:bg-gray-600 rounded"><X :size="14" /></button>
                </div>
            </div>
        </div>

        <!-- Drop Zone & List -->
        <div 
            class="flex-1 overflow-y-auto p-2 bg-gray-50 dark:bg-black/20 relative cursor-default"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="onDrop"
            @mousedown.stop
        >
            <div v-if="isDragOver" class="absolute inset-0 bg-blue-100/50 dark:bg-blue-900/50 z-50 border-2 border-blue-400 border-dashed m-2 rounded flex items-center justify-center text-blue-600 dark:text-blue-300 font-bold pointer-events-none backdrop-blur-[1px]">
                Drop to Stack
            </div>

            <div v-if="!safeStack.length" class="h-full flex flex-col items-center justify-center text-gray-400 text-center p-4">
                <Layers :size="32" class="mb-2 opacity-30" />
                <p class="text-xs">Drag items here to collect them.</p>
            </div>
            
            <div v-else class="space-y-1.5">
                <div 
                    v-for="item in safeStack" 
                    :key="item.uuid" 
                    class="group flex items-center gap-2 bg-white dark:bg-gray-800 border rounded shadow-sm p-2 cursor-grab active:cursor-grabbing hover:border-blue-300 dark:hover:border-blue-700 transition-all select-none"
                    :class="selectedIds.has(item.uuid || '') ? 'border-blue-400 ring-1 ring-blue-400/30' : 'border-gray-200 dark:border-gray-700'"
                    draggable="true"
                    @dragstart="onDragStartStackItem($event, item)"
                    @click="toggleSelection(item.uuid || '')"
                >
                    <div class="shrink-0 text-gray-400" :class="selectedIds.has(item.uuid || '') ? 'text-blue-600' : 'group-hover:text-gray-600'">
                        <CheckSquare v-if="selectedIds.has(item.uuid || '')" :size="16" />
                        <Square v-else :size="16" />
                    </div>

                    <GripHorizontal :size="12" class="text-gray-300 shrink-0" />
                    
                    <div class="flex-1 min-w-0 flex flex-col gap-0.5">
                        <div class="font-medium text-xs text-gray-800 dark:text-gray-200 truncate">{{ item.name }}</div>
                        <div class="text-[10px] text-gray-400 dark:text-gray-500 flex items-center justify-between">
                            <span>{{ new Date(item.issueDate || '').toLocaleDateString() }}</span>
                            <span v-if="item.context?.name" class="flex items-center gap-1 opacity-75 truncate max-w-[120px]">
                                <Folder :size="10" /> {{ item.context.name }}
                            </span>
                        </div>
                    </div>

                    <button @click.stop="dms.removeFromStack(item.uuid || '')" class="p-1 text-gray-300 hover:text-red-500 transition-colors opacity-0 group-hover:opacity-100"><X :size="14"/></button>
                </div>
            </div>
        </div>
    </div>
  </Teleport>
</template>