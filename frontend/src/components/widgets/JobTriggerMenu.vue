<script setup lang="ts">
import { computed, ref } from 'vue'
import { Play, ChevronDown, ShieldCheck, Search as SearchIcon, Archive, Check } from 'lucide-vue-next'
import { AdminService } from '@/services/AdminService'
import { push } from 'notivue'
import { EJobType, EJobTypeLabels } from '@/enums'
import BaseButton from '@/components/ui/BaseButton.vue'

const emit = defineEmits(['job-started'])

const isOpen = ref(false)
const isBusy = ref(false)
// Picking a job only selects it; it starts with the Start button
const selected = ref<EJobType | null>(null)

const JOBS = [
  { id: EJobType.INTEGRITY_CHECK, label: EJobTypeLabels[EJobType.INTEGRITY_CHECK], icon: ShieldCheck, desc: 'Verify file hashes and compare the document store with the database' },
  { id: EJobType.REINDEX_SEARCH, label: EJobTypeLabels[EJobType.REINDEX_SEARCH], icon: SearchIcon, desc: 'Rebuild Lucene/Elastic indexes' },
  { id: EJobType.EXPORT_ARCHIVE, label: EJobTypeLabels[EJobType.EXPORT_ARCHIVE], icon: Archive, desc: 'Create a ZIP dump of all active documents' }
]

const selectedJob = computed(() => JOBS.find(j => j.id === selected.value))

const toggle = () => {
  isOpen.value = !isOpen.value
  selected.value = null
}

const close = () => {
  isOpen.value = false
  selected.value = null
}

const startSelected = async () => {
  const job = selectedJob.value
  if (!job || isBusy.value) return
  close()
  isBusy.value = true
  try {
    const response = await AdminService.startJob(job.id)
    // Backend returns a simple string message, not JSON
    push.success(response || `Job '${job.label}' queued successfully`)
    emit('job-started')
  } catch (e: any) {
    push.error(`${job.label} not started: ${e.message}`)
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
      @click="toggle"
      :disabled="isBusy"
      class="flex items-center gap-2 font-bold transition-all shadow-sm active:scale-95 bg-white dark:bg-gray-800 text-foreground border border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700"
    >
      <Play :size="14" :fill="isBusy ? 'none' : 'currentColor'" />
      <span>{{ isBusy ? 'Requesting...' : 'Run System Task' }}</span>
      <ChevronDown :size="14" class="opacity-70" />
    </BaseButton>

    <!-- Dropdown Menu -->
    <div v-if="isOpen" class="absolute right-0 top-full mt-2 w-72 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-100">
      <div class="bg-gray-50 dark:bg-gray-900/50 px-3 py-2 border-b border-gray-100 dark:border-gray-700 text-[10px] font-bold text-gray-400 dark:text-gray-500 uppercase tracking-wider">
        Available Maintenance Jobs
      </div>
      <div class="p-1" role="radiogroup" aria-label="Maintenance job">
        <button
          v-for="job in JOBS"
          :key="job.id"
          role="radio"
          :aria-checked="selected === job.id"
          @click="selected = job.id"
          class="w-full text-left px-3 py-2 rounded-md group flex items-start gap-3 transition-colors"
          :class="selected === job.id
            ? 'bg-blue-50 dark:bg-blue-900/30 ring-1 ring-blue-200 dark:ring-blue-800'
            : 'hover:bg-gray-50 dark:hover:bg-gray-700/50'"
        >
          <div class="p-1.5 rounded transition-colors"
            :class="selected === job.id
              ? 'bg-white dark:bg-gray-700 text-blue-600 dark:text-blue-400'
              : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400'">
            <component :is="job.icon" :size="16" />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-gray-800 dark:text-gray-200">{{ job.label }}</div>
            <div class="text-[10px] text-gray-500 dark:text-gray-400 leading-tight">{{ job.desc }}</div>
          </div>
          <Check v-if="selected === job.id" :size="16" class="text-blue-600 dark:text-blue-400 shrink-0 mt-1" />
        </button>
      </div>
      <div class="flex items-center justify-end gap-2 px-3 py-2 border-t border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-900/50">
        <button
          @click="close"
          class="px-3 py-1.5 text-xs font-medium rounded-md text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
        >
          Cancel
        </button>
        <button
          @click="startSelected"
          :disabled="!selectedJob"
          class="px-3 py-1.5 text-xs font-bold rounded-md flex items-center gap-1.5 transition-colors bg-blue-600 text-white hover:bg-blue-700 disabled:bg-gray-200 disabled:text-gray-400 dark:disabled:bg-gray-700 dark:disabled:text-gray-500 disabled:cursor-not-allowed"
        >
          <Play :size="12" fill="currentColor" />
          {{ selectedJob ? `Start ${selectedJob.label}` : 'Select a job' }}
        </button>
      </div>
    </div>

    <!-- Backdrop -->
    <div v-if="isOpen" @click="close" class="fixed inset-0 z-40"></div>
  </div>
</template>
