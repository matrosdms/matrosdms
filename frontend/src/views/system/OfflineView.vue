<script setup lang="ts">
import { WifiOff, RefreshCw } from 'lucide-vue-next'
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  onRetry: () => Promise<void>
}>()

const isRetrying = ref(false)
let timer: any = null

const handleRetry = async () => {
  if (isRetrying.value) return
  isRetrying.value = true
  try {
    await props.onRetry()
  } finally {
    // Small delay to let the animation finish gracefully
    setTimeout(() => { isRetrying.value = false }, 800)
  }
}

// Auto-check loop every 10 seconds
onMounted(() => {
    timer = setInterval(() => {
        handleRetry()
    }, 10000)
})

onUnmounted(() => {
    if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="h-screen w-full flex items-center justify-center bg-gray-100/80 dark:bg-black/90 text-gray-600 dark:text-gray-300 p-4 backdrop-blur-sm z-[9999]">
    <div class="bg-white dark:bg-gray-900 p-8 rounded-xl shadow-2xl border border-gray-200 dark:border-gray-800 max-w-md w-full text-center flex flex-col items-center animate-in fade-in zoom-in-95 duration-300 relative overflow-hidden">
       
       <!-- Heartbeat Animation Container -->
       <div class="relative w-24 h-24 flex items-center justify-center mb-6">
          <!-- The Pulse Rings -->
          <div class="absolute inset-0 bg-red-100 dark:bg-red-900/20 rounded-full animate-radar-ping opacity-75"></div>
          <div class="absolute inset-0 bg-red-100 dark:bg-red-900/20 rounded-full animate-radar-ping delay-700 opacity-75"></div>
          
          <!-- Static Icon Circle -->
          <div class="w-20 h-20 bg-red-50 dark:bg-red-900/30 rounded-full flex items-center justify-center shadow-sm border border-red-100 dark:border-red-900/50 relative z-10">
             <WifiOff class="text-red-500 dark:text-red-400" :size="40" />
          </div>
       </div>
       
       <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100 mb-2">Connection Lost</h1>
       <p class="text-sm text-gray-500 dark:text-gray-400 mb-8 leading-relaxed">
         We cannot reach the MatrosDMS server.<br>
         <span class="text-xs text-gray-400 dark:text-gray-600">Next auto-check in a few seconds...</span>
       </p>
       
       <button 
         @click="handleRetry" 
         :disabled="isRetrying"
         class="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-80 disabled:cursor-wait active:scale-[0.98]"
       >
         <RefreshCw :size="20" :class="{ 'animate-spin': isRetrying }" />
         <span>{{ isRetrying ? 'Checking Server...' : 'Check Now' }}</span>
       </button>
    </div>
  </div>
</template>

<style scoped>
@keyframes radar-ping {
  0% {
    transform: scale(0.8);
    opacity: 0.8;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.animate-radar-ping {
  animation: radar-ping 2.5s cubic-bezier(0, 0, 0.2, 1) infinite;
}

.delay-700 {
  animation-delay: 1s;
}
</style>