<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { cn } from '@/lib/utils'

const props = withDefaults(defineProps<{
  modelValue?: string
  label?: string
  placeholder?: string
  error?: string
  disabled?: boolean
  rows?: number
  class?: string
}>(), {
  modelValue: '',
  rows: 3
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus'])
const model = useVModel(props, 'modelValue', emit, { passive: true })
</script>

<template>
  <div class="space-y-1.5 w-full">
    <label v-if="label" class="text-sm font-medium leading-none text-foreground flex justify-between">
      {{ label }}
      <span v-if="error" class="text-destructive text-xs">{{ error }}</span>
    </label>
    
    <textarea
      v-model="model"
      :rows="rows"
      :disabled="disabled"
      :placeholder="placeholder"
      @blur="$emit('blur', $event)"
      @focus="$emit('focus', $event)"
      :class="cn(
        'flex min-h-[60px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 resize-y',
        error ? 'border-destructive focus-visible:ring-destructive' : '',
        $props.class
      )"
    ></textarea>
  </div>
</template>