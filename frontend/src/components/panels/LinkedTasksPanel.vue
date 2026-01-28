<script setup lang="ts">
import { computed } from 'vue'
import { CheckSquare, Plus, Loader2 } from 'lucide-vue-next'
import { useActionStore } from '@/stores/action'
import { useWorkflowStore } from '@/stores/workflow'
import { useQuery } from '@tanstack/vue-query'
import { ActionService } from '@/services/ActionService'
import { EActionStatus } from '@/enums'
import BaseButton from '@/components/ui/BaseButton.vue'

const props = defineProps<{
  itemId: string
}>()

const actionStore = useActionStore()
const workflow = useWorkflowStore()

// Fetch Tasks specifically for this item
const { data: tasks, isLoading, refetch } = useQuery({
    queryKey: ['actions', 'item', props.itemId],
    queryFn: () => ActionService.getByItem(props.itemId),
    enabled: computed(() => !!props.itemId)
})

const onAddTask = () => {
    workflow.startActionCreation({ itemId: props.itemId })
}

const toggleTask = async (task: any) => {
    await actionStore.toggleComplete(task)
    refetch() // Refresh local list state
}
</script>

<template>
  <div>
     <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
            <CheckSquare :size="16" class="text-primary" />
            <label class="text-xs font-bold text-muted-foreground uppercase tracking-wide">Linked Tasks</label>
        </div>
        <BaseButton variant="ghost" size="sm" class="h-6 text-primary" @click="onAddTask">
            <Plus :size="12" class="mr-1" /> Add Task
        </BaseButton>
     </div>
     
     <div v-if="isLoading" class="flex justify-center p-4">
        <Loader2 class="animate-spin text-muted-foreground" :size="16" />
     </div>

     <div v-else-if="tasks && tasks.length > 0" class="flex flex-col gap-2">
         <div 
            v-for="t in tasks" 
            :key="t.uuid" 
            class="flex items-start gap-3 p-2.5 border rounded-lg bg-background/50 hover:bg-background hover:border-primary/30 transition-colors group"
         >
            <input 
                type="checkbox" 
                :checked="t.status === EActionStatus.DONE" 
                @change="toggleTask(t)" 
                class="mt-1 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer" 
            />
            <div class="flex-1 min-w-0">
                <div class="text-sm font-medium text-foreground truncate" :class="{'line-through text-muted-foreground': t.status === EActionStatus.DONE}">
                    {{ t.name }}
                </div>
                <div v-if="t.assignee" class="text-[10px] text-muted-foreground mt-0.5 flex items-center gap-1">
                    Assignee: <span class="font-medium text-foreground">{{ t.assignee.firstname || t.assignee.name }}</span>
                </div>
            </div>
         </div>
     </div>

     <div v-else class="text-xs text-muted-foreground italic bg-muted/30 p-4 rounded-lg border border-dashed text-center">
         No tasks linked to this document.
     </div>
  </div>
</template>