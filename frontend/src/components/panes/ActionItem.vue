<script setup lang="ts">
import { computed } from 'vue'
import { CheckCircle2, Circle, Clock, User, FileText, Folder, ExternalLink } from 'lucide-vue-next'
import { parseBackendDate } from '@/lib/utils'
import { EActionPriority } from '@/enums'
import { useMatrosData } from '@/composables/useMatrosData'
import { ItemService } from '@/services/ItemService'
import { useQuery } from '@tanstack/vue-query'

const props = defineProps<{
  action: any
}>()

defineEmits(['toggle', 'click'])

const { contexts } = useMatrosData()

const isDone = computed(() => props.action.status === 'DONE')
const isHighPriority = computed(() => props.action.priority === EActionPriority.HIGH)
const isOverdue = computed(() => props.action.dueDate && new Date(props.action.dueDate) < new Date() && !isDone.value)

const formattedDate = computed(() => {
    const d = parseBackendDate(props.action.dueDate)
    return d ? d.toLocaleDateString() : ''
})

const hasExternal = computed(() => !!props.action.externalActionTracker && props.action.externalActionTracker !== 'NONE')

const contextName = computed(() => {
    if (!props.action.contextIdentifier) return null
    const ctx = contexts.value.find((c: any) => c.uuid === props.action.contextIdentifier)
    return ctx ? ctx.name : 'Unknown Context'
})

const { data: itemData } = useQuery({
    queryKey: ['item-mini', props.action.itemIdentifier],
    queryFn: () => ItemService.getById(props.action.itemIdentifier!),
    enabled: computed(() => !!props.action.itemIdentifier),
    staleTime: 1000 * 60 * 10 
})

const itemName = computed(() => itemData.value?.name || 'Document')
</script>

<template>
  <div 
    class="flex items-start gap-3 p-3 rounded-md border transition-all group relative cursor-pointer select-none mb-1.5"
    :class="isDone ? 'bg-gray-50 dark:bg-gray-800/50 border-gray-100 dark:border-gray-800 opacity-60' : 'bg-white dark:bg-gray-900 border-gray-200 dark:border-gray-700 hover:border-blue-300 dark:hover:border-blue-700 hover:shadow-md'"
    @click="$emit('click')"
  >
    <button 
        @click.stop="$emit('toggle')" 
        class="mt-0.5 transition-colors focus:outline-none"
        :class="isDone ? 'text-green-600 dark:text-green-500' : 'text-gray-300 dark:text-gray-600 hover:text-green-500 dark:hover:text-green-400'"
    >
        <CheckCircle2 v-if="isDone" :size="18" />
        <Circle v-else :size="18" />
    </button>
    
    <div class="flex-1 min-w-0">
        <!-- Header -->
        <div class="flex justify-between items-start gap-2">
            <span class="text-sm font-medium text-gray-800 dark:text-gray-200 leading-tight truncate" :class="{'line-through text-gray-500 dark:text-gray-500': isDone}">
                {{ action.name }}
            </span>
            
            <div class="flex items-center gap-1 shrink-0">
                <div v-if="hasExternal" class="text-[9px] font-bold text-gray-500 dark:text-gray-400 border border-gray-200 dark:border-gray-700 px-1 rounded flex items-center gap-1" title="Synced Externally">
                    <ExternalLink :size="8" /> {{ action.externalActionTracker }}
                </div>
                <div v-if="isHighPriority && !isDone" class="text-[9px] font-bold text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 px-1.5 py-0.5 rounded border border-red-100 dark:border-red-900 uppercase tracking-wider">
                    High
                </div>
            </div>
        </div>
        
        <!-- TARGET LINKS -->
        <div class="mt-1.5 flex flex-wrap items-center gap-2" v-if="action.itemIdentifier || action.contextIdentifier">
             <div v-if="action.itemIdentifier" 
                  class="flex items-center gap-1.5 text-[10px] text-purple-700 dark:text-purple-300 bg-purple-50 dark:bg-purple-900/30 border border-purple-100 dark:border-purple-800 px-1.5 py-0.5 rounded max-w-[140px]" 
                  :title="'Document: ' + itemName">
                <FileText :size="10" class="shrink-0" /> 
                <span class="truncate font-medium">{{ itemName }}</span>
             </div>

             <div v-if="action.contextIdentifier" 
                  class="flex items-center gap-1.5 text-[10px] text-blue-700 dark:text-blue-300 bg-blue-50 dark:bg-blue-900/30 border border-blue-100 dark:border-blue-800 px-1.5 py-0.5 rounded max-w-[140px]" 
                  :title="'Context: ' + contextName">
                <Folder :size="10" class="shrink-0" /> 
                <span class="truncate font-medium">{{ contextName }}</span>
             </div>
        </div>

        <!-- Footer -->
        <div class="mt-2 flex flex-wrap items-center gap-3">
            <span v-if="action.dueDate" class="flex items-center gap-1 text-[10px] transition-colors" 
                :class="isOverdue ? 'text-red-600 dark:text-red-400 font-bold' : 'text-gray-500 dark:text-gray-400'">
                <Clock :size="10" /> {{ formattedDate }}
            </span>
            
            <span v-if="action.assignee" class="flex items-center gap-1 text-[10px] text-gray-600 dark:text-gray-400">
                <User :size="10" /> {{ action.assignee.firstname || action.assignee.name }}
            </span>
        </div>
    </div>
  </div>
</template>