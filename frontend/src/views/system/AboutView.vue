<script setup lang="ts">
import { ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { SystemService } from '@/services/SystemService'
import { useUIStore } from '@/stores/ui'
import { ArrowLeft, Server, Cpu, Database, Layers, ShieldCheck, Activity, Terminal, Trash2, AlertCircle, AlertTriangle, CheckCircle, Info, Bug } from 'lucide-vue-next'
import LegalText from '@/components/content/LegalText.vue'
import { parseBackendDate } from '@/lib/utils'

const ui = useUIStore()
const activeTab = ref<'system' | 'legal' | 'console'>('system')

// Console Filters
const filters = ref({ info: true, success: true, error: true, debug: true, warning: true })
const filteredLogs = computed(() => ui.logs.filter(log => filters.value[log.type]))
const clearLogs = () => ui.logs = []

const { data: versionInfo, isLoading } = useQuery({
  queryKey: ['system-version'],
  queryFn: SystemService.getVersion
})

const goBack = () => {
    ui.setView('dms')
}
</script>

<template>
  <div class="h-full w-full bg-gray-50 dark:bg-black flex flex-col overflow-hidden font-sans transition-colors duration-300">
    
    <!-- Hero / Header -->
    <div class="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 shrink-0 transition-colors">
        <div class="max-w-6xl mx-auto px-8 pt-8 pb-0">
            <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-4">
                    <button @click="goBack" class="p-2 rounded-full bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors text-gray-600 dark:text-gray-300">
                        <ArrowLeft :size="20" />
                    </button>
                    <div>
                        <h1 class="text-2xl font-extrabold text-gray-900 dark:text-white tracking-tight">System Information</h1>
                        <p class="text-sm text-gray-500 dark:text-gray-400">MatrosDMS Enterprise Edition</p>
                    </div>
                </div>
            </div>
            
            <!-- Tabs -->
            <div class="flex gap-6">
                <button 
                    @click="activeTab = 'system'" 
                    class="pb-3 text-sm font-bold border-b-2 transition-colors flex items-center gap-2"
                    :class="activeTab === 'system' ? 'border-blue-600 text-blue-600 dark:text-blue-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'"
                >
                    <Activity :size="16" /> Overview
                </button>
                <button 
                    @click="activeTab = 'legal'" 
                    class="pb-3 text-sm font-bold border-b-2 transition-colors flex items-center gap-2"
                    :class="activeTab === 'legal' ? 'border-blue-600 text-blue-600 dark:text-blue-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'"
                >
                    <ShieldCheck :size="16" /> Legal & Compliance
                </button>
                <button 
                    @click="activeTab = 'console'" 
                    class="pb-3 text-sm font-bold border-b-2 transition-colors flex items-center gap-2"
                    :class="activeTab === 'console' ? 'border-blue-600 text-blue-600 dark:text-blue-400' : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'"
                >
                    <Terminal :size="16" /> System Console
                </button>
            </div>
        </div>
    </div>

    <!-- Scrollable Content -->
    <div class="flex-1 overflow-y-auto custom-scrollbar">
        <div class="max-w-6xl mx-auto w-full p-8 h-full">
            
            <!-- TAB: SYSTEM OVERVIEW -->
            <div v-if="activeTab === 'system'" class="animate-in fade-in slide-in-from-bottom-2 duration-300">
                <div class="grid md:grid-cols-2 gap-6 mb-12">
                    
                    <!-- Backend Info -->
                    <div class="bg-white dark:bg-gray-900 p-6 rounded-xl border border-gray-200 dark:border-gray-800 shadow-sm relative overflow-hidden group">
                        <h3 class="text-xs font-bold text-blue-600 dark:text-blue-400 uppercase tracking-wider mb-4 flex items-center gap-2">
                            <Server :size="16" /> Core System
                        </h3>
                        <div v-if="isLoading" class="animate-pulse space-y-3">
                            <div class="h-4 bg-gray-100 dark:bg-gray-800 rounded w-3/4"></div>
                            <div class="h-4 bg-gray-100 dark:bg-gray-800 rounded w-1/2"></div>
                        </div>
                        <div v-else class="space-y-4 relative z-10">
                            <div class="flex justify-between border-b border-gray-50 dark:border-gray-800 pb-2">
                                <span class="text-gray-500 dark:text-gray-400 text-sm">Version</span>
                                <span class="font-mono font-medium text-gray-800 dark:text-gray-200">{{ versionInfo?.version || 'Unknown' }}</span>
                            </div>
                             <div class="flex justify-between border-b border-gray-50 dark:border-gray-800 pb-2">
                                <span class="text-gray-500 dark:text-gray-400 text-sm">Environment</span>
                                <span class="font-mono font-medium text-gray-800 dark:text-gray-200">{{ versionInfo?.environment || 'Production' }}</span>
                            </div>
                            <div class="flex justify-between">
                                <span class="text-gray-500 dark:text-gray-400 text-sm">Status</span>
                                <span class="text-green-600 dark:text-green-400 font-bold flex items-center gap-1.5 text-sm">
                                    <span class="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span> Operational
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- Client Info -->
                    <div class="bg-white dark:bg-gray-900 p-6 rounded-xl border border-gray-200 dark:border-gray-800 shadow-sm relative overflow-hidden group">
                        <h3 class="text-xs font-bold text-purple-600 dark:text-purple-400 uppercase tracking-wider mb-4 flex items-center gap-2">
                            <Cpu :size="16" /> Client Interface
                        </h3>
                         <div class="space-y-4 relative z-10">
                            <div class="flex justify-between border-b border-gray-50 dark:border-gray-800 pb-2">
                                <span class="text-gray-500 dark:text-gray-400 text-sm">Version</span>
                                <span class="font-mono font-medium text-gray-800 dark:text-gray-200">2.0.0 (RC)</span>
                            </div>
                            <div class="flex justify-between border-b border-gray-50 dark:border-gray-800 pb-2">
                                <span class="text-gray-500 dark:text-gray-400 text-sm">Framework</span>
                                <span class="font-medium text-gray-800 dark:text-gray-200">Vue 3 + TypeScript</span>
                            </div>
                             <div class="flex justify-between">
                                <span class="text-gray-500 dark:text-gray-400 text-sm">Build Mode</span>
                                <span class="font-mono text-xs text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 px-2 py-0.5 rounded">VITE_PROD</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Tech Stack Grid -->
                <h3 class="text-sm font-bold text-gray-800 dark:text-gray-200 mb-4 flex items-center gap-2 uppercase tracking-wide">
                    <Layers :size="16" class="text-gray-400" /> Technology Stack
                </h3>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div class="bg-white dark:bg-gray-900 p-4 rounded-lg border border-gray-200 dark:border-gray-800 flex items-center gap-3">
                        <div class="p-2 bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 rounded-lg"><div class="font-bold">Vue</div></div>
                        <div class="text-sm font-medium text-gray-700 dark:text-gray-300">Frontend Core</div>
                    </div>
                    <div class="bg-white dark:bg-gray-900 p-4 rounded-lg border border-gray-200 dark:border-gray-800 flex items-center gap-3">
                        <div class="p-2 bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 rounded-lg"><div class="font-bold">TS</div></div>
                        <div class="text-sm font-medium text-gray-700 dark:text-gray-300">TypeScript</div>
                    </div>
                    <div class="bg-white dark:bg-gray-900 p-4 rounded-lg border border-gray-200 dark:border-gray-800 flex items-center gap-3">
                        <div class="p-2 bg-orange-50 dark:bg-orange-900/20 text-orange-600 dark:text-orange-400 rounded-lg"><div class="font-bold">Java</div></div>
                        <div class="text-sm font-medium text-gray-700 dark:text-gray-300">Spring Boot</div>
                    </div>
                    <div class="bg-white dark:bg-gray-900 p-4 rounded-lg border border-gray-200 dark:border-gray-800 flex items-center gap-3">
                        <div class="p-2 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded-lg"><Database :size="16"/></div>
                        <div class="text-sm font-medium text-gray-700 dark:text-gray-300">PostgreSQL</div>
                    </div>
                </div>
            </div>

            <!-- TAB: LEGAL -->
            <div v-if="activeTab === 'legal'" class="animate-in fade-in slide-in-from-bottom-2 duration-300">
                <div class="bg-white dark:bg-gray-900 p-8 rounded-xl border border-gray-200 dark:border-gray-800 shadow-sm max-w-3xl">
                    <LegalText class="text-gray-600 dark:text-gray-400" />
                </div>
            </div>

            <!-- TAB: CONSOLE -->
            <div v-if="activeTab === 'console'" class="animate-in fade-in slide-in-from-bottom-2 duration-300 h-full flex flex-col bg-[#1e1e1e] rounded-xl overflow-hidden border border-gray-800">
                <div class="flex items-center gap-2 px-3 py-2 border-b border-[#333] bg-[#252526] select-none shrink-0">
                    <button @click="filters.error = !filters.error" class="log-filter-btn text-red-400 border-red-900/30" :class="filters.error ? 'bg-red-400/10 border-red-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                        <AlertCircle :size="12" /> Errors
                    </button>
                    <button @click="filters.warning = !filters.warning" class="log-filter-btn text-yellow-400 border-yellow-900/30" :class="filters.warning ? 'bg-yellow-400/10 border-yellow-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                        <AlertTriangle :size="12" /> Warnings
                    </button>
                    <button @click="filters.success = !filters.success" class="log-filter-btn text-green-400 border-green-900/30" :class="filters.success ? 'bg-green-400/10 border-green-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                        <CheckCircle :size="12" /> Success
                    </button>
                    <button @click="filters.info = !filters.info" class="log-filter-btn text-blue-400 border-blue-900/30" :class="filters.info ? 'bg-blue-400/10 border-blue-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                        <Info :size="12" /> Info
                    </button>
                    <button @click="filters.debug = !filters.debug" class="log-filter-btn text-gray-400 border-gray-700" :class="filters.debug ? 'bg-gray-400/10 border-gray-500/50' : 'opacity-50 grayscale hover:opacity-80'">
                        <Bug :size="12" /> Debug
                    </button>
                    
                    <div class="flex-1"></div>
                    <button @click="clearLogs" class="flex items-center gap-1.5 px-3 py-1 rounded border border-red-900 text-red-400 hover:text-white hover:bg-red-900/50 transition-colors font-bold text-xs" title="Clear Console">
                        <Trash2 :size="12" /> Clear
                    </button>
                </div>

                <div class="flex-1 overflow-auto p-4 font-mono text-xs text-gray-300">
                    <div v-for="log in filteredLogs" :key="log.id" class="flex gap-2 mb-1 hover:bg-white/5 px-2 py-1 rounded leading-snug group border-l-2 border-transparent"
                        :class="{
                            'border-red-500': log.type === 'error',
                            'border-yellow-500': log.type === 'warning',
                            'border-green-500': log.type === 'success'
                        }"
                    >
                        <span class="text-gray-500 shrink-0 select-none w-[65px] opacity-70 group-hover:opacity-100">{{ log.time }}</span>
                        <span class="font-bold shrink-0 w-[60px] uppercase tracking-wider select-none" 
                              :class="{
                                'text-red-500': log.type === 'error',
                                'text-yellow-500': log.type === 'warning',
                                'text-green-500': log.type === 'success',
                                'text-blue-500': log.type === 'info',
                                'text-gray-600': log.type === 'debug'
                              }">
                            {{ log.type }}
                        </span>
                        <span class="break-all whitespace-pre-wrap flex-1"
                              :class="{
                                'text-red-300': log.type === 'error',
                                'text-yellow-300': log.type === 'warning',
                                'text-green-300': log.type === 'success',
                                'text-gray-300': log.type === 'info',
                                'text-gray-500': log.type === 'debug'
                              }">{{ log.message }}</span>
                    </div>
                    
                    <div v-if="filteredLogs.length === 0" class="h-full flex flex-col items-center justify-center text-gray-600 italic">
                        <span v-if="ui.logs.length > 0">No logs match current filters</span>
                        <span v-else>Console is empty</span>
                    </div>
                </div>
            </div>

        </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #e5e7eb; border-radius: 4px; }
.dark .custom-scrollbar::-webkit-scrollbar-thumb { background: #4b5563; }
.log-filter-btn { @apply flex items-center gap-1.5 px-2 py-1 rounded border text-[11px] font-medium transition-all; }
</style>