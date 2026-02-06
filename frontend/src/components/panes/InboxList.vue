<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import BasePane from '@/components/ui/BasePane.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import InboxItem from '@/components/panes/InboxItem.vue'
import { useInboxQueries } from '@/composables/queries/useInboxQueries'
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { useServerEvents } from '@/composables/useServerEvents'
import { useDmsStore } from '@/stores/dms'
import { useWorkflowStore } from '@/stores/workflow'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'
import { client } from '@/api/client'
import { InboxService } from '@/services/InboxService'
import { FileText, RefreshCw, Wand2, UploadCloud, Loader2 } from 'lucide-vue-next'
import { push } from 'notivue'
import type { InboxFile } from '@/types/events'

// ============================================================================
// TYPES
// ============================================================================

type InboxFileStatus = 'READY' | 'PROCESSING' | 'ERROR' | 'DUPLICATE'

interface StatusCheckData {
  sha256: string
  status: InboxFileStatus
  progressMessage?: string
}

// ============================================================================
// STORES & COMPOSABLES
// ============================================================================

const dms = useDmsStore()
const ui = useUIStore()
const workflow = useWorkflowStore()
const { inboxFiles, isLoadingInbox, refetchInbox } = useInboxQueries()
const { contexts } = useContextQueries()

useServerEvents()

// ============================================================================
// CONSTANTS
// ============================================================================

const WATCHDOG_CONFIG = {
  interval: 10000, // 10 seconds
  staleThreshold: 10000, // 10 seconds without update
} as const

const CONCURRENT_ANALYSIS_LIMIT = 2

const PROCESSABLE_STATUSES: Set<InboxFileStatus> = new Set(['READY', 'DUPLICATE', 'PROCESSING'])

// ============================================================================
// STATE
// ============================================================================

const searchQuery = ref('')
const ignoredSet = ref(new Set<string>())
const isDragOver = ref(false)
const isUploading = ref(false)

let watchdogTimer: ReturnType<typeof setTimeout> | null = null

// ============================================================================
// FILE MERGING & FILTERING
// ============================================================================

const effectiveFiles = computed(() => {
  const apiFiles = inboxFiles.value || []
  const fileMap = new Map<string, InboxFile>()

  // Build map from API data
  apiFiles.forEach((file: any) => {
    const key = file.sha256
    if (key) {
      fileMap.set(key, { ...file, sha256: key } as InboxFile)
    }
  })

  // Overlay live data
  for (const [hash, liveFile] of Object.entries(workflow.liveInboxFiles)) {
    if (fileMap.has(hash)) {
      fileMap.set(hash, { ...fileMap.get(hash), ...liveFile } as InboxFile)
    } else {
      fileMap.set(hash, liveFile)
    }
  }

  // Filter ignored files
  return Array.from(fileMap.values()).filter(
    file => file.sha256 && !ignoredSet.value.has(file.sha256)
  )
})

const filteredFiles = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return effectiveFiles.value

  return effectiveFiles.value.filter(file => {
    const name = file.emailInfo?.subject || file.fileInfo.originalFilename || ''
    return name.toLowerCase().includes(query)
  })
})

const processableFiles = computed(() => 
  filteredFiles.value.filter(file => !PROCESSABLE_STATUSES.has(file.status))
)

// ============================================================================
// WATCHDOG - STALE FILE DETECTION
// ============================================================================

const checkStaleFiles = async () => {
  const now = Date.now()

  for (const hash of workflow.processingInboxItems) {
    const lastUpdate = workflow.lastInboxUpdate[hash] || 0
    const isStale = now - lastUpdate > WATCHDOG_CONFIG.staleThreshold

    if (!isStale) continue

    try {
      const { data, error } = await client.GET("/api/inbox/{hash}/status", {
        params: { path: { hash } }
      })

      if (error || !data) continue

      const statusData = data as unknown as StatusCheckData
      const isFinal = ['READY', 'ERROR', 'DUPLICATE'].includes(statusData.status)

      if (isFinal) {
        workflow.upsertLiveFile(statusData)
        await refetchInbox()
      } else {
        workflow.lastInboxUpdate[hash] = now
      }
    } catch (error) {
      console.error(`Failed to check status for ${hash}:`, error)
    }
  }
}

