<script setup lang="ts">
import { useDmsStore } from '@/stores/dms'
import { UploadCloud } from 'lucide-vue-next'

const dms = useDmsStore()
</script>

<template>
  <Transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0 scale-95"
    enter-to-class="opacity-100 scale-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100 scale-100"
    leave-to-class="opacity-0 scale-95"
  >
    <div 
        v-if="dms.isDraggingGlobal" 
        class="fixed inset-0 z-[9999] pointer-events-none flex items-center justify-center bg-blue-50/80 dark:bg-blue-900/80 backdrop-blur-sm"
    >
      <div class="border-4 border-dashed border-blue-500 rounded-xl p-12 flex flex-col items-center animate-pulse">
        <div class="bg-white dark:bg-gray-800 p-6 rounded-full shadow-lg mb-4">
            <UploadCloud :size="64" class="text-blue-600 dark:text-blue-400" />
        </div>
        <h2 class="text-3xl font-bold text-blue-800 dark:text-blue-200 uppercase tracking-widest">
            Drop to Upload
        </h2>
        <p class="text-blue-600 dark:text-blue-300 mt-2 font-medium">
            Release anywhere to add to Inbox
        </p>
      </div>
    </div>
  </Transition>
</template>