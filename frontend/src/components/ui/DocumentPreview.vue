<script setup lang="ts">
import { ref, watch, onUnmounted, computed } from 'vue'
import { ItemService } from '@/services/ItemService'
import { InboxService } from '@/services/InboxService'
import { Loader2, FileX, Download, ExternalLink, FileText, AlignLeft, Image as ImageIcon } from 'lucide-vue-next'
import { useQuery } from '@tanstack/vue-query'
import { push } from 'notivue'

// ── Specialised viewers ──────────────────────────────────────────────────
import PdfViewer from '@/components/viewers/PdfViewer.vue'
import ImageViewer from '@/components/viewers/ImageViewer.vue'
import TextViewer from '@/components/viewers/TextViewer.vue'
import EmailViewer from '@/components/viewers/EmailViewer.vue'

// ── Props ────────────────────────────────────────────────────────────────
const props = defineProps<{
  identifier: string   // UUID or Hash
  source: 'item' | 'inbox'
  fileName?: string
}>()

// ── State ────────────────────────────────────────────────────────────────
const blobUrl = ref<string | null>(null)
const viewMode = ref<'file' | 'text'>('file')
const textContent = ref<string | null>(null)
const isFetchingText = ref(false)

// ── Data query (Binary Content) ──────────────────────────────────────────
const { data, isLoading, error } = useQuery({
  queryKey: computed(() => ['content', props.source, props.identifier]),
  queryFn: async () => {
    if (!props.identifier) return null
    if (props.source === 'inbox') return InboxService.getFileBlob(props.identifier)
    return ItemService.getDocumentBlob(props.identifier)
  },
  enabled: computed(() => !!props.identifier),
  staleTime: Infinity,
  gcTime: 1000 * 60 * 5,
  retry: 1,
})

// ── Text Layer Fetching ──────────────────────────────────────────────────
const toggleViewMode = async () => {
    if (viewMode.value === 'text') {
        viewMode.value = 'file'
        return
    }

    // Switch to Text
    viewMode.value = 'text'
    
    // Fetch if not already loaded
    if (!textContent.value && props.source === 'item') {
        isFetchingText.value = true
        try {
            const txt = await ItemService.getRawText(props.identifier)
            textContent.value = txt || "No text layer found for this document."
        } catch (e: any) {
            textContent.value = `Error loading text: ${e.message}`
            push.error("Could not fetch text layer")
        } finally {
            isFetchingText.value = false
        }
    } else if (props.source === 'inbox' && !textContent.value) {
        textContent.value = "Text extraction is only available after import."
    }
}

// Reset state when identifier changes
watch(() => props.identifier, () => {
    viewMode.value = 'file'
    textContent.value = null
})

// ── Blob URL lifecycle ───────────────────────────────────────────────────
watch(data, (newData) => {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = null
  }
  if (newData?.blob) {
    blobUrl.value = URL.createObjectURL(newData.blob)
  }
}, { immediate: true })

onUnmounted(() => {
  if (blobUrl.value) URL.revokeObjectURL(blobUrl.value)
})

// ── MIME detection ───────────────────────────────────────────────────────
const mimeType = computed(() => data.value?.type?.toLowerCase() || '')

const isPdf   = computed(() => mimeType.value === 'application/pdf')
const isImage = computed(() => mimeType.value.startsWith('image/'))
const isText  = computed(() =>
  mimeType.value.startsWith('text/') ||
  mimeType.value === 'application/json' ||
  mimeType.value.includes('xml'),
)
const isEmail = computed(() =>
  mimeType.value === 'message/rfc822' ||
  mimeType.value === 'application/vnd.ms-outlook' ||
  props.fileName?.endsWith('.eml'),
)

// ── Shared actions ───────────────────────────────────────────────────────
const openExternal = () => {
  if (blobUrl.value) window.open(blobUrl.value, '_blank')
}

const downloadFile = () => {
  if (!blobUrl.value) return
  const a = document.createElement('a')
  a.href = blobUrl.value
  a.download = props.fileName || 'document'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}
</script>

