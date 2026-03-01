<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AnyEntity } from '@/types/models'
import EntityHeader from '@/components/panels/EntityHeader.vue'
import LinkedTasksPanel from '@/components/panels/LinkedTasksPanel.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSpinner from '@/components/ui/BaseSpinner.vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { ItemService } from '@/services/ItemService'
import { Sparkles, FileText, AlertTriangle, Database, Hash, Download, ChevronDown, Code } from 'lucide-vue-next'
import { push } from 'notivue'

const props = defineProps<{
  item: AnyEntity
}>()

defineEmits(['edit', 'delete'])

// State for AI / Text Tools
const showTextModal = ref(false)
const textContent = ref('')
const isFetchingText = ref(false)
const aiProcessing = ref(false)

// State for Technical Details & Full JSON
const showTechnical = ref(false)
const showMetadataModal = ref(false)
const metadataContent = ref('')
const isFetchingMetadata = ref(false)

const uuid = computed(() => props.item?.uuid)
const isEmailItem = computed(() => 'email' in props.item)
const isDataTypeItem = computed(() => 'dataType' in props.item)
const hasDescription = computed(() => !!props.item?.description)
const safeItem = computed(() => props.item as any)

// Determine if we show Document Tools (OCR, AI)
const isDocument = computed(() => !!uuid.value && !isEmailItem.value && !isDataTypeItem.value)
// Check new property from backend
const hasTextLayer = computed(() => isDocument.value && !!safeItem.value.hasTextLayer)
const isArchived = computed(() => !!safeItem.value.dateArchived)

// Extract Attributes safely if available
const attributes = computed(() => {
    if (safeItem.value.attributeList && Array.isArray(safeItem.value.attributeList)) {
        return safeItem.value.attributeList;
    }
    return[];
})

// --- TECHNICAL METADATA ---
const metadata = computed(() => safeItem.value.metadata)

const formattedSize = computed(() => {
    if (!metadata.value?.filesize) return null
    const kb = metadata.value.filesize / 1024
    return kb > 1024 ? (kb / 1024).toFixed(2) + ' MB' : kb.toFixed(2) + ' KB'
})

// --- ACTIONS ---

const openMetadataModal = async () => {
    if (!uuid.value) return
    showMetadataModal.value = true
    isFetchingMetadata.value = true
    try {
        // Fetch the definitive, complete object from the backend API
        const fullItem = await ItemService.getById(uuid.value)
        metadataContent.value = JSON.stringify(fullItem, null, 2)
    } catch (e: any) {
        metadataContent.value = `Error fetching metadata: ${e.message}`
        push.error(`Failed to fetch complete metadata: ${e.message}`)
    } finally {
        isFetchingMetadata.value = false
    }
}

