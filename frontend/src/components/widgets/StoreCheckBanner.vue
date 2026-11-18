<script setup lang="ts">
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { AlertTriangle, X } from 'lucide-vue-next'
import { AdminService } from '@/services/AdminService'
import { queryKeys } from '@/composables/queries/queryKeys'
import { useAuthStore } from '@/stores/auth'

// Shows administrators when the document store and the database disagree - e.g. documents on disk
// the database does not know because it fell back to an older state. The server compares them after
// every start and whenever the Integrity Check runs.
const auth = useAuthStore()

const { data } = useQuery({
  queryKey: queryKeys.admin.storeCheck,
  queryFn: AdminService.getStoreCheck,
  enabled: computed(() => auth.isAuthenticated && auth.isAdmin),
  staleTime: 60_000,
  retry: false,
})

// Hiding applies to this result only; a later check that still finds something shows it again
const hiddenCheck = ref<string | null>(null)
const warnings = computed(() =>
  data.value && data.value.checkedAt !== hiddenCheck.value ? data.value.warnings ?? [] : [])
</script>

<template>
  <div
    v-if="warnings.length"
    role="alert"
    class="flex items-start gap-3 px-4 py-2 bg-amber-50 dark:bg-amber-900/20 border-b border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200 text-sm z-10"
  >
    <AlertTriangle :size="16" class="shrink-0 mt-0.5" />
    <div class="flex-1 space-y-0.5">
      <p v-for="(warning, i) in warnings" :key="i">{{ warning }}</p>
    </div>
    <button
      @click="hiddenCheck = data?.checkedAt ?? null"
      class="p-1 rounded hover:bg-amber-100 dark:hover:bg-amber-800/40 transition-colors"
      title="Hide until the next check"
      aria-label="Hide warning"
    >
      <X :size="14" />
    </button>
  </div>
</template>
