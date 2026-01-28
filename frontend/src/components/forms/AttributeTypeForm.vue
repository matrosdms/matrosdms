<script setup lang="ts">
import { ref } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'
import { EAttributeType, EAttributeTypeList, EAttributeTypeLabels, type EAttributeTypeType } from '@/enums'

const emit = defineEmits(['close'])
const queryClient = useQueryClient()

const form = ref({
  name: '',
  description: '',
  key: '',
  type: EAttributeType.TEXT as EAttributeTypeType,
  unit: '',
  pattern: ''
})
const isLoading = ref(false)

// Auto-generate key from name if key is empty
const onNameInput = (val: string) => {
  if (!form.value.key || form.value.key === val.toUpperCase().replace(/\s+/g, '').slice(0, val.length-1)) {
    form.value.key = val.toUpperCase().replace(/[^A-Z0-9_]/g, '')
  }
}

const onSubmit = async () => {
  if (!form.value.name || !form.value.key) return push.warning('Name and Key are required')
  isLoading.value = true
  try {
    await AttributeTypeService.create(form.value)
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.attributes })
    push.success('Attribute Type created')
    emit('close')
  } catch (e: any) {
    push.error(e.message)
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <BaseFormPanel title="New Attribute Definition" :is-loading="isLoading" @submit="onSubmit" @cancel="$emit('close')">
    
    <BaseInput 
      :model-value="form.name" 
      @update:model-value="(v) => { form.name = String(v); onNameInput(String(v)) }"
      label="Name" 
      placeholder="e.g. Invoice Amount" 
      autofocus
    />
    
    <BaseInput v-model="form.key" label="System Key" placeholder="INVOICE_AMOUNT" class="font-mono uppercase" />

    <BaseSelect v-model="form.type" label="Data Type">
        <option v-for="opt in EAttributeTypeList" :key="opt" :value="opt">{{ EAttributeTypeLabels[opt] }}</option>
    </BaseSelect>

    <div v-if="[EAttributeType.NUMBER, EAttributeType.CURRENCY].includes(form.type)">
        <BaseInput v-model="form.unit" label="Unit" placeholder="e.g. € or kg" />
    </div>

    <div v-if="form.type === EAttributeType.TEXT">
        <BaseInput v-model="form.pattern" label="Regex Pattern" placeholder="Optional validation regex" class="font-mono text-xs" />
    </div>

    <BaseTextarea v-model="form.description" label="Description" :rows="3" />
  </BaseFormPanel>
</template>