const downloadMetadataJson = () => {
    if (!metadataContent.value) return
    const blob = new Blob([metadataContent.value], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `metadata_${uuid.value || 'export'}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    push.success('Raw JSON downloaded successfully')
}

const openTextModal = async () => {
    if (!uuid.value) return
    showTextModal.value = true
    isFetchingText.value = true
    try {
        textContent.value = await ItemService.getRawText(uuid.value)
    } catch (e: any) {
        textContent.value = `Error loading text: ${e.message}`
    } finally {
        isFetchingText.value = false
    }
}

const runAiTransform = async (instruction: 'SUMMARY' | 'KEY_FACTS') => {
    if (!uuid.value || aiProcessing.value) return
    aiProcessing.value = true
    try {
        push.info(`Generating ${instruction.toLowerCase().replace('_', ' ')}...`)
        const result = await ItemService.aiTransform(uuid.value, instruction, 'TEXT')
        
        textContent.value = `--- AI GENERATED ${instruction} ---\n\n${result}`
        showTextModal.value = true
        push.success('AI generation complete')
    } catch (e: any) {
        push.error(`AI failed: ${e.message}`)
    } finally {
        aiProcessing.value = false
    }
}
</script>

<template>
  <div v-if="item" class="h-full flex flex-col bg-background animate-in slide-in-from-right-4 duration-300">
      
      <!-- 1. Header Component -->
      <EntityHeader 
        :item="item" 
        @edit="$emit('edit')" 
        @delete="$emit('delete')" 
      />
      
      <!-- 2. Scrollable Body -->
      <div class="p-6 space-y-8 overflow-y-auto flex-1 custom-scrollbar">
          
          <!-- Warning: Archived Item -->
          <div v-if="isArchived" class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-900 p-3 rounded-lg flex items-start gap-3">
              <AlertTriangle class="text-red-600 dark:text-red-400 shrink-0 mt-0.5" :size="18" />
              <div>
                  <h4 class="text-xs font-bold text-red-800 dark:text-red-300 uppercase mb-1">Archived Item</h4>
                  <p class="text-xs text-red-700 dark:text-red-400">
                      This item is in the trash. Restore it to edit or move it.
                  </p>
              </div>
          </div>

          <!-- Description Section -->
          <div v-if="hasDescription" class="grid gap-2">
               <label class="text-xs font-bold text-muted-foreground uppercase tracking-wide">Description</label>
               <div class="text-sm text-foreground bg-muted/30 p-3 rounded-lg border leading-relaxed whitespace-pre-wrap">
                   {{ item.description }}
               </div>
          </div>

          <!-- Document Attributes -->
          <div v-if="attributes.length > 0" class="grid grid-cols-2 gap-4 p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-100 dark:border-gray-800">
              <div v-for="attr in attributes" :key="attr.uuid">
                   <label class="text-xs font-bold text-muted-foreground uppercase block mb-1 truncate" :title="attr.name">{{ attr.name }}</label>
                   <div class="text-sm font-medium truncate" :title="attr.value?.value || attr.value">
                       {{ attr.value?.value || attr.value || '-' }}
                   </div>
              </div>
          </div>

          <!-- AI & Text Tools -->
          <div v-if="isDocument" class="border-t border-dashed border-border pt-4">
              <label class="text-xs font-bold text-muted-foreground uppercase tracking-wide mb-3 flex items-center gap-2">
                  <Sparkles :size="14" class="text-purple-500" /> AI & Content
              </label>
              
              <div class="flex flex-wrap gap-2">
                  <BaseButton v-if="hasTextLayer" variant="outline" size="sm" @click="openTextModal">
                      <FileText :size="14" class="mr-1.5" /> View OCR Text
                  </BaseButton>
                  
                  <BaseButton variant="secondary" size="sm" @click="runAiTransform('SUMMARY')" :disabled="aiProcessing">
                      Summary
                  </BaseButton>
                  
                  <BaseButton variant="secondary" size="sm" @click="runAiTransform('KEY_FACTS')" :disabled="aiProcessing">
                      Key Facts
                  </BaseButton>
              </div>
          </div>

          <!-- Technical Metadata (Non-Intrusive Collapsible) -->
          <div v-if="metadata" class="border-t border-dashed border-border pt-4">
              <div 
                  class="flex items-center justify-between cursor-pointer group select-none py-1" 
                  @click="showTechnical = !showTechnical"
              >
                  <label class="text-xs font-bold text-muted-foreground uppercase tracking-wide flex items-center gap-2 group-hover:text-foreground transition-colors cursor-pointer">
                      <Database :size="14" class="text-gray-500 group-hover:text-primary transition-colors" /> Technical Details
                  </label>
                  <div class="flex items-center gap-2">
                      <BaseButton v-if="showTechnical" variant="ghost" size="sm" @click.stop="openMetadataModal" class="h-6 text-xs text-blue-600 dark:text-blue-400">
                          <Code :size="12" class="mr-1" /> View JSON
                      </BaseButton>
                      <ChevronDown :size="14" class="text-muted-foreground transition-transform duration-200" :class="{ 'rotate-180': showTechnical }" />
                  </div>
              </div>
              
              <div v-show="showTechnical" class="bg-muted/20 rounded-lg border border-border p-3 space-y-3 text-[11px] font-mono mt-3 animate-in slide-in-from-top-2 fade-in duration-200">
                  <div class="flex flex-col gap-0.5" v-if="metadata.filename">
                      <span class="text-muted-foreground font-sans font-bold uppercase text-[9px] tracking-wider">Original Filename</span>
                      <span class="text-foreground break-all">{{ metadata.filename }}</span>
                  </div>
                  <div class="flex flex-col gap-0.5" v-if="metadata.sha256">
                      <span class="text-muted-foreground font-sans font-bold uppercase text-[9px] tracking-wider flex items-center gap-1">
                          <Hash :size="10"/> SHA-256
                      </span>
                      <span class="text-foreground break-all select-all">{{ metadata.sha256 }}</span>
                  </div>
                  <div class="flex flex-col gap-0.5" v-if="metadata.sha256Canonical">
                      <span class="text-muted-foreground font-sans font-bold uppercase text-[9px] tracking-wider flex items-center gap-1">
                          <Hash :size="10"/> Canonical Hash
                      </span>
                      <span class="text-foreground break-all select-all">{{ metadata.sha256Canonical }}</span>
                  </div>
                  <div class="flex flex-wrap gap-4 mt-1">
                      <div class="flex flex-col gap-0.5" v-if="metadata.mimetype">
                          <span class="text-muted-foreground font-sans font-bold uppercase text-[9px] tracking-wider">MIME Type</span>
                          <span class="text-foreground">{{ metadata.mimetype }}</span>
                      </div>
                      <div class="flex flex-col gap-0.5" v-if="formattedSize">
                          <span class="text-muted-foreground font-sans font-bold uppercase text-[9px] tracking-wider">Size</span>
                          <span class="text-foreground">{{ formattedSize }}</span>
                      </div>
                       <div class="flex flex-col gap-0.5" v-if="metadata.source">
                          <span class="text-muted-foreground font-sans font-bold uppercase text-[9px] tracking-wider">Source</span>
                          <span class="text-foreground">{{ metadata.source }}</span>
                      </div>
                  </div>
              </div>
          </div>

          <!-- Tasks Section -->
          <div v-if="!isEmailItem && !isDataTypeItem && uuid" class="border-t border-dashed border-border pt-4"> 
             <LinkedTasksPanel :item-id="uuid" />
          </div>

          <!-- Specific: User Details -->
          <div v-if="isEmailItem" class="grid grid-cols-2 gap-6 p-4 bg-muted/20 rounded-lg border">
              <div>
                  <label class="text-xs font-bold text-muted-foreground uppercase block mb-1">Email</label>
                  <div class="text-sm font-medium">{{ safeItem.email }}</div>
              </div>
              <div>
                  <label class="text-xs font-bold text-muted-foreground uppercase block mb-1">Full Name</label>
                  <div class="text-sm font-medium">{{ safeItem.firstname || '-' }}</div>
              </div>
          </div>

          <!-- Specific: Attribute Details -->
          <div v-if="isDataTypeItem" class="grid grid-cols-2 gap-6 p-4 bg-purple-50/50 dark:bg-purple-900/10 rounded-lg border border-purple-100 dark:border-purple-800">
              <div v-if="safeItem.unit">
                  <label class="text-xs font-bold text-purple-700 dark:text-purple-400 uppercase block mb-1">Unit</label>
                  <div class="text-sm font-bold">{{ safeItem.unit }}</div>
              </div>
              <div v-if="safeItem.pattern">
                  <label class="text-xs font-bold text-purple-700 dark:text-purple-400 uppercase block mb-1">Regex Pattern</label>
                  <code class="text-xs bg-white dark:bg-black px-1.5 py-0.5 rounded border border-purple-200 dark:border-purple-800 font-mono">{{ safeItem.pattern }}</code>
              </div>
          </div>
      </div>
      
      <!-- Text Viewer Modal -->
      <ModalDialog :isOpen="showTextModal" title="Document Content" @close="showTextModal = false">
          <div class="h-[60vh] w-full flex flex-col">
              <div v-if="isFetchingText" class="flex items-center justify-center h-full">
                  <BaseSpinner class="text-primary" :size="32" />
              </div>
              <textarea 
                v-else 
                readonly 
                class="flex-1 w-full p-3 font-mono text-xs bg-gray-50 dark:bg-gray-900 border rounded resize-none focus:outline-none"
                :value="textContent"
              ></textarea>
          </div>
          <template #footer>
             <BaseButton @click="showTextModal = false">Close</BaseButton>
          </template>
      </ModalDialog>

      <!-- JSON Metadata Modal -->
      <ModalDialog :isOpen="showMetadataModal" title="Raw Document Metadata" @close="showMetadataModal = false">
          <div class="h-[60vh] w-full flex flex-col">
              <div v-if="isFetchingMetadata" class="flex items-center justify-center h-full">
                  <BaseSpinner class="text-primary" :size="32" />
              </div>
              <textarea 
                v-else 
                readonly 
                class="flex-1 w-full p-3 font-mono text-xs bg-gray-50 dark:bg-gray-900 border rounded resize-none focus:outline-none whitespace-pre"
                :value="metadataContent"
              ></textarea>
          </div>
          <template #footer>
             <BaseButton variant="outline" @click="showMetadataModal = false">Close</BaseButton>
             <BaseButton variant="default" @click="downloadMetadataJson" :disabled="!metadataContent">
                 <Download :size="14" class="mr-2" /> Download JSON
             </BaseButton>
          </template>
      </ModalDialog>

  </div>
  
  <div v-else class="h-full flex items-center justify-center text-muted-foreground italic">
      No item selected
  </div>
</template>