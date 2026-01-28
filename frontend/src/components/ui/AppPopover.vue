<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { onClickOutside } from '@vueuse/core'

const props = defineProps<{
  align?: 'left' | 'right'
  width?: string
}>()

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)

const toggle = () => {
    isOpen.value = !isOpen.value
}

const close = () => {
    isOpen.value = false
}

const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
        close()
    }
}

onClickOutside(containerRef, () => close())
</script>

<template>
  <div 
    ref="containerRef" 
    class="relative inline-block text-left" 
    @keydown="onKeyDown"
  >
    <!-- Trigger -->
    <div @click="toggle">
      <slot name="trigger" :is-open="isOpen"></slot>
    </div>

    <!-- Dropdown Panel -->
    <Transition
      enter-active-class="transition ease-out duration-100"
      enter-from-class="transform opacity-0 scale-95"
      enter-to-class="transform opacity-100 scale-100"
      leave-active-class="transition ease-in duration-75"
      leave-from-class="transform opacity-100 scale-100"
      leave-to-class="transform opacity-0 scale-95"
    >
      <div 
        v-if="isOpen" 
        class="absolute top-full mt-2 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden origin-top-right focus:outline-none"
        :class="[
            align === 'right' ? 'right-0' : 'left-0',
            width || 'w-56'
        ]"
      >
        <slot name="content" :close="close"></slot>
      </div>
    </Transition>
  </div>
</template>