<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { onClickOutside } from '@vueuse/core'
import ItemFormFields from '@/components/forms/ItemFormFields.vue'
import PreviouslyFiled from '@/components/forms/PreviouslyFiled.vue'
import { ItemService } from '@/services/ItemService'
import type { Item } from '@/types/models'
import { parseBackendDate } from '@/lib/utils'
import BaseButton from '@/components/ui/BaseButton.vue'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
import { useDmsStore } from '@/stores/dms'
import { useWorkflowStore } from '@/stores/workflow'
import { useItemForm } from '@/composables/useItemForm'
import { useHotkeys } from '@/composables/useHotkeys'
import { Sparkles, FileText, Eye, Columns, Folder, Bug, Wand2, Code, X, Link, Hash, ChevronDown, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { push } from 'notivue'

const dms = useDmsStore()
const workflow = useWorkflowStore()

const contextName = computed(() => {
    if (workflow.targetContextForDrop?.name) return workflow.targetContextForDrop.name
    if (dms.selectedContext?.name) return dms.selectedContext.name
    return 'Unassigned'
})

const activeTab = ref<'metadata' | 'preview' | 'split'>('split')
const showDebug = ref(false)
const isDragOver = ref(false)

const { 
    form, aiHighlights, aiProposal, touched, isLoading, save, file, hasReminder, reminderForm,
    applyPrediction, rawPrediction
} = useItemForm(false)

const reminderState = ref({ active: hasReminder, data: reminderForm })
const onUpdateReminderState = (newState: any) => {
    hasReminder.value = newState.active
    reminderForm.value = newState.data
}

const targetContextUuid = computed(() => workflow.targetContextForDrop?.uuid || dms.selectedContext?.uuid || null)

// Comparing the new document with one filed before: side by side, or flipping between the two in one pane
const compareItem = ref<Item | null>(null)
const compareMode = ref<'side' | 'flip'>('side')
const flipShows = ref<'new' | 'old'>('new')
const showNew = computed(() => !compareItem.value || compareMode.value === 'side' || flipShows.value === 'new')
const showOld = computed(() => !!compareItem.value && (compareMode.value === 'side' || flipShows.value === 'old'))
const compareLabel = computed(() => {
    const item = compareItem.value
    if (!item) return ''
    const d = parseBackendDate(item.issueDate)
    return d ? `${item.name} · ${d.toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' })}` : item.name
})

// The target context's latest documents: the context chip opens them as a list, its count shows
// there are some, and ◀ ▶ step through them while comparing
const LATEST = 6
const { data: latestItems, isLoading: latestLoading, isError: latestError } = useQuery({
    queryKey: computed(() => ['context-latest', targetContextUuid.value, LATEST]),
    queryFn: () => ItemService.getLatestInContext(targetContextUuid.value as string, LATEST),
    enabled: computed(() => !!targetContextUuid.value),
    staleTime: 30_000,
})

const rootRef = ref<HTMLElement | null>(null)
const chipRef = ref<HTMLElement | null>(null)
const listRef = ref<HTMLElement | null>(null)
const listOpen = ref(false)
const listLeft = ref(16)
const toggleList = () => {
    if (!listOpen.value && chipRef.value && rootRef.value) {
        listLeft.value = Math.max(8, chipRef.value.getBoundingClientRect().left - rootRef.value.getBoundingClientRect().left)
    }
    listOpen.value = !listOpen.value
}
onClickOutside(listRef, () => { listOpen.value = false }, { ignore: [chipRef] })

const toggleCompare = (item: Item) => {
    compareItem.value = compareItem.value?.uuid === item.uuid ? null : item
    flipShows.value = 'old'
    listOpen.value = false
    if (compareItem.value) activeTab.value = 'split'
}

// ◀ older, ▶ newer: the list is newest first
const compareIndex = computed(() => latestItems.value?.findIndex(i => i.uuid === compareItem.value?.uuid) ?? -1)
const canStep = (direction: 1 | -1) => {
    const next = compareIndex.value + direction
    return compareIndex.value >= 0 && next >= 0 && next < (latestItems.value?.length ?? 0)
}
const step = (direction: 1 | -1) => {
    if (!canStep(direction)) return
    compareItem.value = latestItems.value![compareIndex.value + direction]
    flipShows.value = 'old'
}

// another document or another context: the comparison no longer applies
watch(() => file.value?.sha256, () => { compareItem.value = null; listOpen.value = false })
watch(targetContextUuid, () => { compareItem.value = null })

const handleReApply = () => {
    if(rawPrediction.value) {
        applyPrediction(rawPrediction.value)
        push.success("AI values reapplied")
    }
}

const onDropContext = (e: DragEvent) => {
    isDragOver.value = false
    const raw = e.dataTransfer?.getData('application/json')
    if (!raw) return
    
    try {
        const data = JSON.parse(raw)
        if (data.type === 'dms-context') {
            dms.setSelectedContext({ uuid: data.id, name: data.name } as any)
            push.success(`Linked to context: ${data.name}`)
        }
    } catch(e) {}
}

// Shortcuts
useHotkeys(['s', 'S'], () => {
    if (!isLoading.value && form.value.name) save()
}, { ctrl: true, prevent: true })

useHotkeys('Escape', () => {
    if (listOpen.value) { listOpen.value = false; return }
    if (compareItem.value) { compareItem.value = null; return }
    if (!isLoading.value) dms.cancelCreation()
})
</script>

<template>
  <div ref="rootRef" class="h-full flex flex-col bg-background overflow-hidden transition-colors relative">
      <!-- HEADER -->
      <div class="bg-background border-b border-border h-[45px] flex items-center justify-between px-4 shrink-0 shadow-sm">
          <div class="flex items-center gap-3 overflow-hidden">
              <div class="font-bold text-foreground text-sm whitespace-nowrap">Add Document</div>

              <!-- Source filename chip -->
              <div
                v-if="file?.fileInfo?.originalFilename || file?.displayName"
                class="flex items-center gap-1 px-2 py-0.5 rounded border border-border bg-muted/40 text-muted-foreground text-xs font-mono max-w-[220px] truncate"
                :title="file?.fileInfo?.originalFilename || file?.displayName"
              >
                <FileText :size="11" class="shrink-0" />
                <span class="truncate">{{ file?.fileInfo?.originalFilename || file?.displayName }}</span>
              </div>
              
              <!-- Target context: drop a context here to change it, click to see what was filed there -->
              <button
                ref="chipRef"
                type="button"
                class="flex items-center gap-1.5 px-2 py-0.5 rounded-full border transition-all duration-200 cursor-pointer"
                :class="isDragOver ? 'bg-green-100 border-green-300 text-green-700 scale-105 shadow-md ring-2 ring-green-200' : listOpen ? 'bg-blue-100 dark:bg-blue-900/60 border-blue-300 dark:border-blue-700 text-blue-800 dark:text-blue-200' : 'bg-blue-50 dark:bg-blue-900/30 border-blue-100 dark:border-blue-800 text-blue-700 dark:text-blue-300 hover:border-blue-300 dark:hover:border-blue-700'"
                :title="'Target Context: ' + contextName + ' (click: previously filed documents)'"
                :aria-expanded="listOpen"
                @click="toggleList"
                @dragover.prevent="isDragOver = true"
                @dragleave="isDragOver = false"
                @drop="onDropContext"
              >
                  <Folder v-if="!isDragOver" :size="12" class="shrink-0" />
                  <Link v-else :size="12" class="shrink-0 animate-pulse" />
                  <span class="truncate max-w-[200px] sm:max-w-xs">{{ isDragOver ? 'Drop to Link' : contextName }}</span>
                  <span v-if="!isDragOver && latestItems?.length" class="font-mono text-[10px] opacity-70 shrink-0">· {{ latestItems.length }}</span>
                  <ChevronDown v-if="!isDragOver" :size="12" class="shrink-0 transition-transform" :class="{ 'rotate-180': listOpen }" />
              </button>

              <div v-if="aiHighlights.name || aiHighlights.date" class="hidden md:flex bg-purple-50 dark:bg-purple-900/30 border border-purple-200 dark:border-purple-800 px-2 py-0.5 rounded-full text-[10px] text-purple-700 dark:text-purple-300 items-center gap-1 font-bold animate-in fade-in zoom-in-95">
                  <Sparkles :size="12" /> AI Suggested
              </div>
          </div>
          
          <div class="flex gap-2 items-center shrink-0">
              
              <!-- AI TOOLBAR -->
              <div class="flex items-center border rounded-md p-0.5 mr-2 bg-muted/30" v-if="rawPrediction">
                  <button 
                    @click="handleReApply" 
                    class="p-1.5 hover:bg-purple-50 text-purple-600 dark:text-purple-400 dark:hover:bg-purple-900/30 rounded transition-colors" 
                    title="Re-apply AI Suggestions"
                  >
                      <Wand2 :size="14" />
                  </button>
                  <div class="w-px h-3 bg-border mx-1"></div>
                  <button 
                    @click="showDebug = !showDebug" 
                    class="p-1.5 hover:bg-gray-200 dark:hover:bg-gray-700 rounded transition-colors" 
                    :class="{'text-blue-600 bg-blue-100 dark:bg-blue-900/30 dark:text-blue-300': showDebug, 'text-gray-500': !showDebug}"
                    title="Toggle JSON Debug"
                  >
                      <Code :size="14" />
                  </button>
              </div>

              <div class="flex bg-muted/50 rounded-md p-0.5 border border-border transition-colors">
                  <button @click="activeTab = 'split'" class="hidden md:flex p-1.5 rounded transition-all" :class="activeTab === 'split' ? 'tab-active' : 'tab-inactive'" title="Split View"><Columns :size="14" /></button>
                  <button @click="activeTab = 'preview'" class="p-1.5 rounded transition-all" :class="activeTab === 'preview' ? 'tab-active' : 'tab-inactive'" title="Document Only"><Eye :size="14" /></button>
                  <button @click="activeTab = 'metadata'" class="p-1.5 rounded transition-all" :class="activeTab === 'metadata' ? 'tab-active' : 'tab-inactive'" title="Form Only"><FileText :size="14" /></button>
              </div>
          </div>
      </div>

      <!-- Dropdown of the context chip: the context's latest documents -->
      <div
        v-if="listOpen"
        ref="listRef"
        class="absolute z-30 top-[48px] w-[440px]"
        :style="{ left: listLeft + 'px', maxWidth: `calc(100% - ${listLeft + 8}px)` }"
      >
          <PreviouslyFiled
            :context-uuid="targetContextUuid"
            :context-name="contextName"
            :items="latestItems"
            :is-loading="latestLoading"
            :is-error="latestError"
            :compared-uuid="compareItem?.uuid || null"
            @compare="toggleCompare"
          />
      </div>

      <!-- BODY -->
      <div class="flex-1 overflow-hidden relative flex">
          <!-- Main Form Area -->
          <div class="flex-1 flex flex-col min-w-0 h-full">
              <div v-if="activeTab === 'split'" class="flex h-full w-full">
                  <div class="h-full bg-muted/10 border-r border-border relative flex flex-col" :class="compareItem ? 'w-8/12' : 'w-7/12'">
                      <!-- Compare bar: how to show the two, which one (flip), close -->
                      <div v-if="compareItem" class="h-9 shrink-0 flex items-center gap-2 px-2 border-b border-border bg-background text-xs">
                          <div class="flex bg-muted/50 rounded-md p-0.5 border border-border shrink-0">
                              <button class="px-2 py-0.5 rounded transition-all" :class="compareMode === 'side' ? 'tab-active' : 'tab-inactive'" @click="compareMode = 'side'" title="Side by side">Side by side</button>
                              <button class="px-2 py-0.5 rounded transition-all" :class="compareMode === 'flip' ? 'tab-active' : 'tab-inactive'" @click="compareMode = 'flip'" title="One at a time, flip between them">Flip</button>
                          </div>
                          <template v-if="compareMode === 'flip'">
                              <button class="px-2 py-0.5 rounded border transition-all shrink-0" :class="flipShows === 'new' ? 'border-blue-300 bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800' : 'border-border text-muted-foreground'" @click="flipShows = 'new'">New</button>
                              <button class="px-2 py-0.5 rounded border transition-all truncate min-w-0" :class="flipShows === 'old' ? 'border-blue-300 bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800' : 'border-border text-muted-foreground'" @click="flipShows = 'old'" :title="compareLabel">{{ compareLabel }}</button>
                          </template>
                          <span v-else class="text-muted-foreground truncate">New · {{ compareLabel }}</span>
                          <div class="ml-auto flex items-center shrink-0">
                              <button class="p-1 rounded text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:hover:text-muted-foreground" :disabled="!canStep(1)" @click="step(1)" title="Older document">
                                  <ChevronLeft :size="14" />
                              </button>
                              <button class="p-1 rounded text-muted-foreground hover:text-foreground disabled:opacity-30 disabled:hover:text-muted-foreground" :disabled="!canStep(-1)" @click="step(-1)" title="Newer document">
                                  <ChevronRight :size="14" />
                              </button>
                          </div>
                          <button class="p-1 rounded text-muted-foreground hover:text-red-500 shrink-0" @click="compareItem = null" title="Close comparison (Esc)">
                              <X :size="14" />
                          </button>
                      </div>
                      <div class="flex-1 min-h-0 flex">
                          <div v-show="showNew" class="h-full min-w-0 relative" :class="compareItem && compareMode === 'side' ? 'w-1/2 border-r border-border' : 'w-full'">
                              <DocumentPreview
                                :identifier="file?.sha256 || ''"
                                source="inbox"
                                :file-name="file?.fileInfo?.originalFilename || form.name"
                              />
                          </div>
                          <div v-if="compareItem" v-show="showOld" class="h-full min-w-0 relative" :class="compareMode === 'side' ? 'w-1/2' : 'w-full'">
                              <DocumentPreview
                                :key="compareItem.uuid"
                                :identifier="compareItem.uuid"
                                source="item"
                                :file-name="compareItem.name"
                              />
                          </div>
                      </div>
                  </div>
                  <div class="h-full bg-background overflow-y-auto custom-scrollbar p-6" :class="compareItem ? 'w-4/12' : 'w-5/12'">
                      <div class="max-w-xl mx-auto space-y-4">
                      <ItemFormFields
                            v-model="form" 
                            :touched="touched" 
                            :reminder-state="{ active: hasReminder, data: reminderForm }" 
                            @update:reminder-state="onUpdateReminderState" 
                            :ai-highlights="aiHighlights" 
                            :ai-proposal="aiProposal"
                          />
                          <!-- Inline Buttons (Split View) -->
                          <div class="pt-6 flex justify-between items-center border-t border-dashed border-border mt-4">
                              <div class="text-[10px] text-muted-foreground flex gap-2">
                                  <span class="hidden xl:inline"><kbd class="font-mono border rounded px-1 bg-muted">Ctrl+S</kbd> Save</span>
                                  <span class="hidden xl:inline"><kbd class="font-mono border rounded px-1 bg-muted">Esc</kbd> Cancel</span>
                              </div>
                              <div class="flex gap-2">
                                  <BaseButton variant="outline" @click="dms.cancelCreation" :disabled="isLoading">Cancel</BaseButton>
                                  <BaseButton variant="default" @click="save" :disabled="isLoading || !form.name">Save</BaseButton>
                              </div>
                          </div>
                      </div>
                  </div>
              </div>

              <div v-else-if="activeTab === 'preview'" class="h-full w-full bg-muted/10">
                  <DocumentPreview 
                    :identifier="file?.sha256 || ''" 
                    source="inbox" 
                    :file-name="file?.fileInfo?.originalFilename || form.name"
                  />
              </div>

              <div v-else class="h-full w-full bg-background overflow-y-auto custom-scrollbar p-6">
                  <div class="max-w-2xl mx-auto">
                      <ItemFormFields
                        v-model="form" 
                        :touched="touched" 
                        :reminder-state="{ active: hasReminder, data: reminderForm }" 
                        @update:reminder-state="onUpdateReminderState"
                        :ai-highlights="aiHighlights" 
                        :ai-proposal="aiProposal"
                      />
                      <!-- Inline Buttons (Full Form View) -->
                      <div class="pt-6 flex justify-between items-center border-t border-dashed border-border mt-4">
                          <div class="text-[10px] text-muted-foreground flex gap-2">
                              <span><kbd class="font-mono border rounded px-1 bg-muted">Ctrl+S</kbd> Save</span>
                              <span><kbd class="font-mono border rounded px-1 bg-muted">Esc</kbd> Cancel</span>
                          </div>
                          <div class="flex gap-2">
                              <BaseButton variant="outline" @click="dms.cancelCreation" :disabled="isLoading">Cancel</BaseButton>
                              <BaseButton variant="default" @click="save" :disabled="isLoading || !form.name">Save</BaseButton>
                          </div>
                      </div>
                  </div>
              </div>
          </div>

          <!-- RIGHT SIDE DEBUG PANEL (Sliding) -->
          <div 
            v-if="showDebug" 
            class="w-[350px] border-l border-border bg-gray-50 dark:bg-gray-900 overflow-y-auto p-0 shadow-xl z-20 font-mono text-xs flex flex-col transition-all"
          >
              <div class="flex items-center justify-between px-3 py-2 border-b border-border bg-white dark:bg-gray-800 shrink-0">
                  <span class="font-bold uppercase text-muted-foreground flex items-center gap-2">
                      <Bug :size="14" /> AI Raw Response
                  </span>
                  <button @click="showDebug = false" class="text-muted-foreground hover:text-red-500 p-1">
                      <X :size="14" />
                  </button>
              </div>
              
              <div class="p-3">
                  <div v-if="!rawPrediction" class="text-muted-foreground italic p-4 text-center">
                      No prediction data available.
                  </div>
                  <pre v-else class="whitespace-pre-wrap break-all text-gray-700 dark:text-gray-300">{{ JSON.stringify(rawPrediction, null, 2) }}</pre>
              </div>
          </div>
      </div>
  </div>
</template>