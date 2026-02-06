<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import IconPicker from '@/components/ui/IconPicker.vue'
import AppTag from '@/components/ui/AppTag.vue'
import { useDmsStore } from '@/stores/dms'
import { ContextService } from '@/services/ContextService'
import { queryKeys } from '@/composables/queries/queryKeys'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { EStage, EStageList, EStageLabels, type EStageType } from '@/enums'

const dms = useDmsStore()
const queryClient = useQueryClient()

// --- Types ---
interface DimensionTag {
    id: string
    label: string
    dimension: string
}

// --- State ---
const touched = ref(false)
const isLoading = ref(false)
const isDragOver = ref(false)
const form = ref({ 
    name: '', 
    description: '', 
    icon: '', 
    stage: EStage.ACTIVE as EStageType, 
    version: 0 
})

// Single flat list of all tags with their dimension info
const activeTags = ref<DimensionTag[]>([])

const isValid = computed(() => form.value.name.trim().length > 0)

// --- Initialization ---
const loadContextData = () => {
    const ctx = dms.selectedContext
    if (!ctx) return
    
    form.value = { 
        name: ctx.name || '', 
        description: ctx.description || '', 
        icon: ctx.icon || '',
        stage: (ctx.stage as EStageType) || EStage.ACTIVE,
        version: ctx.version || 0
    }
    
    // Load existing tags from dictionary
    const dict = ctx.dictionary
    if (dict && typeof dict === 'object') {
        const loadedTags: DimensionTag[] = []
        
        Object.entries(dict).forEach(([dimension, categories]) => {
            if (Array.isArray(categories)) {
                categories.forEach((cat: any) => {
                    if (cat?.uuid && cat?.name) {
                        loadedTags.push({
                            id: cat.uuid,
                            label: cat.name,
                            dimension
                        })
                    }
                })
            }
        })
        
        activeTags.value = loadedTags
    }
}

onMounted(() => {
    loadContextData()
})

// Reload if context changes
watch(() => dms.selectedContext, () => {
    loadContextData()
}, { deep: true })

// --- Logic ---
const removeTag = (tagId: string) => {
    activeTags.value = activeTags.value.filter(t => t.id !== tagId)
}

const handleDragOver = (e: DragEvent) => {
    e.preventDefault()
    isDragOver.value = true
}

const handleDragLeave = () => {
    isDragOver.value = false
}

const handleDrop = (event: DragEvent) => {
    isDragOver.value = false
    const rawData = event.dataTransfer?.getData('application/json')
    if (!rawData) return

    try {
        const data = JSON.parse(rawData)
        
        // Validate it's a category node
        if (data.type !== 'category-node') {
            push.warning('Only category items can be dropped here')
            return
        }
        
        // Prevent duplicates
        if (activeTags.value.some(t => t.id === data.id)) {
            push.info(`'${data.label}' is already assigned.`)
            return
        }
        
        // Add with dimension info
        activeTags.value.push({ 
            id: data.id, 
            label: data.label,
            dimension: data.rootType || 'UNKNOWN'
        })
    } catch (e) {
        console.error("Drop Parse Error", e)
    }
}

const onSubmit = async () => {
    if (!isValid.value) return
    isLoading.value = true
    
    try {
        if (!dms.selectedContext?.uuid) {
            push.error('No context selected')
            return
        }

        // Collect all tag IDs
        const categoryList = activeTags.value.map(t => t.id)

        const payload = { 
            name: form.value.name,
            description: form.value.description,
            icon: form.value.icon,
            stage: form.value.stage,
            categoryList,
            version: form.value.version 
        }

        console.log('Updating context with payload:', payload)

        await ContextService.update(dms.selectedContext.uuid, payload as any)
        
        // Invalidate using proper query key
        await queryClient.invalidateQueries({ queryKey: queryKeys.context.all })
        
        push.success('Context updated')
        dms.cancelCreation()
    } catch(err: any) {
        console.error('Context update error:', err)
        if (!err.message?.includes('409')) {
            push.error(`Failed: ${err.message}`)
        } else {
            push.warning('Conflict: The context was modified by someone else. Please refresh.')
        }
    } finally { 
        isLoading.value = false 
    }
}
</script>

<template>
  <BaseFormPanel title="Edit Context" :subtitle="dms.selectedContext?.name" :is-loading="isLoading" @submit="onSubmit" @cancel="dms.cancelCreation">
      
      <BaseInput 
        v-model="form.name" 
        label="Name" 
        autofocus 
        :error="touched && !isValid ? 'Name is required' : ''" 
        @blur="touched = true" 
      />
      
      <div class="flex gap-4 items-end">
          <div class="w-32 shrink-0">
            <IconPicker v-model="form.icon" />
          </div>
          <div class="flex-1">
            <BaseSelect v-model="form.stage" label="Stage">
                <option v-for="opt in EStageList" :key="opt" :value="opt">{{ EStageLabels[opt] }}</option>
            </BaseSelect>
          </div>
      </div>
      
      <BaseTextarea 
        v-model="form.description" 
        label="Description" 
        :rows="3" 
      />

      <!-- Single Unified Drop Zone for All Dimensions -->
      <div class="space-y-2 pt-4 border-t border-border mt-4">
          <div class="flex items-center justify-between">
              <h3 class="text-xs font-bold text-muted-foreground uppercase tracking-wide">
                  Categories
              </h3>
              <span class="text-[9px] text-muted-foreground/60">(Drag from sidebar)</span>
          </div>
          
          <!-- Drop Zone -->
          <div
            :class="[
                'w-full min-h-[80px] border-2 border-dashed rounded-lg p-2',
                'flex flex-wrap items-start content-start gap-1.5',
                'transition-all duration-150',
                isDragOver 
                    ? 'border-primary bg-primary/5 ring-2 ring-primary/20' 
                    : 'border-border hover:border-muted-foreground/30 bg-muted/20'
            ]"
            @dragover="handleDragOver"
            @dragleave="handleDragLeave"
            @drop.prevent="handleDrop"
          >
              <!-- Display existing tags -->
              <AppTag 
                v-for="tag in activeTags" 
                :key="tag.id" 
                :root-id="tag.dimension" 
                :label="tag.label"
                :removable="true"
                @remove="removeTag(tag.id)"
              />

              <!-- Empty state -->
              <div v-if="activeTags.length === 0" class="w-full h-full flex items-center justify-center py-4">
                  <span class="text-xs text-muted-foreground/50 italic">
                      {{ isDragOver ? 'Release to add category' : 'Drop categories here to assign them' }}
                  </span>
              </div>
          </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3 w-full">
            <BaseButton variant="outline" @click="dms.cancelCreation" :disabled="isLoading">Cancel</BaseButton>
            <BaseButton variant="default" @click="onSubmit" :disabled="isLoading || !isValid">Save Changes</BaseButton>
        </div>
      </template>

  </BaseFormPanel>
</template>