<script setup lang="ts">
import { X } from 'lucide-vue-next'

defineProps<{
  isOpen: boolean
  title?: string
}>()

defineEmits(['close', 'submit'])

defineSlots<{
  default(props: {}): any
  footer(props: {}): any
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center font-sans">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/20 backdrop-blur-[1px] transition-opacity" @click="$emit('close')"></div>
      
      <!-- Content -->
      <div class="relative bg-white rounded-xl shadow-2xl w-full max-w-md border border-gray-100 transform transition-all p-0 overflow-hidden scale-100">
        
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 bg-gray-50/50">
          <h3 class="text-base font-semibold text-gray-800 tracking-tight">{{ title }}</h3>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-md hover:bg-gray-100">
            <X :size="18" />
          </button>
        </div>

        <!-- Body -->
        <div class="p-6">
          <slot></slot>
        </div>

        <!-- Footer -->
        <div class="flex justify-end gap-3 px-6 py-4 bg-gray-50 border-t border-gray-100">
          <slot name="footer">
            <button @click="$emit('close')" class="px-4 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 hover:text-gray-900 transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-gray-200">
                Cancel
            </button>
            <button @click="$emit('submit')" class="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-lg hover:bg-blue-700 shadow-md hover:shadow-lg transition-all focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1">
                Save Changes
            </button>
          </slot>
        </div>
      </div>
    </div>
  </Teleport>
</template>