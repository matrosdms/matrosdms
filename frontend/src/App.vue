<script setup lang="ts">
import DashboardView from '@/views/dashboard/DashboardView.vue'
import ActivityBar from '@/components/ActivityBar.vue'
import LoginView from '@/views/auth/LoginView.vue'
import SettingsView from '@/views/admin/SettingsView.vue'
import ProfileView from '@/views/user/ProfileView.vue'
import OfflineView from '@/views/system/OfflineView.vue'
import AboutView from '@/views/system/AboutView.vue'
import SearchView from '@/views/search/SearchView.vue'
import AiView from '@/views/ai/AiView.vue'
import WelcomeTour from '@/components/onboarding/WelcomeTour.vue'
import ToastProvider from '@/components/ui/ToastProvider.vue'
import ContextFilterBar from '@/components/widgets/ContextFilterBar.vue'
import GlobalSearch from '@/components/widgets/GlobalSearch.vue'
import UserMenu from '@/components/widgets/UserMenu.vue'
import { Loader2, Bell } from 'lucide-vue-next'
import { computed, ref, onMounted } from 'vue'
import { useIsFetching, useIsMutating } from '@tanstack/vue-query'
import { push } from 'notivue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { useActionStore } from '@/stores/action'
import { useDmsStore } from '@/stores/dms'
import { usePreferencesStore } from '@/stores/preferences'
import { useAppBoot } from '@/composables/useAppBoot'
import { useRouteSync } from '@/composables/useRouteSync'
import { InboxService } from '@/services/InboxService'

const auth = useAuthStore()
const ui = useUIStore()
const dms = useDmsStore()
const actionStore = useActionStore()
const prefs = usePreferencesStore()
const { isBackendDisconnected, bootSystem, onRetryConnection } = useAppBoot()

useRouteSync()

const dashboardRef = ref<InstanceType<typeof DashboardView> | null>(null)
const settingsRef = ref<InstanceType<typeof SettingsView> | null>(null)
const searchRef = ref<InstanceType<typeof SearchView> | null>(null)
const aiRef = ref<InstanceType<typeof AiView> | null>(null)

const isFetching = useIsFetching()
const isMutating = useIsMutating()
const isLoading = computed(() => isFetching.value > 0 || isMutating.value > 0)

bootSystem()

onMounted(() => {
  prefs.applyTheme()
})

const handleFoldSidebar = () => {
  if (ui.currentView === 'dms') dashboardRef.value?.toggleSidebar()
  else if (ui.currentView === 'settings') settingsRef.value?.toggleSidebar()
  else if (ui.currentView === 'search') searchRef.value?.toggleSidebar()
  else if (ui.currentView === 'ai') aiRef.value?.toggleSidebar()
}

const handleSaveLayout = () => {
  if (ui.currentView === 'dms' && dashboardRef.value) {
    dashboardRef.value.saveLayout()
  } else if (ui.currentView === 'settings' && settingsRef.value) {
    settingsRef.value.saveLayout()
  } else {
    push.info("Layout saving is available in Dashboard and Settings.")
  }
}

const handleResetLayout = () => {
  if (ui.currentView === 'dms' && dashboardRef.value) {
    dashboardRef.value.resetLayout()
  } else if (ui.currentView === 'settings' && settingsRef.value) {
    settingsRef.value.resetLayout()
  }
}

const handleViewSwitch = (view: any) => ui.setView(view)

const openTasks = () => {
  ui.setSidebarMode('actions')
  ui.setView('dms')
}

// --- GLOBAL DRAG & DROP LOGIC ---

const handleDragEnter = (event: DragEvent) => {
  // Check if dragging external files
  if (event.dataTransfer?.types.includes('Files')) {
    if (auth.isAuthenticated) {
        // Automatically Switch Sidebar to Inbox so user sees where to drop
        // We do NOT trigger the global overlay anymore.
        if (ui.sidebarMode !== 'inbox') {
            ui.setView('dms')
            ui.setSidebarMode('inbox')
        }
    }
  }
}

const handleGlobalDrop = async (event: DragEvent) => {
    // "Release anywhere" handler (Fallback if not dropped directly on Inbox list)
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
        event.preventDefault() // Stop browser from opening file
        
        const files = Array.from(event.dataTransfer.files)
        push.info(`Uploading ${files.length} file(s) to Inbox...`)
        
        for (const file of files) {
            try {
                await InboxService.upload(file)
            } catch (e: any) {
                push.error(`Failed to upload ${file.name}: ${e.message}`)
            }
        }
    }
}
</script>

