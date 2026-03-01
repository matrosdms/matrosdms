<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import BasePane from '@/components/ui/BasePane.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import InboxItem from '@/components/panes/InboxItem.vue'
import { useInboxQueries } from '@/composables/queries/useInboxQueries'
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { useServerEvents } from '@/composables/useServerEvents'
import { useListNavigation } from '@/composables/useListNavigation'
import { useDmsStore } from '@/stores/dms'
import { useWorkflowStore } from '@/stores/workflow'
import { useUIStore } from '@/stores/ui'
import { ViewMode } from '@/enums'
import { client } from '@/api/client'
import { InboxService } from '@/services/InboxService'
import { ItemService } from '@/services/ItemService'
import { FileText, RefreshCw, Wand2, UploadCloud, Loader2 } from 'lucide-vue-next'
import { push } from 'notivue'
import type { InboxFile } from '@/types/events'

type InboxFileStatus = 'READY' | 'PROCESSING' | 'ERROR' | 'DUPLICATE'
interface StatusCheckData { sha256: string; status: InboxFileStatus; progressMessage?: string }

const dms = useDmsStore()
const ui = useUIStore()
const workflow = useWorkflowStore()
const { inboxFiles, isLoadingInbox, refetchInbox } = useInboxQueries()
const { contexts } = useContextQueries()

useServerEvents()

const WATCHDOG_CONFIG = { interval: 10000, staleThreshold: 10000 }
const CONCURRENT_ANALYSIS_LIMIT = 2
const PROCESSABLE_STATUSES: Set<InboxFileStatus> = new Set(['READY', 'DUPLICATE', 'PROCESSING'])

const searchQuery = ref('')
const ignoredSet = ref(new Set<string>())
const isDragOver = ref(false)
const isUploading = ref(false)
const activeIndex = ref(-1)
const listContainerRef = ref<HTMLDivElement | null>(null)
let watchdogTimer: ReturnType<typeof setTimeout> | null = null

const effectiveFiles = computed(() => {
  const apiFiles = inboxFiles.value || []
  const fileMap = new Map<string, InboxFile>()
  apiFiles.forEach((file: any) => { if (file.sha256) fileMap.set(file.sha256, { ...file, sha256: file.sha256 } as InboxFile) })
  for (const [hash, liveFile] of Object.entries(workflow.liveInboxFiles)) {
    if (fileMap.has(hash)) fileMap.set(hash, { ...fileMap.get(hash), ...liveFile } as InboxFile)
    else fileMap.set(hash, liveFile)
  }
  return Array.from(fileMap.values()).filter(file => file.sha256 && !ignoredSet.value.has(file.sha256))
})

const filteredFiles = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return effectiveFiles.value
  return effectiveFiles.value.filter(file => (file.emailInfo?.subject || file.fileInfo?.originalFilename || '').toLowerCase().includes(query))
})

const processableFiles = computed(() => filteredFiles.value.filter(file => !file.status || !PROCESSABLE_STATUSES.has(file.status)))

// Scrolls active item into view and ensures container focus
const scrollActiveIntoView = () => {
    if (activeIndex.value < 0) return
    const el = listContainerRef.value?.children[activeIndex.value] as HTMLElement
    if (el) el.scrollIntoView({ block: 'nearest' })
}

const focusListContainer = () => {
    requestAnimationFrame(() => {
        listContainerRef.value?.focus({ preventScroll: true })
        scrollActiveIntoView()
    })
}

const ensureActiveIndex = () => {
  if (!filteredFiles.value.length) { activeIndex.value = -1; return }
  if (activeIndex.value === -1) activeIndex.value = 0
  else if (activeIndex.value >= filteredFiles.value.length) activeIndex.value = filteredFiles.value.length - 1
}

watch(filteredFiles, ensureActiveIndex, { immediate: true })

