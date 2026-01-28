<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'
import {
  EAttributeType,
  EAttributeTypeList,
  EAttributeTypeLabels,
  type EAttributeTypeType
} from '@/enums'

const props = defineProps<{
  initialData: any
}>()

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

// Check if current type is supported for editing
const isTypeSupported = computed(() => {
  return EAttributeTypeList.includes(props.initialData?.dataType)
})

const populateForm = () => {
  if (props.initialData) {
    form.value = {
      name: props.initialData.name || '',
      description: props.initialData.description || '',
      key: props.initialData.key || (props.initialData.name ? props.initialData.name.toUpperCase().replace(/[^A-Z0-9_]/g, '_') : ''),
      type: (EAttributeTypeList.includes(props.initialData.dataType) ? props.initialData.dataType : EAttributeType.TEXT) as EAttributeTypeType,
      unit: props.initialData.unit || '',
      pattern: props.initialData.pattern || ''
    }
  }
}

watch(() => props.initialData, populateForm, { immediate: true })

const onSubmit = async () => {
  if (!form.value.name || !form.value.key) return push.warning('Name and Key are required')
  isLoading.value = true
  try {
    if (!props.initialData?.uuid) return
    await AttributeTypeService.update(props.initialData.uuid, form.value)
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.attributes })
    push.success('Attribute Type updated')
    emit('close')
  } catch (e: any) {
    push.error(e.message)
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <BaseFormPanel title="Edit Attribute Definition" :is-loading="isLoading" @submit="onSubmit" @cancel="$emit('close')">

    <div v-if="!isTypeSupported" class="bg-yellow-50 border border-yellow-200 text-yellow-800 text-xs p-3 rounded">
        <strong>Warning:</strong> This attribute is of type <code>{{ initialData.dataType }}</code> which cannot be modified here.
    </div>

    <BaseInput v-model="form.name" label="Name" placeholder="e.g. Invoice Amount" autofocus />
    
    <BaseInput v-model="form.key" label="System Key" disabled class="bg-muted font-mono uppercase" />

    <BaseSelect v-model="form.type" label="Data Type" :disabled="!isTypeSupported">
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