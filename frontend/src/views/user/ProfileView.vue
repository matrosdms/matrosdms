<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { usePreferencesStore } from '@/stores/preferences'
import { ArrowLeft, User, Moon, Sun, Bell, Shield, LogOut } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue' // Fixed Import

const auth = useAuthStore()
const ui = useUIStore()
const prefs = usePreferencesStore()

const goBack = () => {
    ui.setView('dms')
}
</script>

<template>
  <div class="h-full w-full bg-gray-50 dark:bg-gray-900 flex flex-col overflow-hidden font-sans transition-colors duration-300">
    
    <!-- Header -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shrink-0 transition-colors duration-300">
        <div class="max-w-3xl mx-auto px-6 py-6">
            <div class="flex items-center gap-4">
                <button @click="goBack" class="p-2 rounded-full bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-gray-600 dark:text-gray-300">
                    <ArrowLeft :size="20" />
                </button>
                <div>
                    <h1 class="text-2xl font-extrabold text-gray-900 dark:text-white tracking-tight">My Profile</h1>
                    <p class="text-sm text-gray-500 dark:text-gray-400">Personal settings and preferences</p>
                </div>
            </div>
        </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto">
        <div class="max-w-3xl mx-auto w-full p-6 space-y-6">
            
            <!-- User Info Card -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm p-6 flex items-start gap-6 transition-colors duration-300">
                <div class="w-16 h-16 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0 text-xl font-bold border border-blue-200 dark:border-blue-800">
                    <User :size="32" />
                </div>
                <div class="flex-1">
                    <h2 class="text-lg font-bold text-gray-800 dark:text-white">{{ auth.currentUser?.firstname || 'User' }}</h2>
                    <div class="text-sm text-gray-500 dark:text-gray-400 font-mono mb-2">@{{ auth.currentUser?.name }}</div>
                    
                    <div class="flex gap-2 mb-4">
                        <span class="px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 text-xs font-bold border border-gray-200 dark:border-gray-600">
                            {{ auth.currentUser?.email || 'No Email' }}
                        </span>
                        <span class="px-2 py-0.5 rounded bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300 text-xs font-bold border border-purple-100 dark:border-purple-800 flex items-center gap-1">
                            <Shield :size="10" /> {{ auth.currentUser?.role }}
                        </span>
                    </div>
                </div>
                <!-- Updated Button -->
                <BaseButton variant="destructive" @click="auth.logout">
                    <LogOut :size="14" class="mr-2" /> Sign Out
                </BaseButton>
            </div>

            <!-- Appearance Settings -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden transition-colors duration-300">
                <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50">
                    <h3 class="font-bold text-gray-700 dark:text-gray-200 text-sm uppercase tracking-wide">Appearance & UI</h3>
                </div>
                
                <div class="p-6 space-y-6">
                    <!-- Dark Mode Toggle -->
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                            <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 transition-colors">
                                <Moon v-if="prefs.isDarkMode" :size="20" />
                                <Sun v-else :size="20" />
                            </div>
                            <div>
                                <div class="font-bold text-gray-800 dark:text-gray-100 text-sm">Dark Mode</div>
                                <div class="text-xs text-gray-500 dark:text-gray-400">Switch between light and dark themes.</div>
                            </div>
                        </div>
                        <button 
                            @click="prefs.toggleDarkMode"
                            class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
                            :class="prefs.isDarkMode ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'"
                        >
                            <span 
                                class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
                                :class="prefs.isDarkMode ? 'translate-x-6' : 'translate-x-1'"
                            />
                        </button>
                    </div>

                    <!-- Compact Sidebar -->
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                            <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">
                                <Bell :size="20" />
                            </div>
                            <div>
                                <div class="font-bold text-gray-800 dark:text-gray-100 text-sm">Show Notifications</div>
                                <div class="text-xs text-gray-500 dark:text-gray-400">Enable UI toast popups.</div>
                            </div>
                        </div>
                        <button 
                            @click="prefs.showNotifications = !prefs.showNotifications"
                            class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
                            :class="prefs.showNotifications ? 'bg-green-500' : 'bg-gray-200 dark:bg-gray-700'"
                        >
                            <span 
                                class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
                                :class="prefs.showNotifications ? 'translate-x-6' : 'translate-x-1'"
                            />
                        </button>
                    </div>
                </div>
            </div>

            <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 text-xs text-blue-800 dark:text-blue-300">
                <strong>Note:</strong> These preferences are currently stored in your browser (LocalStorage).
            </div>

        </div>
    </div>
  </div>
</template>