const handleFileClick = async (file: InboxFile, index?: number) => {
  if (typeof index === 'number') setActiveIndex(index)
  if (file.status === 'DUPLICATE') {
    if (file.doublette) { 
      try {
        const fullItem = await ItemService.getById(file.doublette);
        dms.setSelectedContext(fullItem.context || null);
        dms.setSelectedItem(fullItem);
        ui.setView('dms');
        ui.setRightPanelView(ViewMode.DETAILS);
        push.info(`Switched to existing document.`);
      } catch (e) {
        ItemService.openDocument(file.doublette); 
        push.info(`Opening existing document...`);
      }
    } 
    else push.warning("Duplicate file. Please ignore/delete it.")
    nextTick(focusListContainer); return
  }
  if (file.status === 'PROCESSING') { push.info("File is processing. Please wait."); nextTick(focusListContainer); return }
  
  let targetContext = null
  if (file.prediction?.context) targetContext = contexts.value.find((c: any) => c.uuid === file.prediction?.context)
  if (!targetContext) targetContext = dms.selectedContext
  
  if (targetContext) dms.startItemCreation(targetContext, { sha256: file.sha256, name: file.emailInfo?.subject || file.fileInfo?.originalFilename || "Untitled", prediction: file.prediction || null })
  else openPreview(file)
  nextTick(focusListContainer)
}

const { handleKey: handleListKey } = useListNavigation({
  listLength: computed(() => filteredFiles.value.length),
  activeIndex,
  onSelect: (index) => { 
      const file = filteredFiles.value[index]
      if (file) handleFileClick(file, index) 
  }
})

const handleListKeyDown = (event: KeyboardEvent) => {
    handleListKey(event)
    // Scroll visually on arrow keys
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        nextTick(scrollActiveIntoView)
    }
}

const setActiveIndex = (index: number) => activeIndex.value = index

const checkStaleFiles = async () => {
  const now = Date.now()
  for (const hash of workflow.processingInboxItems) {
    if (now - (workflow.lastInboxUpdate[hash] || 0) <= WATCHDOG_CONFIG.staleThreshold) continue
    try {
      const { data, error } = await client.GET("/api/inbox/{hash}/status", { params: { path: { hash } } })
      if (error || !data) continue
      const statusData = data as unknown as StatusCheckData
      if (['READY', 'ERROR', 'DUPLICATE'].includes(statusData.status)) {
        workflow.upsertLiveFile(statusData); await refetchInbox()
      } else {
        workflow.lastInboxUpdate[hash] = now
      }
    } catch (e) { console.error(`Failed check ${hash}`, e) }
  }
}

const startWatchdog = () => watchdogTimer = setInterval(checkStaleFiles, WATCHDOG_CONFIG.interval)
const stopWatchdog = () => { if (watchdogTimer) { clearInterval(watchdogTimer); watchdogTimer = null } }

const triggerDigest = async (hash?: string) => {
  if (!hash) return
  workflow.upsertLiveFile({ sha256: hash, status: 'PROCESSING', progressMessage: 'Starting Analysis...' })
  try {
    const { error } = await client.POST("/api/inbox/{hash}/digest", { params: { path: { hash } } })
    if (error) throw new Error("Request failed")
  } catch (error) {
    workflow.upsertLiveFile({ sha256: hash, status: 'ERROR', progressMessage: 'Analysis Request Failed' })
    push.error("Failed to start analysis")
  }
}

