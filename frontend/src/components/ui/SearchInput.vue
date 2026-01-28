<script setup>
import { Search } from 'lucide-vue-next'

defineProps({
  modelValue: String,
  placeholder: { type: String, default: 'Search...' }
})

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="relative w-full">
    <Search class="absolute left-2.5 top-1.5 text-gray-400 dark:text-gray-500 pointer-events-none transition-colors" :size="14" />
    <input 
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      type="text" 
      :placeholder="placeholder" 
      class="w-full pl-8 pr-3 py-1 text-[12px] border border-gray-300 dark:border-gray-700 rounded-full bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 placeholder:text-gray-400 dark:placeholder:text-gray-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 dark:focus:border-blue-400 dark:focus:ring-blue-400 transition-all shadow-sm" 
    />
  </div>
</template>