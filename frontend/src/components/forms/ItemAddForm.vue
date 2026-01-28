<script setup lang="ts">
import { ref, computed } from 'vue'
import ItemFormFields from '@/components/forms/ItemFormFields.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
import { useDmsStore } from '@/stores/dms'
import { useWorkflowStore } from '@/stores/workflow'
import { useItemForm } from '@/composables/useItemForm'
import { useHotkeys } from '@/composables/useHotkeys'
import { Sparkles, FileText, Eye, Columns, Folder, Bug, Wand2, Code, X, Link } from 'lucide-vue-next'
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
    form, aiHighlights, touched, isLoading, save, file, hasReminder, reminderForm,
    applyPrediction, rawPrediction
} = useItemForm(false)

const reminderState = ref({ active: hasReminder, data: reminderForm })
const onUpdateReminderState = (newState: any) => {
    hasReminder.value = newState.active
    reminderForm.value = newState.data
}

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
    if (!isLoading.value) dms.cancelCreation()
})
</script>

<template>
  <div class="h-full flex flex-col bg-background overflow-hidden transition-colors">
      <!-- HEADER -->
      <div class="bg-background border-b border-border h-[45px] flex items-center justify-between px-4 shrink-0 shadow-sm">
          <div class="flex items-center gap-3 overflow-hidden">
              <div class="font-bold text-foreground text-sm whitespace-nowrap">Add Document</div>
              
              <div 
                class="flex items-center gap-1.5 px-2 py-0.5 rounded-full border transition-all duration-200 cursor-default"
                :class="isDragOver ? 'bg-green-100 border-green-300 text-green-700 scale-105 shadow-md ring-2 ring-green-200' : 'bg-blue-50 dark:bg-blue-900/30 border-blue-100 dark:border-blue-800 text-blue-700 dark:text-blue-300'"
                :title="'Target Context: ' + contextName"
                @dragover.prevent="isDragOver = true"
                @dragleave="isDragOver = false"
                @drop="onDropContext"
              >
                  <Folder v-if="!isDragOver" :size="12" class="shrink-0" />
                  <Link v-else :size="12" class="shrink-0 animate-pulse" />
                  <span class="truncate max-w-[200px] sm:max-w-xs">{{ isDragOver ? 'Drop to Link' : contextName }}</span>
              </div>

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
                  <button @click="activeTab = 'split'" class="hidden md:flex p-1.5 rounded transition-all" :class="activeTab === 'split' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'" title="Split View"><Columns :size="14" /></button>
                  <button @click="activeTab = 'preview'" class="p-1.5 rounded transition-all" :class="activeTab === 'preview' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'" title="Document Only"><Eye :size="14" /></button>
                  <button @click="activeTab = 'metadata'" class="p-1.5 rounded transition-all" :class="activeTab === 'metadata' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'" title="Form Only"><FileText :size="14" /></button>
              </div>
          </div>
      </div>

      <!-- BODY -->
      <div class="flex-1 overflow-hidden relative flex">
          <!-- Main Form Area -->
          <div class="flex-1 flex flex-col min-w-0 h-full">
              <div v-if="activeTab === 'split'" class="flex h-full w-full">
                  <div class="w-7/12 h-full bg-muted/10 border-r border-border relative">
                      <DocumentPreview 
                        :identifier="file?.sha256 || ''" 
                        source="inbox" 
                        :file-name="file?.fileInfo?.originalFilename || form.name"
                      />
                  </div>
                  <div class="w-5/12 h-full bg-background overflow-y-auto custom-scrollbar p-6">
                      <div class="max-w-xl mx-auto space-y-4">
                          <ItemFormFields 
                            v-model="form" 
                            :touched="touched" 
                            :reminder-state="{ active: hasReminder, data: reminderForm }" 
                            @update:reminder-state="onUpdateReminderState" 
                            :ai-highlights="aiHighlights" 
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