const ignoreFile = async (hash?: string) => {
  if (!hash) return
  ignoredSet.value.add(hash)
  try {
    await client.POST("/api/inbox/{hash}/ignore", { params: { path: { hash } } })
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
  if (toProcess.length === 0) { push.info("Nothing new to analyze"); return }
  push.info(`Queued ${toProcess.length} documents...`)
  const queue = [...toProcess]
  const processNext = async (): Promise<void> => {
    if (queue.length === 0) return
    const file = queue.shift()
    if (file?.sha256) await triggerDigest(file.sha256)
    await processNext()
  }
  const workers = Array(CONCURRENT_ANALYSIS_LIMIT).fill(null).map(() => processNext())
  await Promise.all(workers)
}

const openPreview = (file: InboxFile) => {
  if (!file.sha256) return
  ui.setRightPanel(ViewMode.PREVIEW, { id: file.sha256, name: file.emailInfo?.subject || file.fileInfo?.originalFilename, source: 'inbox' })
  nextTick(focusListContainer)
}

const handlePreviewClick = (file: InboxFile, index: number) => { setActiveIndex(index); openPreview(file) }
const assignContextToFile = (hash: string | undefined, contextId: string) => {
  if (!hash) return
  workflow.upsertLiveFile({ sha256: hash, prediction: { context: contextId, manuallyAssigned: true } })
  const ctx = contexts.value.find((c: any) => c.uuid === contextId)
  if (ctx) push.success(`Linked to ${ctx.name}`)
}

const handleDragEnter = (event: DragEvent) => { if (event.dataTransfer?.types.includes('Files')) isDragOver.value = true }
const handleDragLeave = () => isDragOver.value = false
const handleDrop = async (event: DragEvent) => {
  isDragOver.value = false; event.stopPropagation()
  const files = event.dataTransfer?.files; if (!files?.length) return
  isUploading.value = true; let successCount = 0
  for (const file of Array.from(files)) {
    if (file.size === 0) continue
    try { await InboxService.upload(file); successCount++ } catch (e: any) { push.error(`Failed to upload ${file.name}: ${e.message}`) }
  }
  if (successCount > 0) push.success(`Uploaded ${successCount} file${successCount > 1 ? 's' : ''}`)
  isUploading.value = false
}

onMounted(startWatchdog)
onUnmounted(stopWatchdog)
</script>

<template>
  <div class="h-full relative group/inbox" @dragover.prevent="handleDragEnter" @dragleave.prevent="handleDragLeave" @drop.prevent="handleDrop">
    <div v-if="isDragOver" class="absolute inset-0 z-50 bg-blue-50/90 dark:bg-blue-900/90 flex flex-col items-center justify-center border-4 border-blue-400 border-dashed m-2 rounded-lg pointer-events-none animate-in fade-in duration-200 backdrop-blur-sm">
      <UploadCloud :size="48" class="text-blue-500 dark:text-blue-300 mb-2" /><span class="text-lg font-bold text-blue-700 dark:text-blue-200">Drop files to upload</span>
    </div>
    <div v-if="isUploading" class="absolute inset-0 z-50 bg-white/80 dark:bg-gray-900/80 flex flex-col items-center justify-center backdrop-blur-sm">
      <Loader2 :size="32" class="text-blue-600 dark:text-blue-400 animate-spin mb-2" /><span class="text-sm font-bold text-gray-600 dark:text-gray-300">Uploading files...</span>
    </div>
    <BasePane title="Inbox" :count="filteredFiles.length">
      <template #actions>
        <button class="btn-icon text-purple-600 hover:bg-purple-50 dark:text-purple-400 dark:hover:bg-purple-900/30" title="Analyze All (Skip Duplicates)" @click="analyzeAll"><Wand2 :size="14" /></button>
        <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1" />
        <button class="btn-icon" title="Refresh" @click="() => refetchInbox()"><RefreshCw :size="14" /></button>
      </template>
      <template #filter><SearchInput v-model="searchQuery" placeholder="Filter inbox..." /></template>
      <div 
        ref="listContainerRef" 
        class="p-2 space-y-1 outline-none focus:pane-focused transition-all" 
        tabindex="0" 
        @keydown="handleListKeyDown" 
        @focus="ensureActiveIndex()" 
        @mousedown="focusListContainer"
      >
        <div v-if="isLoadingInbox && filteredFiles.length === 0" class="p-4 text-xs text-gray-400 italic text-center">Loading...</div>
        <div v-else-if="filteredFiles.length === 0" class="p-6 text-center flex flex-col items-center opacity-50"><FileText :size="24" class="mb-2 text-gray-300 dark:text-gray-600" /><span class="text-xs text-gray-500 dark:text-gray-400 italic">Drop files here to upload</span></div>
        <template v-else>
          <InboxItem v-for="(file, index) in filteredFiles" :key="file.sha256" :file="file" :is-processing="file.status === 'PROCESSING'" :is-duplicate="file.status === 'DUPLICATE'" :is-active="index === activeIndex" @click="handleFileClick(file, index)" @preview="handlePreviewClick(file, index)" @ignore="ignoreFile(file.sha256)" @analyze="triggerDigest(file.sha256)" @assign-context="assignContextToFile(file.sha256, $event)" />
        </template>
      </div>
    </BasePane>
  </div>
</template>