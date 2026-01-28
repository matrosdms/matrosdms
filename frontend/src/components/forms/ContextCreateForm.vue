<script setup lang="ts">
import { ref, computed } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import IconPicker from '@/components/ui/IconPicker.vue'
import { useDmsStore } from '@/stores/dms'
import { useConfigStore } from '@/stores/config'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { ContextService } from '@/services/ContextService'
import { push } from 'notivue'
import { getTagStyle } from '@/utils/tagStyles'
import { EStage, EStageList, EStageLabels, type EStageType } from '@/enums'

const dms = useDmsStore()
const config = useConfigStore()
const queryClient = useQueryClient()
const touched = ref(false)

const form = ref({ 
    name: '', 
    icon: '', 
    description: '', 
    categoryList: [] as string[], 
    stage: EStage.ACTIVE as EStageType
})

const allowedDimensions = computed(() => config.contextDimensions)
const isValid = computed(() => form.value.name.trim().length > 0)

const { mutate: createContext, isPending } = useMutation({
  mutationFn: async () => {
    const categoryList = allowedDimensions.value.flatMap(key => 
        (dms.filters[key] || []).map(t => t.id)
    );

    const payload = {
        name: form.value.name, 
        description: form.value.description, 
        icon: form.value.icon, 
        categoryList,
        stage: form.value.stage
    }

    await ContextService.create(payload)
  },
  onSuccess: () => { 
    queryClient.invalidateQueries({ queryKey: ['contexts'] })
    push.success(`Context '${form.value.name}' created`)
    dms.cancelCreation()
  },
  onError: (e: any) => push.error(`Error: ${e.message}`)
})
</script>

<template>
  <BaseFormPanel title="Create New Context" :is-loading="isPending" @submit="createContext" @cancel="dms.cancelCreation">
      <BaseInput 
        v-model="form.name" 
        label="Name" 
        placeholder="e.g. Invoices 2024" 
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
      
      <BaseTextarea v-model="form.description" label="Description" :rows="5" />

      <div class="p-3 bg-muted/20 rounded-md border border-muted">
          <p class="text-xs font-bold text-muted-foreground uppercase tracking-wide mb-2">Auto-assigned Tags</p>
          <div class="flex gap-2 flex-wrap">
            <template v-for="cat in allowedDimensions" :key="cat">
                <div v-for="t in dms.filters[cat]" :key="t.id" class="px-2 py-0.5 rounded text-[10px] font-bold border" :style="getTagStyle(cat)">
                    {{ t.label }}
                </div>
            </template>
            <span v-if="!allowedDimensions.some(cat => dms.filters[cat]?.length)" class="text-xs text-muted-foreground italic">
                None selected (Tags selected in the sidebar will be applied).
            </span>
          </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3 w-full">
            <BaseButton variant="outline" @click="dms.cancelCreation" :disabled="isPending">Cancel</BaseButton>
            <BaseButton variant="default" @click="createContext" :loading="isPending" :disabled="!isValid">Save Context</BaseButton>
        </div>
      </template>
  </BaseFormPanel>
</template>