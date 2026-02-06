<script setup lang="ts">
import { ref, watch, onUnmounted, computed } from 'vue'
import { ItemService } from '@/services/ItemService'
import { InboxService } from '@/services/InboxService'
import { Loader2, FileX, Download, ExternalLink, Mail, FileText } from 'lucide-vue-next'
import { useQuery } from '@tanstack/vue-query'
import { usePreferencesStore } from '@/stores/preferences'
import EmailViewer from '@/components/viewers/EmailViewer.vue'

const props = defineProps<{
  identifier: string; // UUID or Hash
  source: 'item' | 'inbox';
  fileName?: string; // NEW: Passed from parent to ensure correct download name
}>()

const prefs = usePreferencesStore()
const blobUrl = ref<string | null>(null)

// --- CACHED QUERY ---
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
    retry: 1
})

// --- URL MANAGEMENT ---
watch(data, (newData) => {
    if (blobUrl.value) {
        window.URL.revokeObjectURL(blobUrl.value)
        blobUrl.value = null
    }
    if (newData && newData.blob) {
        blobUrl.value = window.URL.createObjectURL(newData.blob)
    }
}, { immediate: true })

onUnmounted(() => {
    if (blobUrl.value) window.URL.revokeObjectURL(blobUrl.value)
})

// --- MEDIA TYPE DETECTION ---
const mimeType = computed(() => data.value?.type?.toLowerCase() || '')

const isPdf = computed(() => mimeType.value === 'application/pdf')
const isImage = computed(() => mimeType.value.startsWith('image/'))
const isText = computed(() => mimeType.value.startsWith('text/') || mimeType.value === 'application/json' || mimeType.value.includes('xml'))
const isEmail = computed(() => 
    mimeType.value === 'message/rfc822' || 
    mimeType.value === 'application/vnd.ms-outlook' || 
    (props.identifier && props.identifier.endsWith && (props.identifier.endsWith('.eml')))
)

const isSupported = computed(() => isPdf.value || isImage.value || isText.value || isEmail.value)

const openExternal = () => {
    if(blobUrl.value) window.open(blobUrl.value, '_blank')
}

// NEW: Force browser to use the correct filename
const downloadFile = () => {
    if (!blobUrl.value) return
    const a = document.createElement('a')
    a.href = blobUrl.value
    a.download = props.fileName || 'document' // Enforce filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
}
</script>

<template>
  <div class="w-full h-full bg-gray-100 dark:bg-gray-950 flex flex-col overflow-hidden relative">
      <!-- Loading State -->
      <div v-if="isLoading" class="absolute inset-0 flex flex-col items-center justify-center bg-gray-50/80 dark:bg-black/80 z-10">
          <Loader2 class="animate-spin text-blue-500 mb-2" :size="32" />
          <span class="text-xs text-gray-500 font-medium">Fetching content...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="flex-1 flex flex-col items-center justify-center text-center p-6">
          <FileX class="text-red-300 mb-3" :size="48" />
          <p class="text-sm text-red-600 font-bold">Could not preview document</p>
          <p class="text-xs text-gray-500 mt-1">{{ (error as Error).message }}</p>
      </div>

      <!-- Content Viewers -->
      <template v-else-if="data?.blob">
          
          <!-- 1. Email Viewer -->
          <EmailViewer v-if="isEmail" :blob="data.blob" />

          <!-- 2. PDF Viewer (iframe for sandboxed rendering - security) -->
          <iframe 
            v-else-if="isPdf && blobUrl" 
            :src="blobUrl" 
            class="w-full h-full border-0 block bg-white dark:bg-gray-900" 
            type="application/pdf"
            :style="{ colorScheme: prefs.isDarkMode ? 'dark' : 'light' }"
          ></iframe>
          
          <!-- 3. Image Viewer -->
          <div v-else-if="isImage && blobUrl" class="w-full h-full flex items-center justify-center bg-gray-200 dark:bg-black overflow-auto p-4">
              <img :src="blobUrl" class="max-w-full max-h-full object-contain shadow-md" />
          </div>
          
          <!-- 4. Text/Code Viewer -->
          <iframe 
            v-else-if="isText && blobUrl" 
            :src="blobUrl" 
            class="w-full h-full border-0 bg-white"
            :style="{ colorScheme: prefs.isDarkMode ? 'dark' : 'light' }"
          ></iframe>
          
          <!-- 5. Fallback -->
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

          <!-- Floating Actions (Bottom Right) -->
          <div v-if="!isEmail" class="absolute bottom-6 right-6 z-20 flex gap-2">
              <!-- NEW: Explicit Download Button -->
              <button @click="downloadFile" class="bg-blue-600 hover:bg-blue-700 text-white p-2 rounded-full shadow-lg backdrop-blur-sm transition-transform hover:scale-105" title="Download">
                  <Download :size="18" />
              </button>
              
              <button @click="openExternal" class="bg-black/75 hover:bg-black text-white p-2 rounded-full shadow-lg backdrop-blur-sm transition-transform hover:scale-105" title="Open in New Tab">
                  <ExternalLink :size="18" />
              </button>
          </div>
      </template>

      <!-- Empty State -->
      <div v-else class="flex-1 flex items-center justify-center text-gray-400">
          No Document Selected
      </div>
  </div>
</template>