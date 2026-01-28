<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  modelValue: boolean
  title?: string
  activeClass?: string // Optional override for active colors
}>()

const emit = defineEmits(['update:modelValue'])

const classes = computed(() => cn(
  "p-1.5 rounded-md border transition-all duration-200 flex-shrink-0 shadow-sm flex items-center justify-center",
  props.modelValue 
    ? (props.activeClass || "bg-green-50 border-green-200 text-green-600") 
    : "bg-white border-gray-300 text-gray-400 hover:bg-gray-50 hover:text-gray-600"
))
</script>

<template>
  <button 
    type="button"
    :class="classes" 
    :title="title" 
    @click="$emit('update:modelValue', !modelValue)"
  >
    <slot />
  </button>
</template>