<template>
  <div class="w-full h-full bg-gray-100 dark:bg-gray-950 flex flex-col overflow-hidden relative group/preview">

    <!-- 1. LOADING -->
    <div v-if="isLoading" class="absolute inset-0 flex flex-col items-center justify-center bg-gray-50/80 dark:bg-black/80 z-20">
      <Loader2 class="animate-spin text-blue-500 mb-2" :size="32" />
      <span class="text-xs text-gray-500 font-medium">Fetching content...</span>
    </div>

    <!-- 2. ERROR -->
    <div v-else-if="error" class="flex-1 flex flex-col items-center justify-center text-center p-6">
      <FileX class="text-red-300 mb-3" :size="48" />
      <p class="text-sm text-red-600 font-bold">Could not preview document</p>
      <p class="text-xs text-gray-500 mt-1">{{ (error as Error).message }}</p>
    </div>

    <!-- 3. VIEW MODE: TEXT LAYER -->
    <div v-else-if="viewMode === 'text'" class="flex-1 overflow-hidden relative">
        <div v-if="isFetchingText" class="absolute inset-0 flex items-center justify-center bg-white/50 dark:bg-black/50 z-10">
            <Loader2 class="animate-spin text-primary" :size="24" />
        </div>
        <TextViewer :text-content="textContent" />
    </div>

    <!-- 4. VIEW MODE: FILE (Default) -->
    <template v-else-if="data?.blob && blobUrl">
      <EmailViewer v-if="isEmail" :blob="data.blob" />
      <PdfViewer   v-else-if="isPdf"   :blob-url="blobUrl" />
      <ImageViewer v-else-if="isImage" :blob-url="blobUrl" />
      <TextViewer  v-else-if="isText"  :blob-url="blobUrl" />

      <!-- Fallback — download prompt -->
      <div v-else class="flex-1 flex flex-col items-center justify-center p-6 text-center">
        <div class="bg-white dark:bg-gray-900 p-8 rounded-lg shadow-sm border border-gray-200 dark:border-gray-800 max-w-sm">
          <div class="w-12 h-12 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4 text-gray-400">
            <FileText :size="24" />
          </div>
          <p class="text-sm font-bold text-gray-700 dark:text-gray-200 mb-2">Preview not available</p>
          <p class="text-xs text-gray-500 dark:text-gray-400 mb-6 font-mono bg-gray-50 dark:bg-gray-800 py-1 rounded border dark:border-gray-700">
            {{ mimeType }}
          </p>
          <button @click="downloadFile" class="w-full flex items-center justify-center gap-2 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors text-xs font-bold">
            <Download :size="16" /> Download File
          </button>
        </div>
      </div>
    </template>

    <!-- 5. EMPTY -->
    <div v-else class="flex-1 flex items-center justify-center text-gray-400">
      No Document Selected
    </div>

    <!-- FLOATING ACTION BAR -->
    <div v-if="blobUrl || textContent" class="absolute bottom-6 right-6 z-30 flex gap-2 transition-opacity opacity-0 group-hover/preview:opacity-100">
        
        <!-- Toggle Text/File -->
        <button 
            v-if="source === 'item' && !isEmail" 
            @click="toggleViewMode" 
            class="p-2 rounded-full shadow-lg backdrop-blur-sm transition-transform hover:scale-105 border border-white/10"
            :class="viewMode === 'text' ? 'bg-blue-600 text-white' : 'bg-black/75 hover:bg-black text-white'"
            :title="viewMode === 'text' ? 'View Original File' : 'View Text Layer'"
        >
            <component :is="viewMode === 'text' ? ImageIcon : AlignLeft" :size="18" />
        </button>

        <button @click="downloadFile" class="bg-black/75 hover:bg-black text-white p-2 rounded-full shadow-lg backdrop-blur-sm transition-transform hover:scale-105" title="Download">
          <Download :size="18" />
        </button>
        
        <button v-if="viewMode === 'file'" @click="openExternal" class="bg-black/75 hover:bg-black text-white p-2 rounded-full shadow-lg backdrop-blur-sm transition-transform hover:scale-105" title="Open in New Tab">
          <ExternalLink :size="18" />
        </button>
    </div>
  </div>
</template>