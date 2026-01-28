<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'
import { Sparkles } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue?: string | number | boolean
  label?: string
  options?: any[] 
  placeholder?: string
  disabled?: boolean
  error?: string
  class?: string
  valueKey?: string
  labelKey?: string
  suggestion?: boolean // NEW: AI Highlight
}>(), {
  modelValue: '',
  valueKey: 'value',
  labelKey: 'label'
})

const emit = defineEmits(['update:modelValue', 'change'])
const model = useVModel(props, 'modelValue', emit, { passive: true })
</script>

<template>
  <div class="space-y-1.5 w-full">
    <label v-if="label" class="text-sm font-medium leading-none text-foreground flex justify-between">
      <span class="flex items-center gap-1.5">
        {{ label }}
        <Sparkles v-if="suggestion" class="text-purple-500 animate-pulse" :size="10" />
      </span>
      <span v-if="error" class="text-destructive text-xs">{{ error }}</span>
    </label>
    
    <div class="relative">
        <select
          v-model="model"
          :disabled="disabled"
          @change="$emit('change', $event)"
          :class="cn(
            'flex h-9 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring disabled:cursor-not-allowed disabled:opacity-50 appearance-none transition-all',
            error ? 'border-destructive focus:ring-destructive' : '',
            suggestion ? 'border-purple-300 bg-purple-50/50 dark:border-purple-800 dark:bg-purple-900/10 focus:ring-purple-500 text-purple-900 dark:text-purple-100 font-medium' : '',
            $props.class
          )"
        >
          <option v-if="placeholder" value="" disabled selected>{{ placeholder }}</option>
          
          <template v-if="options">
              <option 
                v-for="(opt, idx) in options" 
                :key="idx" 
                :value="typeof opt === 'object' ? opt[valueKey] : opt"
              >
                {{ typeof opt === 'object' ? opt[labelKey] : opt }}
              </option>
          </template>
          <slot v-else />
        </select>
        <!-- Chevron Icon -->
        <svg class="absolute right-3 top-3 h-4 w-4 opacity-50 pointer-events-none" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
    </div>
  </div>
</template>