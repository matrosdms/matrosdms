<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  FileText, GripVertical, Loader2, Ban, AlertTriangle, Folder,
  Calendar, Tag, Mail, RefreshCw, Sparkles, ArrowRight, Link2
} from 'lucide-vue-next'
import { useDragDrop } from '@/composables/useDragDrop'
import { useMatrosData } from '@/composables/useMatrosData'
import type { InboxFile } from '@/types/events'

// ============================================================================
// TYPES
// ============================================================================

interface InboxItemProps {
  file: InboxFile
  isProcessing: boolean
  isDuplicate: boolean
}

interface InboxItemEmits {
  (e: 'click'): void
  (e: 'preview'): void
  (e: 'ignore'): void
  (e: 'analyze'): void
  (e: 'assignContext', contextId: string): void
}

interface DroppedContextData {
  type: string
  id: string
  name?: string
}

// ============================================================================
// PROPS & EMITS
// ============================================================================

const props = defineProps<InboxItemProps>()
const emit = defineEmits<InboxItemEmits>()

// ============================================================================
// COMPOSABLES
// ============================================================================

const { startDrag, endDrag } = useDragDrop()
const { contexts } = useMatrosData()

// ============================================================================
// CONSTANTS
// ============================================================================

const CLICK_SUPPRESSION_DELAY = 300 // ms after drop to ignore clicks

const ICON_STYLES = {
  duplicate: 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400',
  processing: 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400',
  email: 'bg-indigo-50 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400',
  default: 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-300',
} as const

const CONTAINER_STYLES = {
  duplicate: 'bg-orange-50/50 dark:bg-orange-900/10 border-orange-200 dark:border-orange-800 opacity-80',
  default: 'bg-white dark:bg-gray-800 border-gray-200 dark:border-gray-700 hover:border-blue-300 dark:hover:border-blue-700 hover:shadow-md',
} as const

// ============================================================================
// STATE
// ============================================================================

const inboxItemRef = ref<HTMLElement | null>(null)
const isDragOver = ref(false)
const lastDropTime = ref(0)

// ============================================================================
// COMPUTED - DISPLAY
// ============================================================================

const displayName = computed(() => 
  props.file.displayName || 
  props.file.fileInfo.originalFilename || 
  'Unknown'
)

const subTitle = computed(() => {
  if (props.file.emailInfo?.sender) {
    return `From: ${props.file.emailInfo.sender}`
  }
  return ''
})

const isEmail = computed(() => 
  props.file.source === 'EMAIL' || 
  props.file.fileInfo.extension === '.eml'
)

const progressLabel = computed(() => 
  props.file.progressMessage || 'Processing...'
)

// ============================================================================
// COMPUTED - PREDICTION
// ============================================================================

const prediction = computed(() => props.file.prediction)

const isManuallyAssigned = computed(() => 
  !!prediction.value?.manuallyAssigned
)

const contextName = computed(() => {
  const ctxId = prediction.value?.context
  if (!ctxId) return null
  
  const ctx = contexts.value.find((c: any) => c.uuid === ctxId)
  return ctx?.name || null
})

const categoryName = computed(() => prediction.value?.category)

// ============================================================================
// COMPUTED - STYLING
// ============================================================================

const iconContainerClass = computed(() => {
  if (props.isDuplicate) return ICON_STYLES.duplicate
  if (props.isProcessing) return ICON_STYLES.processing
  if (isEmail.value) return ICON_STYLES.email
  return ICON_STYLES.default
})

const containerClass = computed(() => {
  if (props.isDuplicate) return CONTAINER_STYLES.duplicate
  return CONTAINER_STYLES.default
})

const hasPrediction = computed(() => 
  (prediction.value || isManuallyAssigned.value) && 
  !props.isDuplicate && 
  !props.isProcessing
)

const showAIReady = computed(() => 
  !props.isProcessing && 
  !props.isDuplicate && 
  prediction.value
)

// ============================================================================
// DRAG & DROP - SOURCE
// ============================================================================

// CRITICAL FIX: Allow dragging even during processing for error recovery
const isDraggable = computed(() => !props.isDuplicate)

const handleDragStart = (event: DragEvent) => {
  if (!isDraggable.value) {
    event.preventDefault()
    return
  }
  
  const payload = {
    sha256: props.file.sha256,
    name: displayName.value,
    prediction: props.file.prediction || null
  }
  
  startDrag(event, 'inbox-file', payload)
}

