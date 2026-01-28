<script setup>
import { useDmsStore } from '@/stores/dms'
import { client } from '@/api/client'
import { push } from 'notivue'
import { useQueryClient } from '@tanstack/vue-query'
import { Archive, AlertTriangle } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue' // Updated Import

const dms = useDmsStore()
const queryClient = useQueryClient()

const onConfirm = async () => {
  const promise = push.promise('Archiving context...')
  try {
    const { error } = await client.DELETE('/api/contexts/{id}', {
      params: { path: { id: dms.selectedContext.uuid } }
    })
    
    if (error) throw new Error(error.message || 'Archive failed')

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
      <span class="text-[13px] font-bold text-destructive flex items-center gap-2"><Archive :size="14"/> Archive Context</span>
    </div>
    <div class="flex-1 p-6 flex flex-col items-center justify-center text-center overflow-auto">
      <div class="bg-background p-8 rounded-lg border border-border shadow-sm max-w-md w-full transition-colors">
        <div class="w-16 h-16 bg-destructive/10 rounded-full flex items-center justify-center mx-auto mb-4">
          <AlertTriangle class="text-destructive" :size="32" />
        </div>
        <h3 class="text-lg font-bold text-foreground mb-2">Archive "{{ dms.selectedContext?.name }}"?</h3>
        <p class="text-sm text-muted-foreground mb-6">
          This context will be moved to the archive. It will no longer appear in the active list unless you change your filter settings.
        </p>
        
        <div class="flex flex-col gap-3">
          <BaseButton variant="destructive" class="w-full" @click="onConfirm">
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