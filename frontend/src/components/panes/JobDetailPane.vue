<script setup lang="ts">
import { computed } from 'vue'
import { Terminal, CheckCircle2, XCircle, Clock, Play, Timer, CalendarClock, AlertOctagon } from 'lucide-vue-next'
import { parseBackendDate } from '@/lib/utils'
import { EJobStatus } from '@/enums'
import type { components } from '@/types/schema'

const props = defineProps<{
  // Allow UI appended logs while preserving strict core types
  item: components['schemas']['JobMessage'] & { logs?: any[] }
}>()

const statusColor = computed(() => {
    switch(props.item?.status) {
        case EJobStatus.COMPLETED: return 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/30 border-green-200 dark:border-green-800'
        case EJobStatus.FAILED: return 'text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/30 border-red-200 dark:border-red-800'
        case EJobStatus.RUNNING: return 'text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800'
        default: return 'text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border-gray-200 dark:border-gray-700'
    }
})

const logs = computed(() => props.item?.logs ||[])
const startDate = computed(() => parseBackendDate(props.item?.executionTime)?.toLocaleString() || '-')
</script>

<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-900 animate-in fade-in transition-colors">
    <!-- Header Info -->
    <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-900">
        <div class="flex justify-between items-start">
            <div>
                <div class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">System Job</div>
                <h2 class="text-lg font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
                    {{ item.taskName }}
                    <span class="px-2 py-0.5 rounded-full text-xs font-bold border flex items-center gap-1" :class="statusColor">
                        <component :is="item.status === EJobStatus.COMPLETED ? CheckCircle2 : item.status === EJobStatus.FAILED ? XCircle : Play" :size="12" />
                        {{ item.status }}
                    </span>
                </h2>
                <div class="text-[11px] text-gray-500 dark:text-gray-400 mt-1 font-mono flex items-center gap-4">
                    <span class="flex items-center gap-1"><Clock :size="12"/> Execution Time: {{ startDate }}</span>
                </div>
            </div>
            <div class="text-right text-xs text-gray-400 font-mono">ID: {{ item.uuid }}</div>
        </div>

        <div class="mt-4 grid grid-cols-2 gap-4 bg-white dark:bg-gray-800 p-3 rounded-md border border-gray-200 dark:border-gray-700 shadow-sm">
            <div class="flex items-center gap-3">
                <div class="p-2 bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 rounded-full"><Timer :size="18"/></div>
                <div>
                    <div class="text-[10px] text-gray-500 dark:text-gray-400 uppercase font-bold">Execution Time</div>
                    <div class="text-sm font-mono dark:text-gray-200">{{ startDate }}</div>
                </div>
            </div>
            
            <div class="flex items-center gap-3">
                <div class="p-2 bg-purple-50 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 rounded-full"><CalendarClock :size="18"/></div>
                <div>
                    <div class="text-[10px] text-gray-500 dark:text-gray-400 uppercase font-bold">Task Type</div>
                    <div class="text-sm font-medium dark:text-gray-200">{{ item.taskName }}</div>
                </div>
            </div>
        </div>
        
        <div v-if="item.status === EJobStatus.FAILED" class="mt-2 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-900 rounded p-2 flex gap-2 text-xs text-red-700 dark:text-red-400">
            <AlertOctagon :size="16" />
            <div>
                <strong>Execution Failed.</strong> Check the logs below for stack trace.
            </div>
        </div>
    </div>

    <!-- Logs Console -->
    <div class="flex-1 p-4 bg-gray-50 dark:bg-[#1e1e1e] overflow-auto font-mono text-xs transition-colors border-t border-gray-200 dark:border-gray-800">
        <div v-if="logs.length === 0" class="text-gray-400 italic select-none">No logs available.</div>
        <div v-for="(log, i) in logs" :key="i" class="mb-1 border-b border-gray-200 dark:border-gray-800/50 pb-0.5 last:border-0 flex gap-3">
            <span class="text-gray-400 dark:text-gray-500 shrink-0 select-none">[{{ parseBackendDate(log.timestamp)?.toLocaleTimeString() || '--:--:--' }}]</span>
            <span :class="{
                'text-red-600 dark:text-red-400 font-bold': log.severity === 'ERROR',
                'text-yellow-600 dark:text-yellow-400': log.severity === 'WARN',
                'text-green-600 dark:text-green-400': log.severity === 'INFO',
                'text-gray-600 dark:text-gray-400': log.severity === 'DEBUG'
            }">{{ log.message }}</span>
        </div>
    </div>
  </div>
</template>