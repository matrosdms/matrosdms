<script setup lang="ts">
import { type Component } from 'vue'

defineProps<{
  icon?: Component
  title: string
  description?: string
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center h-full p-8 text-center select-none opacity-60">
    <div v-if="icon" class="mb-3 p-3 bg-gray-50 rounded-full border border-gray-100">
        <component 
        :is="icon" 
        :size="24" 
        class="text-gray-400" 
        />
    </div>
    <h3 class="text-xs font-bold text-gray-900 uppercase tracking-wide">{{ title }}</h3>
    <p v-if="description" class="mt-1 text-xs text-gray-500 max-w-[200px]">
      {{ description }}
    </p>
    <div v-if="$slots.default" class="mt-4">
      <slot />
    </div>
  </div>
</template>