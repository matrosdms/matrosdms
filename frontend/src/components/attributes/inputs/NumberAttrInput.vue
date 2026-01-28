<script setup lang="ts">
import { onMounted, ref } from 'vue'

const props = defineProps<{
  modelValue: any
  unit?: string
  autofocus?: boolean
}>()

const emit = defineEmits(['update:modelValue', 'save'])
const inputRef = ref<HTMLInputElement | null>(null)

onMounted(() => {
    if (props.autofocus) setTimeout(() => inputRef.value?.focus(), 50)
})
</script>

<template>
  <div class="relative w-full h-full bg-white dark:bg-gray-900 rounded-md border border-gray-300 dark:border-gray-700 shadow-sm focus-within:ring-2 focus-within:ring-blue-100 dark:focus-within:ring-blue-900 focus-within:border-blue-500 transition-all flex items-center overflow-hidden">
    <input 
        ref="inputRef"
        type="number" 
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        class="w-full h-9 px-3 text-sm outline-none bg-transparent font-mono placeholder:text-gray-400 text-gray-900 dark:text-gray-100" 
        step="any" 
        placeholder="0.00" 
        @keydown.enter.prevent="$emit('save')"
    />
    <span v-if="unit" class="absolute right-3 text-[10px] text-gray-500 dark:text-gray-400 font-bold pointer-events-none bg-gray-50 dark:bg-gray-800 px-1.5 py-0.5 rounded border border-gray-200 dark:border-gray-700">
        {{ unit }}
    </span>
  </div>
</template>