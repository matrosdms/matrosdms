<script setup lang="ts">
import { computed, ref } from 'vue'
import { UploadCloud } from 'lucide-vue-next'
import { push } from 'notivue'
import AppTag from '@/components/ui/AppTag.vue'

const props = withDefaults(defineProps<{
  dimension: string
  label: string
  tags?: { id: string; label: string }[]
  isActive?: boolean
  removable?: boolean
  minHeight?: string
}>(), {
    tags: () => [],
    removable: true,
    minHeight: 'min-h-[42px]'
})

// Ensure tags is always an array
const safeTags = computed(() => props.tags ?? [])

const emit = defineEmits(['remove-tag', 'item-dropped', 'activate'])

const isDragOver = ref(false)

// --- Styling Presets (Shared with ContextFilterBar) ---
const THEMES: Record<string, any> = {
    'WHO':   { 
        container: 'bg-blue-50/50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800',
        active: 'ring-blue-100 dark:ring-blue-800 border-blue-400',
        text: 'text-blue-500 dark:text-blue-400' 
    },
    'WHAT':  { 
        container: 'bg-green-50/50 dark:bg-green-900/10 border-green-200 dark:border-green-800',
        active: 'ring-green-100 dark:ring-green-800 border-green-400',
        text: 'text-green-600 dark:text-green-400' 
    },
    'WHERE': { 
        container: 'bg-orange-50/50 dark:bg-orange-900/10 border-orange-200 dark:border-orange-800',
        active: 'ring-orange-100 dark:ring-orange-800 border-orange-400',
        text: 'text-orange-600 dark:text-orange-400' 
    }
}

const theme = computed(() => THEMES[props.dimension] || { 
    container: 'bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700', 
    active: 'ring-gray-200',
    text: 'text-muted-foreground' 
})

const containerClass = computed(() => {
    const base = `w-full border rounded-md transition-all flex flex-wrap items-center content-start gap-1.5 p-1.5 ${props.minHeight}`
    
    if (isDragOver.value) {
        return `${base} ring-2 ring-primary ring-offset-1 bg-white dark:bg-gray-800 border-primary`
    }
    
    if (props.isActive) {
        return `${base} ${theme.value.container} ${theme.value.active} ring-2 ring-offset-0`
    }
    
    return `${base} ${theme.value.container}`
})

// --- Drop Logic ---
const handleDragEnter = () => {
    isDragOver.value = true
}

const handleDrop = (event: DragEvent) => {
    isDragOver.value = false
    const rawData = event.dataTransfer?.getData('application/json')
    if (!rawData) return

    try {
        const data = JSON.parse(rawData)
        
        // Strict Validation
        if (data.type !== 'category-node') return
        if (data.rootType && data.rootType !== props.dimension) {
            push.warning(`Cannot drop '${data.rootType}' into '${props.dimension}'`)
            return
        }

        emit('item-dropped', data)
    } catch (e) {
        console.error("Drop Parse Error", e)
    }
}
</script>

<template>
  <div class="flex flex-col group w-full">
      <!-- Label -->
      <span class="text-[9px] font-extrabold uppercase mb-1 ml-1 tracking-wider truncate" :class="theme.text">
          {{ label }}
      </span>

      <!-- Drop Zone -->
      <div
        :class="containerClass"
        @click="$emit('activate')"
        @dragover.prevent="handleDragEnter"
        @dragleave="isDragOver = false"
        @drop.prevent="handleDrop"
      >
          <AppTag 
            v-for="tag in safeTags" 
            :key="tag.id" 
            :root-id="dimension" 
            :label="tag.label"
            :removable="removable"
            @remove="$emit('remove-tag', tag.id)"
          />

          <div v-if="safeTags.length === 0" class="flex-1 text-center py-1">
              <span class="text-[10px] text-muted-foreground/40 italic select-none pointer-events-none flex items-center justify-center gap-1">
                  <span v-if="isDragOver">Release to add</span>
                  <span v-else>Drop here</span>
              </span>
          </div>
      </div>
  </div>
</template>