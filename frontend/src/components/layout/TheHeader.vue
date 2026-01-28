<script setup>
import ContextFilterBar from '@/components/widgets/ContextFilterBar.vue'
import { User, Search, Loader2, WifiOff, LogOut } from 'lucide-vue-next'
import { computed } from 'vue'
import { store } from '@/store.js'
import { useIsFetching, useIsMutating } from '@tanstack/vue-query'
import { useMatrosData } from '@/composables/useMatrosData'

const isFetching = useIsFetching()
const isMutating = useIsMutating()
const { isBackendDisconnected } = useMatrosData()

const isLoading = computed(() => isFetching.value > 0 || isMutating.value > 0)
const handleLogout = () => store.logout()
</script>

<template>
  <header class="flex-shrink-0 border-b border-border bg-white flex flex-nowrap items-center justify-between px-6 py-2 gap-4 shadow-sm z-20 relative h-[80px]">
    
    <!-- LEFT SECTION: Brand + Context -->
    <div class="flex items-center gap-4 flex-1 min-w-0">
      <div class="font-bold tracking-tight text-xl text-gray-800 flex-shrink-0 flex items-center">
        MatrosDMS
        <!-- STATUS ICON -->
        <div class="ml-3 w-8 h-8 flex items-center justify-center">
          <div v-if="isBackendDisconnected" class="flex items-center justify-center bg-red-50 border border-red-200 rounded-full p-1.5 shadow-sm animate-pulse" title="Backend Unreachable">
            <WifiOff class="text-red-600" :size="18" />
          </div>
          <div v-else class="flex items-center justify-center rounded-full p-1 transition-colors duration-300" :class="isLoading ? 'bg-blue-50 text-blue-600' : 'bg-transparent text-gray-300'">
            <Loader2 :size="18" :class="{ 'animate-spin': isLoading }" />
          </div>
        </div>
      </div>

      <div class="w-px h-8 bg-gray-200 flex-shrink-0"></div>

      <!-- NEW COMPONENT: Context Filters -->
      <ContextFilterBar />
    </div>

    <!-- RIGHT SECTION: Global Search & User -->
    <div class="flex items-center gap-4 justify-end flex-shrink-0">
      <div class="relative w-[300px] xl:w-[400px] transition-all">
        <Search class="absolute left-3 top-2.5 text-gray-400" :size="16" />
        <input type="text" placeholder="Global Search..." class="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-full bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 focus:outline-none text-sm shadow-sm transition-all placeholder:text-gray-400" />
      </div>
      <div class="pl-2 border-l border-gray-200 flex-shrink-0 flex items-center gap-2">
        <div class="text-xs font-medium text-gray-700 hidden md:block">{{ store.currentUser?.firstname || store.currentUser?.name }}</div>
        <button @click="handleLogout" class="p-1.5 rounded-full hover:bg-red-50 hover:text-red-600 text-gray-500 transition-colors" title="Logout">
          <LogOut :size="18" />
        </button>
      </div>
    </div>
  </header>
</template>