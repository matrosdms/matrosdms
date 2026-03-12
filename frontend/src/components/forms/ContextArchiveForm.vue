<script setup>
import { useDmsStore } from '@/stores/dms'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { Archive, FolderArchive } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import { ContextService } from '@/services/ContextService'

const dms = useDmsStore()
const queryClient = useQueryClient()

const onConfirm = async () => {
  const promise = push.promise('Archiving context...')
  try {
    await ContextService.archive(dms.selectedContext.uuid)

    queryClient.invalidateQueries({ queryKey: ['contexts'] })
    dms.setSelectedContext(null)
    dms.cancelCreation()
    promise.resolve('Context archived')
  } catch(err) {
    promise.reject(`Failed: ${err.message}`)
  }
}
</script>

<template>
  <div class="h-full flex flex-col bg-muted/20 transition-colors">
    <div class="p-2 border-b border-border bg-background h-[35px] flex items-center shadow-sm">
      <span class="text-[13px] font-bold text-amber-600 flex items-center gap-2"><Archive :size="14"/> Archive Context</span>
    </div>
    <div class="flex-1 p-6 flex flex-col items-center justify-center text-center overflow-auto">
      <div class="bg-background p-8 rounded-lg border border-border shadow-sm max-w-md w-full transition-colors">
        <div class="w-16 h-16 bg-amber-100 dark:bg-amber-900/30 rounded-full flex items-center justify-center mx-auto mb-4">
          <Archive class="text-amber-600 dark:text-amber-400" :size="32" />
        </div>
        <h3 class="text-lg font-bold text-foreground mb-2">Archive "{{ dms.selectedContext?.name }}"?</h3>
        <p class="text-sm text-muted-foreground mb-6">
          This context will be archived and hidden from the active list. It can be restored at any time.
        </p>
        
        <div class="flex flex-col gap-3">
          <BaseButton variant="warning" class="w-full" @click="onConfirm">
            Yes, Archive Context
          </BaseButton>
          <BaseButton variant="outline" class="w-full" @click="dms.cancelCreation">
            Cancel
          </BaseButton>
        </div>
      </div>
    </div>
  </div>
</template>