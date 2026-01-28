<script setup lang="ts">
import { EJobStatusList } from '@/enums'

defineProps<{
  modelValue: string
}>()

defineEmits(['update:modelValue'])
</script>

<template>
  <div class="mr-2">
    <select 
        :value="modelValue" 
        @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
        class="text-xs border border-gray-300 dark:border-gray-700 rounded px-1.5 py-1 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors cursor-pointer outline-none focus:ring-1 focus:ring-blue-500"
    >
        <option value="">ALL STATUS</option>
        <option v-for="status in EJobStatusList" :key="status" :value="status">{{ status }}</option>
    </select>
  </div>
</template>