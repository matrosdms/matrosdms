<script setup lang="ts">
import { ref, computed } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import IconPicker from '@/components/ui/IconPicker.vue' // NEW
import { useDmsStore } from '@/stores/dms'
import { CategoryService } from '@/services/CategoryService'
import { push } from 'notivue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'

const dms = useDmsStore()
const queryClient = useQueryClient()
const form = ref({ name: '', description: '', icon: '' })
const parentId = computed(() => dms.parentCategoryForCreation)
const touched = ref(false)
const isValid = computed(() => form.value.name.trim().length > 0)

const { mutate: createCategory, isPending } = useMutation({
  mutationFn: async () => {
    if (!parentId.value) throw new Error("Parent ID missing")
    const payload = { name: form.value.name, description: form.value.description, icon: form.value.icon, object: false }
    await CategoryService.create(parentId.value, payload)
  },
  onSuccess: () => { push.success("Category created"); queryClient.invalidateQueries({ queryKey: ['category'] }); dms.finishTask() },
  onError: (e: any) => { push.error(`Creation failed: ${e.message}`) }
})
const onSubmit = () => { if (!isValid.value) { touched.value = true; return; }; createCategory() }
</script>

<template>
  <BaseFormPanel title="Create New Category" :subtitle="`Parent ID: ${parentId}`" :is-loading="isPending" @submit="onSubmit" @cancel="dms.finishTask">
      <BaseInput 
        v-model="form.name" 
        label="Name" 
        autofocus 
        :error="touched && !isValid ? 'Name is required' : ''" 
        @blur="touched = true" 
      />
      
      <!-- Icon Picker Replaced BaseInput -->
      <IconPicker v-model="form.icon" />
      
      <BaseTextarea 
        v-model="form.description" 
        label="Description" 
        :rows="5" 
      />

      <template #footer>
        <div class="flex justify-end gap-3 w-full">
            <BaseButton variant="outline" @click="dms.finishTask" :disabled="isPending">Cancel</BaseButton>
            <BaseButton variant="default" @click="onSubmit" :disabled="isPending || !isValid">Create Category</BaseButton>
        </div>
      </template>
  </BaseFormPanel>
</template>