const startWatchdog = () => {
  watchdogTimer = setInterval(checkStaleFiles, WATCHDOG_CONFIG.interval)
}

const stopWatchdog = () => {
  if (watchdogTimer) {
    clearInterval(watchdogTimer)
    watchdogTimer = null
  }
}

// ============================================================================
// FILE OPERATIONS
// ============================================================================

const triggerDigest = async (hash?: string) => {
  if (!hash) return

  workflow.upsertLiveFile({
    sha256: hash,
    status: 'PROCESSING',
    progressMessage: 'Starting Analysis...'
  })

  try {
    const { error } = await client.POST("/api/inbox/{hash}/digest", {
      params: { path: { hash } }
    })

    if (error) throw new Error("Request failed")
  } catch (error) {
    workflow.upsertLiveFile({
      sha256: hash,
      status: 'ERROR',
      progressMessage: 'Analysis Request Failed'
    })
    push.error("Failed to start analysis")
  }
}

const ignoreFile = async (hash?: string) => {
  if (!hash) return

  ignoredSet.value.add(hash)

  try {
    await client.POST("/api/inbox/{hash}/ignore", {
      params: { path: { hash } }
    })

    push.success("File ignored")
    workflow.removeLiveFile(hash)
    await refetchInbox()
  } catch (error) {
    ignoredSet.value.delete(hash)
    push.error("Failed to ignore file")
  }
}

const analyzeAll = async () => {
  const toProcess = processableFiles.value

  if (toProcess.length === 0) {
    push.info("Nothing new to analyze")
    return
  }

  push.info(`Queued ${toProcess.length} documents...`)

  const queue = [...toProcess]
  
  const processNext = async (): Promise<void> => {
    if (queue.length === 0) return

    const file = queue.shift()
    if (file?.sha256) {
      await triggerDigest(file.sha256)
    }

    await processNext()
  }

  // Start concurrent processing
  const workers = Array(CONCURRENT_ANALYSIS_LIMIT).fill(null).map(() => processNext())
  await Promise.all(workers)
}

// ============================================================================
// UI INTERACTIONS
// ============================================================================

const openPreview = (file: InboxFile) => {
  if (!file.sha256) return

  ui.setRightPanel(ViewMode.PREVIEW, {
    id: file.sha256,
    name: file.emailInfo?.subject || file.fileInfo.originalFilename,
    source: 'inbox'
  })
}

const handleFileClick = (file: InboxFile) => {
  if (file.status === 'DUPLICATE') {
    push.warning("Duplicate file. Please ignore/delete it.")
    return
  }

  if (file.status === 'PROCESSING') {
    push.info("File is processing. Please wait.")
    return
  }

  const hash = file.sha256
  const name = file.emailInfo?.subject || file.fileInfo.originalFilename || "Untitled"
  const payload = {
    sha256: hash,
    name,
    prediction: file.prediction || null
  }

  // Priority: Use linked context if present, otherwise selected context
  let targetContext = null
  
  if (file.prediction?.context) {
    // Find the linked context by ID
    targetContext = contexts.value.find((c: any) => c.uuid === file.prediction?.context)
  }
  
  // Fallback to selected context if no link or link not found
  if (!targetContext) {
    targetContext = dms.selectedContext
  }

  if (targetContext) {
    dms.startItemCreation(targetContext, payload)
  } else {
    openPreview(file)
  }
}

const assignContextToFile = (hash: string, contextId: string) => {
  // Update the live file prediction via workflow store (reactive)
  workflow.upsertLiveFile({
    sha256: hash,
    prediction: {
      context: contextId,
      manuallyAssigned: true
    }
  })
  
  // Find context name for feedback
  const ctx = contexts.value.find((c: any) => c.uuid === contextId)
  if (ctx) {
    push.success(`Linked to ${ctx.name}`)
  }
}

// ============================================================================
// DRAG & DROP
// ============================================================================

const handleDragEnter = (event: DragEvent) => {
  if (event.dataTransfer?.types.includes('Files')) {
    isDragOver.value = true
  }
}

