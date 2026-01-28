<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: any
  autofocus?: boolean
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const internalValue = ref(props.modelValue === true || props.modelValue === 'true')

watch(() => props.modelValue, (val) => {
    internalValue.value = val === true || val === 'true'
})

const toggle = () => {
    internalValue.value = !internalValue.value
    emit('update:modelValue', internalValue.value)
}
</script>

<template>
  <div class="flex items-center h-9 px-1 w-full bg-white dark:bg-gray-900 rounded-md border border-gray-300 dark:border-gray-700 shadow-sm transition-colors hover:bg-gray-50 dark:hover:bg-gray-800">
    <label class="flex items-center cursor-pointer gap-2.5 w-full p-1.5 h-full">
      <input 
        type="checkbox" 
        :checked="internalValue" 
        @change="toggle"
        class="h-4 w-4 text-blue-600 rounded border-gray-300 dark:border-gray-600 focus:ring-blue-500 dark:bg-gray-700" 
      />
      <span class="text-sm text-gray-700 dark:text-gray-200 select-none font-medium">
        {{ internalValue ? 'Yes (True)' : 'No (False)' }}
      </span>
    </label>
  </div>
</template>