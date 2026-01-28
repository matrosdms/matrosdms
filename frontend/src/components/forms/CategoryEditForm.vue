<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import CategoryTree from '@/components/navigation/CategoryTree.vue' 
import IconPicker from '@/components/ui/IconPicker.vue' // NEW
import ModalDialog from '@/components/ui/ModalDialog.vue'
import { useDmsStore } from '@/stores/dms'
import { push } from 'notivue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { CategoryService } from '@/services/CategoryService'
import { FolderInput, Trash2, X, AlertTriangle } from 'lucide-vue-next'

interface CategoryNode { id: string; label: string; }

const dms = useDmsStore()
const queryClient = useQueryClient()
const form = ref({ name: '', description: '', icon: '', parentId: '', parentName: '' })
const isLoading = ref(false)
const touched = ref(false)
const isPickerOpen = ref(false)
const showDeleteModal = ref(false)
const isValid = computed(() => form.value.name.trim().length > 0)

onMounted(async () => {
  if (!dms.selectedCategoryId) return
  isLoading.value = true
  try {
    const data = await CategoryService.getById(dms.selectedCategoryId)
    let pId = '', pName = 'Root / No Parent'
    if (data.parents && data.parents.length > 0) {
        const parent = data.parents[data.parents.length - 1]
        pId = parent.uuid || ''; pName = parent.name || ''
    }
    form.value = { name: data.name || '', description: data.description || '', icon: data.icon || '', parentId: pId, parentName: pName }
  } catch (e) { push.error('Failed to load details') } finally { isLoading.value = false }
})

const { mutate: updateCategory, isPending } = useMutation({
    mutationFn: async () => {
        if (!dms.selectedCategoryId) return
        await CategoryService.update(dms.selectedCategoryId, {
            name: form.value.name,
            description: form.value.description,
            icon: form.value.icon,
            parentIdentifier: form.value.parentId || undefined
        })
    },
    onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['category'] })
        push.success('Category updated')
        dms.cancelCreation()
    },
    onError: (e: any) => push.error(`Update failed: ${e.message}`)
})

const { mutate: deleteCategory, isPending: isDeleting } = useMutation({
    mutationFn: async () => {
        if (!dms.selectedCategoryId) return
        await CategoryService.delete(dms.selectedCategoryId)
    },
    onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['category'] })
        push.success('Category deleted')
        dms.setSelectedCategory(null)
        dms.cancelCreation()
        showDeleteModal.value = false
    },
    onError: (e: any) => push.error(`Cannot delete: ${e.message}`)
})

const onSubmit = () => { if (!isValid.value) { touched.value = true; return }; updateCategory() }
const onParentSelected = (node: CategoryNode) => {
    if (node.id === dms.selectedCategoryId) { push.warning("Cannot move a category into itself"); return }
    form.value.parentId = node.id; form.value.parentName = node.label; isPickerOpen.value = false
}
</script>

<template>
  <BaseFormPanel title="Edit Category" :subtitle="dms.selectedCategoryLabel" :is-loading="isLoading || isPending || isDeleting" @submit="onSubmit" @cancel="dms.cancelCreation">
      <div class="relative z-20 space-y-1.5">
          <label class="text-sm font-medium text-foreground">Parent Category (Move)</label>
          <div class="flex gap-2">
              <div @click="isPickerOpen = !isPickerOpen" class="flex-1 border border-input rounded-md p-2 text-sm bg-background cursor-pointer hover:bg-muted/50 transition-colors flex items-center gap-2">
                  <div class="w-2 h-2 rounded-full bg-blue-500"></div>
                  <span class="font-medium text-foreground">{{ form.parentName || 'Select Parent...' }}</span>
              </div>
              <BaseButton variant="outline" size="icon" @click="isPickerOpen = !isPickerOpen">
                  <FolderInput :size="18" />
              </BaseButton>
          </div>
          
          <div v-if="isPickerOpen" class="absolute top-full left-0 w-full mt-1 bg-popover border border-input shadow-xl rounded-md max-h-64 overflow-hidden flex flex-col z-50 animate-in fade-in zoom-in-95 duration-100">
              <div class="bg-muted/50 p-2 border-b border-input flex justify-between items-center">
                  <span class="text-xs font-bold text-muted-foreground uppercase">Select new Location</span>
                  <button @click="isPickerOpen = false"><X :size="14"/></button>
              </div>
              <CategoryTree 
                  :selectionMode="true" 
                  :selectedId="form.parentId" 
                  @node-selected="onParentSelected" 
              />
          </div>
      </div>

      <BaseInput v-model="form.name" label="Name" autofocus :error="touched && !isValid ? 'Name is required' : ''" @blur="touched = true" />
      
      <!-- Icon Picker -->
      <IconPicker v-model="form.icon" />
      
      <div>
          <label class="text-sm font-medium text-foreground block mb-1.5">Description</label>
          <textarea v-model="form.description" rows="5" class="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 resize-y"></textarea>
      </div>

      <template #footer>
        <div class="flex justify-between items-center w-full">
            <BaseButton type="button" variant="destructive" @click="showDeleteModal = true" :loading="isDeleting">
                <Trash2 :size="16" class="mr-2" /> Delete
            </BaseButton>
            <div class="flex gap-3">
                <BaseButton variant="outline" @click="dms.cancelCreation" :disabled="isLoading || isPending">Cancel</BaseButton>
                <BaseButton variant="default" @click="onSubmit" :disabled="isLoading || isPending || !isValid">Save Changes</BaseButton>
            </div>
        </div>
      </template>

      <ModalDialog :isOpen="showDeleteModal" title="Delete Category" @close="showDeleteModal = false">
          <div class="flex gap-4">
              <div class="w-12 h-12 bg-red-100 dark:bg-red-900/30 rounded-full flex items-center justify-center shrink-0"><AlertTriangle class="text-red-600 dark:text-red-400" :size="24" /></div>
              <div>
                  <p class="text-sm text-foreground font-bold mb-2">Are you sure you want to delete "{{ form.name }}"?</p>
                  <div class="flex gap-3 justify-end mt-4">
                      <BaseButton variant="outline" @click="showDeleteModal = false">Cancel</BaseButton>
                      <BaseButton variant="destructive" @click="() => deleteCategory()">Yes, Delete</BaseButton>
                  </div>
              </div>
          </div>
      </ModalDialog>
  </BaseFormPanel>
</template>