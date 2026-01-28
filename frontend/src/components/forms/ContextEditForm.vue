<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import IconPicker from '@/components/ui/IconPicker.vue' // NEW
import { useDmsStore } from '@/stores/dms'
import { useConfigStore } from '@/stores/config'
import { ContextService } from '@/services/ContextService'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { getTagClassByKey } from '@/utils/tagStyles'
import { EStage, EStageList, EStageLabels, type EStageType } from '@/enums'

const dms = useDmsStore()
const config = useConfigStore()
const queryClient = useQueryClient()
const touched = ref(false)
const isLoading = ref(false)
const form = ref({ name: '', description: '', icon: '', stage: EStage.ACTIVE as EStageType, version: 0 })
const allowedDimensions = computed(() => config.contextDimensions)
const isValid = computed(() => form.value.name.trim().length > 0)

onMounted(() => {
  if (dms.selectedContext) {
    form.value = { 
        name: dms.selectedContext.name || '', 
        description: dms.selectedContext.description || '', 
        icon: dms.selectedContext.icon || '',
        stage: (dms.selectedContext.stage as EStageType) || EStage.ACTIVE,
        version: dms.selectedContext.version || 0
    }
  }
})

const onSubmit = async () => {
  if (!isValid.value) return;
  isLoading.value = true
  try {
    if (!dms.selectedContext?.uuid) return;
    const payload = { ...form.value, stage: form.value.stage, categoryList: [], version: form.value.version }
    await ContextService.update(dms.selectedContext.uuid, payload as any)
    queryClient.invalidateQueries({ queryKey: ['contexts'] })
    if (dms.selectedContext) Object.assign(dms.selectedContext, { ...payload, version: (form.value.version || 0) + 1 })
    push.success('Context updated'); dms.cancelCreation()
  } catch(err: any) { if (!err.message?.includes('409')) push.error(`Failed: ${err.message}`) } finally { isLoading.value = false }
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
        :rows="8" 
      />

      <div class="p-3 bg-muted/20 rounded-md border border-muted">
          <p class="text-xs font-bold text-muted-foreground uppercase tracking-wide mb-2">Attached Tags</p>
          <div class="flex gap-2 flex-wrap">
            <template v-for="cat in allowedDimensions" :key="cat">
                <div v-for="t in dms.filters[cat]" :key="t.id" class="px-2 py-0.5 rounded text-[10px] font-bold border" :style="getTagClassByKey(cat)">
                    {{ t.label }}
                </div>
            </template>
            <span v-if="!allowedDimensions.some(cat => dms.filters[cat]?.length)" class="text-xs text-muted-foreground italic">
                Current Selection in Filter Bar
            </span>
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