const handleDragEnd = () => {
  endDrag()
}

// ============================================================================
// DRAG & DROP - TARGET
// ============================================================================

const canAcceptDrop = computed(() => 
  !props.isProcessing && !props.isDuplicate
)

const handleDragEnter = () => {
  if (canAcceptDrop.value) {
    isDragOver.value = true
  }
}

const handleDragLeave = (event: DragEvent) => {
  const relatedTarget = event.relatedTarget as Node
  if (inboxItemRef.value?.contains(relatedTarget)) return
  
  isDragOver.value = false
}

const handleDrop = (event: DragEvent) => {
  event.stopPropagation()
  isDragOver.value = false
  
  const rawData = event.dataTransfer?.getData('application/json')
  if (!rawData) return

  try {
    const data: DroppedContextData = JSON.parse(rawData)
    
    if (data.type === 'dms-context') {
      lastDropTime.value = Date.now()
      
      // Emit event to parent to update prediction via workflow store
      emit('assignContext', data.id)
    }
  } catch (error) {
    console.error('Failed to parse dropped data:', error)
  }
}

// ============================================================================
// CLICK HANDLING
// ============================================================================

const handleClick = (event: MouseEvent) => {
  // Suppress click if drop just happened (prevents accidental opens)
  const timeSinceDrop = Date.now() - lastDropTime.value
  if (timeSinceDrop < CLICK_SUPPRESSION_DELAY) {
    event.preventDefault()
    event.stopPropagation()
    return
  }
  
  emit('click')
}

// ============================================================================
// ACTION HANDLERS
// ============================================================================

const handleAnalyze = (event: Event) => {
  event.stopPropagation()
  emit('analyze')
}

const handleIgnore = (event: Event) => {
  event.stopPropagation()
  emit('ignore')
}
</script>

