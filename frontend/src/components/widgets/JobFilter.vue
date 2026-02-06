<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { EJobStatusList } from '@/enums'

const props = defineProps<{
  modelValue: any
}>()

const emit = defineEmits(['update:modelValue'])

const status = ref('')
const range = ref('7d')

// Sync internal state if parent changes (e.g. reset)
watch(() => props.modelValue, (val) => {
    if (!val || typeof val === 'string') {
        status.value = ''
        // Keep range if set, or default
    } else {
        status.value = val.status || ''
        range.value = val.range || '7d'
    }
}, { immediate: true })

const emitChange = () => {
    emit('update:modelValue', { 
        status: status.value, 
        range: range.value 
    })
}

const onStatusChange = (e: Event) => {
    status.value = (e.target as HTMLSelectElement).value
    emitChange()
}

const onRangeChange = (e: Event) => {
    range.value = (e.target as HTMLSelectElement).value
    emitChange()
}

// Initial emit to ensure query runs with defaults
onMounted(() => {
    emitChange()
})
</script>

<template>
  <div class="flex items-center gap-2 mr-2">
    <select 
        :value="status" 
        @change="onStatusChange"
        class="text-xs border border-gray-300 dark:border-gray-700 rounded px-1.5 py-1 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors cursor-pointer outline-none focus:ring-1 focus:ring-blue-500"
    >
        <option value="">ALL STATUS</option>
        <option v-for="s in EJobStatusList" :key="s" :value="s">{{ s }}</option>
    </select>

    <select 
        :value="range" 
        @change="onRangeChange"
        class="text-xs border border-gray-300 dark:border-gray-700 rounded px-1.5 py-1 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors cursor-pointer outline-none focus:ring-1 focus:ring-blue-500"
    >
        <option value="7d">Last 7 Days</option>
        <option value="30d">Last 30 Days</option>
        <option value="all">All Time</option>
    </select>
  </div>
</template>