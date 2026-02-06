<script setup lang="ts">
import { ref } from 'vue'
import ItemFormFields from '@/components/forms/ItemFormFields.vue'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { useDmsStore } from '@/stores/dms'
import { useItemForm } from '@/composables/useItemForm'
import { ItemService } from '@/services/ItemService'
import { useHotkeys } from '@/composables/useHotkeys'
import { push } from 'notivue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { Trash2, FileText, Eye, AlertTriangle, Columns } from 'lucide-vue-next'

const dms = useDmsStore()
const queryClient = useQueryClient()
const activeTab = ref<'metadata' | 'preview' | 'split'>('split')
const showDeleteModal = ref(false)

const { form, touched, isLoading, save } = useItemForm(true)

const { mutate: deleteItem, isPending: isDeleting } = useMutation({
    mutationFn: async () => {
        if(!dms.selectedItem?.uuid) return;
        await ItemService.archive(dms.selectedItem.uuid)
    },
    onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['items'] })
        queryClient.invalidateQueries({ queryKey: ['contexts'] })
        push.success('Document moved to trash')
        showDeleteModal.value = false
        dms.setSelectedItem(null)
        dms.cancelCreation()
    },
    onError: (e: any) => {
        showDeleteModal.value = false
        push.error(`Deletion failed: ${e.message}`)
    }
})

// Shortcuts
useHotkeys(['s', 'S'], () => {
    if (!isLoading.value && !isDeleting.value && form.value.name) save()
}, { ctrl: true, prevent: true })

useHotkeys('Escape', () => {
    if (!isLoading.value && !isDeleting.value) {
        if (showDeleteModal.value) showDeleteModal.value = false;
        else dms.cancelCreation();
    }
})
</script>

<template>
  <div class="h-full flex flex-col bg-background overflow-hidden transition-colors">
      <!-- HEADER -->
      <div class="bg-background border-b border-border h-[45px] flex items-center justify-between px-4 shrink-0 shadow-sm">
          <div class="font-bold text-foreground text-sm">Edit Document</div>
          
          <div class="flex gap-2 items-center">
              <div class="flex bg-muted/50 rounded-md p-0.5 border border-border transition-colors">
                  <button @click="activeTab = 'split'" class="hidden md:flex p-1.5 rounded transition-all" :class="activeTab === 'split' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'" title="Split View"><Columns :size="14" /></button>
                  <button @click="activeTab = 'preview'" class="p-1.5 rounded transition-all" :class="activeTab === 'preview' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'" title="Document Only"><Eye :size="14" /></button>
                  <button @click="activeTab = 'metadata'" class="p-1.5 rounded transition-all" :class="activeTab === 'metadata' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'" title="Form Only"><FileText :size="14" /></button>
              </div>
          </div>
      </div>

      <!-- BODY -->
      <div class="flex-1 overflow-hidden relative">
          
          <!-- SPLIT VIEW -->
          <div v-if="activeTab === 'split'" class="flex h-full w-full">
              <div class="w-7/12 h-full bg-muted/10 border-r border-border relative">
                  <DocumentPreview 
                    :identifier="dms.selectedItem?.uuid || ''" 
                    source="item" 
                    :file-name="dms.selectedItem?.name || form.name"
                  />
              </div>
              <div class="w-5/12 h-full bg-background overflow-y-auto custom-scrollbar p-6">
                  <div class="max-w-xl mx-auto space-y-4">
                      <ItemFormFields v-model="form" :touched="touched" />
                      
                      <!-- Inline Actions -->
                      <div class="pt-6 flex justify-between items-center border-t border-dashed border-border mt-4">
                          <div>
                              <BaseButton 
                                variant="destructive"
                                size="sm"
                                @click="showDeleteModal = true"
                                :disabled="isDeleting || isLoading"
                              >
                                <Trash2 :size="14" class="mr-1" /> Delete
                              </BaseButton>
                          </div>
                          <div class="flex gap-2 items-center">
                              <span class="text-[10px] text-muted-foreground hidden xl:inline mr-2"><kbd class="font-mono border rounded px-1 bg-muted">Ctrl+S</kbd></span>
                              <BaseButton variant="outline" size="sm" @click="dms.cancelCreation" :disabled="isLoading || isDeleting">Cancel</BaseButton>
                              <BaseButton variant="default" size="sm" @click="save" :disabled="isLoading || isDeleting || !form.name">Save Changes</BaseButton>
                          </div>
                      </div>
                  </div>
              </div>
          </div>

          <!-- FULL PREVIEW -->
          <div v-else-if="activeTab === 'preview'" class="h-full w-full bg-muted/10">
              <DocumentPreview 
                :identifier="dms.selectedItem?.uuid || ''" 
                source="item" 
                :file-name="dms.selectedItem?.name || form.name"
              />
          </div>

          <!-- FULL FORM -->
          <div v-else class="h-full w-full bg-background overflow-y-auto custom-scrollbar p-6">
              <div class="max-w-2xl mx-auto">
                  <ItemFormFields v-model="form" :touched="touched" />
                  
                  <!-- Inline Actions -->
                  <div class="pt-6 flex justify-between items-center border-t border-dashed border-border mt-4">
                      <div>
                          <BaseButton 
                            variant="destructive"
                            size="sm"
                            @click="showDeleteModal = true"
                            :disabled="isDeleting || isLoading"
                          >
                            <Trash2 :size="14" class="mr-1" /> Delete
                          </BaseButton>
                      </div>
                      <div class="flex gap-2 items-center">
                          <span class="text-[10px] text-muted-foreground hidden md:inline mr-2">
                              <kbd class="font-mono border rounded px-1 bg-muted">Ctrl+S</kbd> Save
                          </span>
                          <BaseButton variant="outline" size="sm" @click="dms.cancelCreation" :disabled="isLoading || isDeleting">Cancel</BaseButton>
                          <BaseButton variant="default" size="sm" @click="save" :disabled="isLoading || isDeleting || !form.name">Save Changes</BaseButton>
                      </div>
                  </div>
              </div>
          </div>
      </div>

      <!-- DELETE MODAL -->
      <ModalDialog :isOpen="showDeleteModal" title="Delete Document" @close="showDeleteModal = false">
          <div class="flex gap-4">
              <div class="w-12 h-12 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center shrink-0">
                  <AlertTriangle class="text-red-600 dark:text-red-400" :size="24" />
              </div>
              <div>
                  <p class="text-sm text-foreground font-bold mb-2">Are you sure you want to delete "{{ form.name }}"?</p>
                  <p class="text-xs text-muted-foreground mb-4">This action will move the document to the trash. It can be recovered later if needed.</p>
                  <div class="flex gap-3 justify-end mt-4">
                      <BaseButton variant="outline" @click="showDeleteModal = false">Cancel</BaseButton>
                      <BaseButton variant="destructive" @click="() => deleteItem()">Yes, Delete</BaseButton>
                  </div>
              </div>
          </div>
      </ModalDialog>
  </div>
</template>