<template>
  <div
    class="flex flex-col h-screen w-full overflow-hidden bg-[#f8f9fa] dark:bg-black text-foreground font-sans transition-colors duration-300"
    @dragover.prevent="handleDragEnter"
    @drop.prevent="handleGlobalDrop"
    @dragleave.prevent
  >
    <ToastProvider />
    <!-- Removed DropZoneOverlay -->

    <div v-if="isBackendDisconnected" class="absolute inset-0 z-[100]">
      <OfflineView :onRetry="onRetryConnection" />
    </div>

    <div v-else-if="!auth.isConfigLoaded" class="h-full w-full flex flex-col items-center justify-center bg-gray-50 dark:bg-gray-900 z-50">
      <div class="flex flex-col items-center animate-in fade-in zoom-in-95 duration-500">
        <div class="w-12 h-12 border-[5px] border-gray-200 border-t-primary rounded-full animate-spin mb-6"></div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100 tracking-tight">MatrosDMS</h1>
        <span class="text-sm text-gray-500 mt-2 font-medium animate-pulse">Booting System...</span>
      </div>
    </div>

    <LoginView v-else-if="!auth.isAuthenticated" />

    <template v-else>
      <WelcomeTour />
      
      <!-- Main Header -->
      <header class="flex-shrink-0 border-b border-border bg-white dark:bg-gray-900 dark:border-gray-700 flex items-center px-4 py-2 gap-4 shadow-sm z-20 relative min-h-[60px] transition-colors duration-300">
        
        <!-- Left: Brand + Context Filters -->
        <div class="flex items-center gap-4 shrink-0">
          <div class="font-bold tracking-tight text-xl text-gray-800 dark:text-white flex-shrink-0 flex items-center cursor-pointer group" @click="ui.setView('dms')">
            <span class="group-hover:text-primary transition-colors">MatrosDMS</span>
            <div class="ml-3 w-8 h-8 flex items-center justify-center">
              <div class="flex items-center justify-center rounded-full p-1 transition-colors duration-300" :class="isLoading ? 'bg-blue-50 text-primary' : 'bg-transparent text-gray-300 dark:text-gray-600'">
                  <Loader2 :size="18" :class="{ 'animate-spin': isLoading }" />
              </div>
            </div>
          </div>
          
          <div class="w-px h-8 bg-gray-200 dark:bg-gray-700 flex-shrink-0 hidden sm:block"></div>
          
          <div class="overflow-x-auto custom-scrollbar -ml-2 sm:ml-0">
             <ContextFilterBar />
          </div>
        </div>

        <!-- Center: Search (Flexible) -->
        <div class="flex-1 flex justify-center max-w-2xl mx-auto px-4">
           <GlobalSearch class="w-full" />
        </div>

        <!-- Right: User Menu -->
        <div class="flex items-center gap-4 justify-end flex-shrink-0 ml-auto">
          <div class="pl-2 border-l border-gray-200 dark:border-gray-700 flex-shrink-0 flex items-center gap-3">
            <button @click="openTasks" class="relative p-1.5 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-500 dark:text-gray-400 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-100" title="My Tasks">
                <Bell :size="18" />
                <span v-if="actionStore.myPendingCount > 0" class="absolute top-0 right-0 w-4 h-4 bg-red-500 text-white text-[9px] font-bold flex items-center justify-center rounded-full border border-white dark:border-gray-800 shadow-sm">
                    {{ actionStore.myPendingCount }}
                </span>
            </button>
            <UserMenu />
          </div>
        </div>
      </header>

      <div class="flex flex-1 overflow-hidden">
        <ActivityBar @fold-sidebar="handleFoldSidebar" @save-layout="handleSaveLayout" @reset-layout="handleResetLayout" @switch-view="handleViewSwitch" />
        <div class="flex-1 relative min-w-0">
          <DashboardView v-if="ui.currentView === 'dms'" ref="dashboardRef" />
          <SettingsView v-else-if="ui.currentView === 'settings'" ref="settingsRef" />
          <SearchView v-else-if="ui.currentView === 'search'" ref="searchRef" />
          <AiView v-else-if="ui.currentView === 'ai'" ref="aiRef" />
          <ProfileView v-else-if="ui.currentView === 'profile'" />
          <AboutView v-else-if="ui.currentView === 'about'" />
        </div>
      </div>
    </template>
  </div>
</template>