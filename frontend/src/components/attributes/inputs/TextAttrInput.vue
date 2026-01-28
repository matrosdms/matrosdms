<script setup lang="ts">
import { onMounted, ref } from 'vue'

const props = defineProps<{
  modelValue: any
  placeholder?: string
  autofocus?: boolean
}>()

const emit = defineEmits(['update:modelValue', 'save'])
const inputRef = ref<HTMLInputElement | null>(null)

onMounted(() => {
    if (props.autofocus) setTimeout(() => inputRef.value?.focus(), 50)
})
</script>

<template>
  <div class="w-full bg-white dark:bg-gray-900 rounded-md border border-gray-300 dark:border-gray-700 shadow-sm focus-within:ring-2 focus-within:ring-blue-100 dark:focus-within:ring-blue-900 focus-within:border-blue-500 transition-all">
    <input 
        ref="inputRef"
        type="text" 
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        class="w-full h-9 px-3 text-sm outline-none bg-transparent placeholder:text-gray-400 text-gray-900 dark:text-gray-100" 
        :placeholder="placeholder || 'Enter value...'" 
        @keydown.enter.prevent="$emit('save')"
    />
  </div>
</template>