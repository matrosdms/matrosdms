<script setup lang="ts">
import { ref } from 'vue'
import { Play, ChevronDown, ShieldCheck, Search as SearchIcon, Archive } from 'lucide-vue-next'
import { AdminService } from '@/services/AdminService'
import { push } from 'notivue'
import { EJobType, EJobTypeLabels } from '@/enums'
import BaseButton from '@/components/ui/BaseButton.vue'

const emit = defineEmits(['job-started'])

const isOpen = ref(false)
const isBusy = ref(false)

const JOBS = [
  { id: EJobType.INTEGRITY_CHECK, label: EJobTypeLabels[EJobType.INTEGRITY_CHECK], icon: ShieldCheck, desc: 'Verify file hashes and database consistency' },
  { id: EJobType.REINDEX_SEARCH, label: EJobTypeLabels[EJobType.REINDEX_SEARCH], icon: SearchIcon, desc: 'Rebuild Lucene/Elastic indexes' },
  { id: EJobType.EXPORT_ARCHIVE, label: EJobTypeLabels[EJobType.EXPORT_ARCHIVE], icon: Archive, desc: 'Create a ZIP dump of all active documents' }
]

const runJob = async (type: any) => {
  isOpen.value = false
  isBusy.value = true
  try {
    const response = await AdminService.startJob(type)
    // Backend returns a simple string message, not JSON
    push.success(response || `Job '${type}' queued successfully`)
    emit('job-started')
  } catch (e: any) {
    push.error(`Failed to start job: ${e.message}`)
  } finally {
    isBusy.value = false
  }
}
</script>

<template>
  <div class="relative">
    <BaseButton 
      variant="default" 
      size="sm"
      @click="isOpen = !isOpen"
      :disabled="isBusy"
      class="flex items-center gap-2 font-bold transition-all shadow-sm active:scale-95 bg-white dark:bg-gray-800 text-foreground border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700"
    >
      <Play :size="14" :fill="isBusy ? 'none' : 'currentColor'" />
      <span>{{ isBusy ? 'Requesting...' : 'Run System Task' }}</span>
      <ChevronDown :size="14" class="opacity-70" />
    </BaseButton>

    <!-- Dropdown Menu -->
    <div v-if="isOpen" class="absolute right-0 top-full mt-2 w-64 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-100">
      <div class="bg-gray-50 dark:bg-gray-900/50 px-3 py-2 border-b border-gray-100 dark:border-gray-700 text-[10px] font-bold text-gray-400 dark:text-gray-500 uppercase tracking-wider">
        Available Maintenance Jobs
      </div>
      <div class="p-1">
        <button 
          v-for="job in JOBS" 
          :key="job.id"
          @click="runJob(job.id)"
          class="w-full text-left px-3 py-2 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md group flex items-start gap-3 transition-colors"
        >
          <div class="p-1.5 bg-gray-100 dark:bg-gray-700 group-hover:bg-white dark:group-hover:bg-gray-600 rounded text-gray-500 dark:text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
            <component :is="job.icon" :size="16" />
          </div>
          <div>
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">{{ job.label }}</div>
            <div class="text-[10px] text-gray-500 dark:text-gray-400 leading-tight">{{ job.desc }}</div>
          </div>
        </button>
      </div>
    </div>

    <!-- Backdrop -->
    <div v-if="isOpen" @click="isOpen = false" class="fixed inset-0 z-40"></div>
  </div>
</template>