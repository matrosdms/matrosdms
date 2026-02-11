<script setup lang="ts">
import { ref, computed } from 'vue'
import type { AnyEntity } from '@/types/models'
import EntityHeader from '@/components/panels/EntityHeader.vue'
import LinkedTasksPanel from '@/components/panels/LinkedTasksPanel.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSpinner from '@/components/ui/BaseSpinner.vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { ItemService } from '@/services/ItemService'
import { Sparkles, FileText, AlertTriangle } from 'lucide-vue-next'
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
    return [];
})

// --- ACTIONS ---

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
        
        // Show result in a modal or just append to description?
        // For now, let's re-use the text modal to show the result
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

          <!-- AI & Text Tools (New) -->
          <div v-if="isDocument" class="border-t border-dashed border-border pt-4">
              <label class="text-xs font-bold text-muted-foreground uppercase tracking-wide mb-3 block flex items-center gap-2">
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

          <!-- Tasks Section -->
          <div v-if="!isEmailItem && !isDataTypeItem && uuid"> 
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
  </div>
  
  <div v-else class="h-full flex items-center justify-center text-muted-foreground italic">
      No item selected
  </div>
</template>