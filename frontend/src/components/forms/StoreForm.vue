<script setup>
import { ref, onMounted } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import IconPicker from '@/components/ui/IconPicker.vue'
import { StoreService } from '@/services/StoreService'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'

const props = defineProps({
  initialData: Object
})

const emit = defineEmits(['close'])
const queryClient = useQueryClient()
const form = ref({ name: '', shortname: '', description: '', icon: '' })
const isLoading = ref(false)

const isEdit = !!props.initialData

onMounted(() => {
  if (props.initialData) {
    form.value = { 
        name: props.initialData.name || '',
        shortname: props.initialData.shortname || '',
        description: props.initialData.description || '',
        icon: props.initialData.icon || ''
    }
  }
})

const onSubmit = async () => {
  if (!form.value.name) return push.warning('Name is required')
  isLoading.value = true
  try {
    if (isEdit) {
      await StoreService.update(props.initialData.uuid, form.value)
      push.success('Store updated')
    } else {
      await StoreService.create(form.value)
      push.success('Store created')
    }
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.stores })
    emit('close')
  } catch (e) {
    push.error(e.message)
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <BaseFormPanel :title="isEdit ? 'Edit Store' : 'New Physical Store'" :is-loading="isLoading" @submit="onSubmit" @cancel="$emit('close')">
    <BaseInput v-model="form.name" label="Name" placeholder="e.g. Basement Archive" autofocus />
    
    <div class="flex gap-4">
        <div class="flex-1">
            <BaseInput v-model="form.shortname" label="Short Code" placeholder="e.g. LOC-01" />
        </div>
        <div class="flex-1">
            <IconPicker v-model="form.icon" />
        </div>
    </div>

    <BaseTextarea v-model="form.description" label="Description" :rows="4" />
  </BaseFormPanel>
</template>