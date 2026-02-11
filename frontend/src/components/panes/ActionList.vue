<script setup lang="ts">
import { onMounted, computed, ref, watch, nextTick } from 'vue'
import BasePane from '@/components/ui/BasePane.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import ActionItem from '@/components/panes/ActionItem.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { useActionStore } from '@/stores/action'
import { useWorkflowStore } from '@/stores/workflow'
import { useDmsStore } from '@/stores/dms'
import { UserService } from '@/services/UserService'
import { useListNavigation } from '@/composables/useListNavigation'
import { useQuery } from '@tanstack/vue-query'
import { RefreshCw, Plus, CheckSquare, Filter, EyeOff, Eye, CalendarDays } from 'lucide-vue-next'
import { EActionStatus } from '@/enums'

const actionStore = useActionStore()
const workflow = useWorkflowStore()
const dms = useDmsStore()

const showDone = ref(false)
const showFilters = ref(false)
const searchQuery = ref('')
const timeHorizon = ref('30') 

const filters = ref({ assignee: '', minDate: '' })

const { data: users } = useQuery({ queryKey: ['users'], queryFn: UserService.getAll })

const load = () => {
    actionStore.fetchActions({
        assignee: filters.value.assignee || undefined,
        minDate: filters.value.minDate || undefined,
        status: showDone.value ? undefined : [EActionStatus.OPEN, EActionStatus.IN_PROGRESS, EActionStatus.ON_HOLD]
    })
}

watch([filters, showDone], () => load(), { deep: true })
onMounted(() => load())

const filteredActions = computed(() => {
    let list = actionStore.actions || []
    if (searchQuery.value) {
        const q = searchQuery.value.toLowerCase()
        list = list.filter(a => a.name?.toLowerCase().includes(q) || a.description?.toLowerCase().includes(q))
    }
    if (timeHorizon.value !== 'all') {
        const cutoff = new Date()
        cutoff.setDate(cutoff.getDate() + parseInt(timeHorizon.value))
        list = list.filter(a => {
            if (!a.dueDate) return true
            return new Date(a.dueDate) <= cutoff
        })
    }
    return list
})

const onCreate = () => {
    if (dms.selectedItem) workflow.startActionCreation({ itemId: dms.selectedItem.uuid })
    else if (dms.selectedContext) workflow.startActionCreation({ contextId: dms.selectedContext.uuid })
    else workflow.startActionCreation()
}

const onEdit = (action: any) => workflow.startActionEditing(action)

// ── Keyboard Navigation ──────────────────────────────────────────────────
const activeIndex = ref(-1)
const listContainerRef = ref<HTMLDivElement | null>(null)

const focusListContainer = () => {
  nextTick(() => listContainerRef.value?.focus({ preventScroll: true }))
}

watch(filteredActions, (list) => {
  if (!list.length) { activeIndex.value = -1; return }
  if (activeIndex.value >= list.length) activeIndex.value = list.length - 1
  if (activeIndex.value === -1 && list.length > 0) activeIndex.value = 0
})

const { handleKey: handleListKey } = useListNavigation({
  listLength: computed(() => filteredActions.value.length),
  activeIndex,
  onSelect: (index) => {
    const action = filteredActions.value[index]
    if (action) onEdit(action)
  },
})

const handleListKeyDown = (event: KeyboardEvent) => handleListKey(event)

const handleActionClick = (action: any, index: number) => {
  activeIndex.value = index
  onEdit(action)
  focusListContainer()
}
</script>

<template>
  <BasePane title="Action List" :count="filteredActions.length">
    
    <template #actions>
        <button @click="showDone = !showDone" class="btn-icon" :class="{'text-green-600 bg-green-50 dark:bg-green-900/30 dark:text-green-400': showDone}" :title="showDone ? 'Hide Completed' : 'Show Completed'">
            <component :is="showDone ? Eye : EyeOff" :size="14" />
        </button>
        <button @click="showFilters = !showFilters" class="btn-icon" :class="{'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400': showFilters}" title="Toggle Filters">
            <Filter :size="14" />
        </button>
        <button @click="load" class="btn-icon" title="Refresh">
            <RefreshCw :size="14" :class="{'animate-spin': actionStore.isLoading}" />
        </button>
        <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1"></div>
        <button @click="onCreate" class="btn-icon-primary" title="New Task">
            <Plus :size="16" stroke-width="3" />
        </button>
    </template>

    <template #filter>
        <SearchInput v-model="searchQuery" placeholder="Search tasks..." />
        
        <!-- Expanded Filters -->
        <div v-if="showFilters" class="mt-2 flex gap-2 animate-in slide-in-from-top-2">
            <div class="flex-1">
                <select v-model="filters.assignee" class="w-full text-xs border border-gray-300 dark:border-gray-700 rounded p-1 bg-white dark:bg-gray-800 dark:text-gray-200 h-7 outline-none">
                    <option value="">All Users</option>
                    <option v-for="u in users" :key="u.uuid" :value="u.uuid">{{ u.firstname || u.name }}</option>
                </select>
            </div>
            <div class="flex-1">
                <div class="flex items-center bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded h-7 px-1">
                    <CalendarDays :size="12" class="text-gray-400 mr-1" />
                    <select v-model="timeHorizon" class="w-full text-xs bg-transparent dark:text-gray-200 outline-none">
                        <option value="30">Due in 30 Days</option>
                        <option value="90">Due in 90 Days</option>
                        <option value="all">Show All Future</option>
                    </select>
                </div>
            </div>
        </div>
    </template>

    <div
      ref="listContainerRef"
      class="p-2 space-y-2 outline-none"
      tabindex="0"
      @keydown="handleListKeyDown"
      @focus="() => { if (activeIndex === -1 && filteredActions.length) activeIndex = 0 }"
    >
        <div v-if="actionStore.isLoading && !actionStore.actions.length" class="flex justify-center p-6">
            <div class="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        </div>
        
        <EmptyState 
            v-else-if="filteredActions.length === 0" 
            :icon="CheckSquare" 
            title="No tasks found" 
            description="Create a new task or adjust your filters."
        />
        
        <ActionItem 
            v-else
            v-for="(action, index) in filteredActions" 
            :key="action.uuid" 
            :action="action"
            :class="index === activeIndex ? 'ring-2 ring-blue-400/80 ring-offset-1 ring-offset-white dark:ring-offset-gray-900' : ''"
            @toggle="actionStore.toggleComplete(action)"
            @click="handleActionClick(action, index)" 
        />
    </div>
  </BasePane>
</template>