<template>
  <div
    ref="inboxItemRef"
    :draggable="isDraggable"
    class="group relative flex flex-col rounded-xl transition-all border 
           select-none overflow-hidden mb-3 cursor-pointer"
    :class="containerClass"
    @dragstart="handleDragStart"
    @dragend="handleDragEnd"
    @dragenter.prevent="handleDragEnter"
    @dragleave="handleDragLeave"
    @dragover.prevent
    @drop.prevent.stop="handleDrop"
    @click="handleClick"
  >
    <!-- Drop Overlay -->
    <div
      v-if="isDragOver"
      class="absolute inset-0 z-50 bg-blue-50/95 dark:bg-gray-800/95 
             flex flex-col items-center justify-center border-2 border-blue-500 
             rounded-xl animate-in fade-in duration-150 cursor-copy"
    >
      <Folder class="text-blue-500 mb-2" :size="32" />
      <span class="text-sm font-bold text-blue-700 dark:text-blue-300 uppercase tracking-wider">
        Assign Context
      </span>
    </div>

    <!-- Header: Source Badge & Actions -->
    <div class="flex justify-between items-start px-4 pt-3 pb-1">
      <!-- Source Badge -->
      <div class="flex items-center gap-2">
        <span
          v-if="isEmail"
          class="text-[10px] font-bold px-1.5 py-0.5 rounded-md 
                 bg-indigo-50 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 
                 border border-indigo-100 dark:border-indigo-800 uppercase tracking-wide 
                 flex items-center gap-1"
        >
          <Mail :size="10" /> Email
        </span>
        <span
          v-else
          class="text-[10px] font-bold px-1.5 py-0.5 rounded-md 
                 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 
                 border border-gray-200 dark:border-gray-600 uppercase tracking-wide 
                 flex items-center gap-1"
        >
          <GripVertical :size="10" /> Inbox
        </span>
      </div>

      <!-- Action Buttons -->
      <div
        v-if="!isDuplicate"
        class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
      >
        <button
          v-if="!isProcessing"
          class="p-1.5 hover:bg-blue-50 text-gray-400 hover:text-blue-600 
                 dark:hover:bg-blue-900/30 dark:hover:text-blue-400 rounded-md 
                 transition-colors"
          title="Re-run AI Analysis"
          @click="handleAnalyze"
        >
          <Sparkles :size="16" />
        </button>

        <button
          class="p-1.5 hover:bg-red-50 text-gray-400 hover:text-red-600 
                 dark:hover:bg-red-900/30 dark:hover:text-red-400 rounded-md 
                 transition-colors"
          title="Ignore"
          @click="handleIgnore"
        >
          <Ban :size="16" />
        </button>
      </div>
    </div>

    <!-- Body: Icon & Title -->
    <div class="flex items-start gap-3 px-4 py-2">
      <!-- Status Icon -->
      <div class="shrink-0 mt-0.5">
        <div
          class="flex items-center justify-center w-10 h-10 rounded-full 
                 transition-colors border border-transparent dark:border-white/5 shadow-sm"
          :class="iconContainerClass"
        >
          <Loader2 v-if="isProcessing" class="animate-spin" :size="20" />
          <AlertTriangle v-else-if="isDuplicate" class="animate-pulse" :size="20" />
          <Mail v-else-if="isEmail" :size="20" />
          <FileText v-else :size="20" />
        </div>
      </div>

      <!-- Title & Subtitle -->
      <div class="flex-1 min-w-0">
        <span
          class="text-sm font-semibold text-gray-800 dark:text-gray-100 
                 leading-snug break-words block hover:text-blue-600 
                 dark:hover:text-blue-400 hover:underline transition-colors"
          :title="displayName"
        >
          {{ displayName }}
        </span>

        <div
          v-if="subTitle && !isProcessing"
          class="text-xs text-muted-foreground truncate"
        >
          {{ subTitle }}
        </div>

        <!-- Status Messages -->
        <div
          v-if="isProcessing || isDuplicate"
          class="mt-1 flex items-center gap-2 text-xs font-mono"
        >
          <span
            v-if="isProcessing"
            class="text-blue-600 dark:text-blue-400 flex items-center gap-1.5"
          >
            <RefreshCw :size="10" class="animate-spin" /> {{ progressLabel }}
          </span>
          <span
            v-else-if="isDuplicate"
            class="text-orange-600 dark:text-orange-400 font-bold uppercase"
          >
            Duplicate
          </span>
        </div>

        <!-- AI Ready Badge -->
        <div
          v-else-if="showAIReady"
          class="mt-1 text-[10px] text-green-600 dark:text-green-400 
                 font-bold flex items-center gap-1"
        >
          <Sparkles :size="10" /> AI Ready
        </div>
      </div>
    </div>

    <!-- Footer: Prediction Card -->
    <div v-if="hasPrediction" class="px-4 pb-4 mt-2">
      <div
        class="bg-gray-50 dark:bg-gray-900/50 rounded-lg border border-gray-100 
               dark:border-gray-800 p-2.5 flex flex-col gap-2 transition-colors 
               hover:border-gray-300 dark:hover:border-gray-700"
        :class="{
          'border-blue-200 dark:border-blue-800 bg-blue-50/50 dark:bg-blue-900/20': 
            isManuallyAssigned
        }"
      >
        <!-- Context Row -->
        <div class="flex items-center gap-2 w-full text-sm font-medium text-gray-700 dark:text-gray-300">
          <Link2
            v-if="isManuallyAssigned"
            :size="14"
            class="shrink-0 text-blue-500 dark:text-blue-400"
          />
          <Folder
            v-else
            :size="14"
            class="shrink-0 text-gray-400 dark:text-gray-500"
          />
          <span
            class="truncate flex-1"
            :class="{ 'text-blue-700 dark:text-blue-300': isManuallyAssigned }"
          >
            {{ contextName || 'Unassigned' }}
          </span>
          <ArrowRight v-if="contextName" :size="12" class="text-gray-300" />
        </div>

        <!-- Metadata Row -->
        <div
          class="flex items-center justify-between text-xs text-muted-foreground 
                 pt-2 border-t border-gray-200 dark:border-gray-700 border-dashed"
        >
          <!-- Category -->
          <div class="flex items-center gap-1.5 overflow-hidden max-w-[65%]">
            <Tag :size="12" class="shrink-0 opacity-70" />
            <span class="truncate">{{ categoryName || 'General' }}</span>
          </div>

          <!-- Document Date -->
          <div
            v-if="prediction?.documentDate"
            class="flex items-center gap-1.5 shrink-0"
          >
            <Calendar :size="12" class="shrink-0 opacity-70" />
            <span class="font-mono">{{ prediction.documentDate }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>