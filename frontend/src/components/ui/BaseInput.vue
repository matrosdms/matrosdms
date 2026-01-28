<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'
import { ref, onMounted, nextTick } from 'vue'
import { Sparkles } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  modelValue?: string | number
  defaultValue?: string | number
  label?: string
  type?: string
  placeholder?: string
  error?: boolean | string
  disabled?: boolean
  autofocus?: boolean
  class?: string
  description?: string
  suggestion?: boolean // NEW: Indicates AI-filled content
}>(), {
  modelValue: '',
  type: 'text'
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'enter'])
const model = useVModel(props, 'modelValue', emit, { passive: true })
const inputRef = ref<HTMLInputElement | null>(null)

onMounted(() => {
  if (props.autofocus) nextTick(() => inputRef.value?.focus())
})
</script>

<template>
  <div class="space-y-1.5 w-full">
    <label 
      v-if="label" 
      class="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 text-gray-700 dark:text-gray-300 flex justify-between"
    >
      <span class="flex items-center gap-1.5">
        {{ label }}
        <Sparkles v-if="suggestion" class="text-purple-500 animate-pulse" :size="10" />
      </span>
      <span v-if="error && typeof error === 'string'" class="text-destructive text-xs">{{ error }}</span>
    </label>
    
    <div class="relative">
        <input
          ref="inputRef"
          v-model="model"
          :type="type"
          :disabled="disabled"
          :placeholder="placeholder"
          @blur="$emit('blur', $event)"
          @focus="$emit('focus', $event)"
          @keydown.enter="$emit('enter', $event)"
          :class="cn(
            'flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm transition-all file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-50',
            error ? 'border-destructive focus-visible:ring-destructive' : '',
            suggestion ? 'border-purple-300 bg-purple-50/50 dark:border-purple-800 dark:bg-purple-900/10 focus-visible:ring-purple-500' : '',
            $props.class
          )"
        />
        <!-- AI Indicator Icon inside input -->
        <Sparkles 
            v-if="suggestion && !error" 
            class="absolute right-3 top-2.5 text-purple-400 pointer-events-none opacity-60" 
            :size="14" 
        />
    </div>
    
    <p v-if="description && !error" class="text-[10px] text-muted-foreground">
      {{ description }}
    </p>
  </div>
</template>