const handleDragLeave = () => {
  isDragOver.value = false
}

const handleDrop = async (event: DragEvent) => {
  isDragOver.value = false
  event.stopPropagation()

  const files = event.dataTransfer?.files
  if (!files?.length) return

  isUploading.value = true
  let successCount = 0

  for (const file of Array.from(files)) {
    if (file.size === 0) continue

    try {
      await InboxService.upload(file)
      successCount++
    } catch (error: any) {
      push.error(`Failed to upload ${file.name}: ${error.message}`)
    }
  }

  if (successCount > 0) {
    push.success(`Uploaded ${successCount} file${successCount > 1 ? 's' : ''}`)
  }

  isUploading.value = false
}

// ============================================================================
// LIFECYCLE
// ============================================================================

onMounted(() => {
  startWatchdog()
})

onUnmounted(() => {
  stopWatchdog()
})
</script>

<template>
  <div
    class="h-full relative group/inbox"
    @dragover.prevent="handleDragEnter"
    @dragleave.prevent="handleDragLeave"
    @drop.prevent="handleDrop"
  >
    <!-- Drag Overlay -->
    <div
      v-if="isDragOver"
      class="absolute inset-0 z-50 bg-blue-50/90 dark:bg-blue-900/90 
             flex flex-col items-center justify-center border-4 border-blue-400 
             border-dashed m-2 rounded-lg pointer-events-none animate-in 
             fade-in duration-200 backdrop-blur-sm"
    >
      <UploadCloud :size="48" class="text-blue-500 dark:text-blue-300 mb-2" />
      <span class="text-lg font-bold text-blue-700 dark:text-blue-200">
        Drop files to upload
      </span>
    </div>

    <!-- Upload Progress Overlay -->
    <div
      v-if="isUploading"
      class="absolute inset-0 z-50 bg-white/80 dark:bg-gray-900/80 
             flex flex-col items-center justify-center backdrop-blur-sm"
    >
      <Loader2 :size="32" class="text-blue-600 dark:text-blue-400 animate-spin mb-2" />
      <span class="text-sm font-bold text-gray-600 dark:text-gray-300">
        Uploading files...
      </span>
    </div>

    <!-- Main Content -->
    <BasePane title="Inbox" :count="filteredFiles.length">
      <template #actions>
        <button
          type="button"
          class="btn-icon text-purple-600 hover:bg-purple-50 
                 dark:text-purple-400 dark:hover:bg-purple-900/30"
          title="Analyze All (Skip Duplicates)"
          @click="analyzeAll"
        >
          <Wand2 :size="14" />
        </button>

        <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1" />

        <button
          type="button"
          class="btn-icon"
          title="Refresh"
          @click="() => refetchInbox()"
        >
          <RefreshCw :size="14" />
        </button>
      </template>

      <template #filter>
        <SearchInput v-model="searchQuery" placeholder="Filter inbox..." />
      </template>

      <!-- File List -->
      <div class="p-2 space-y-1">
        <!-- Loading State -->
        <div
          v-if="isLoadingInbox && filteredFiles.length === 0"
          class="p-4 text-xs text-gray-400 italic text-center"
        >
          Loading...
        </div>

        <!-- Empty State -->
        <div
          v-else-if="filteredFiles.length === 0"
          class="p-6 text-center flex flex-col items-center opacity-50"
        >
          <FileText :size="24" class="mb-2 text-gray-300 dark:text-gray-600" />
          <span class="text-xs text-gray-500 dark:text-gray-400 italic">
            Drop files here to upload
          </span>
        </div>

        <!-- File Items -->
        <template v-else>
          <InboxItem
            v-for="file in filteredFiles"
            :key="file.sha256"
            :file="file"
            :is-processing="file.status === 'PROCESSING'"
            :is-duplicate="file.status === 'DUPLICATE'"
            @click="handleFileClick(file)"
            @preview="openPreview(file)"
            @ignore="ignoreFile(file.sha256)"
            @analyze="triggerDigest(file.sha256)"
            @assign-context="assignContextToFile(file.sha256, $event)"
          />
        </template>
      </div>
    </BasePane>
  </div>
</template>