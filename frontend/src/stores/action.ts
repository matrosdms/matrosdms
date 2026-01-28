import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ActionService } from '@/services/ActionService'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import type { components } from '@/types/schema'
import { EActionStatus, type EActionStatusType } from '@/enums'

type MAction = components['schemas']['MAction'];

export const useActionStore = defineStore('action', () => {
    const ui = useUIStore()
    const auth = useAuthStore()

    const actions = ref<MAction[]>([])
    const isLoading = ref(false)
    const currentFilters = ref<{ status?: string[], assignee?: string, minDate?: string }>({})

    async function fetchActions(filters: { status?: string[], assignee?: string, minDate?: string } = {}) {
        if (!auth.isAuthenticated) return;

        isLoading.value = true
        currentFilters.value = filters
        
        try {
            const pageable = { size: 100, sort: ['dueDate,asc'] }
            const response = await ActionService.getAll(pageable, filters)
            actions.value = response.content || []
        } catch (e: any) {
            ui.addLog(`Failed to load actions: ${e.message}`, 'error')
        } finally {
            isLoading.value = false
        }
    }

    async function fetchActionsForItem(itemId: string) {
        if (!auth.isAuthenticated) return [];
        return await ActionService.getByItem(itemId)
    }

    async function toggleComplete(action: MAction) {
        if (!action.uuid || !action.name) return
        
        // Backend now defines terminal states logic
        // We define DONE and REJECTED as terminal "Completed" states
        const isCurrentlyCompleted = action.status === EActionStatus.DONE || action.status === EActionStatus.REJECTED;
        
        // Toggle logic: If terminal -> Reopen. If open -> Mark Done.
        const newStatus: EActionStatusType = isCurrentlyCompleted ? EActionStatus.OPEN : EActionStatus.DONE;
        const oldStatus = action.status;
        
        // Optimistic UI Update (Strictly Typed)
        action.status = newStatus

        try {
            await ActionService.update(action.uuid, {
                name: action.name,
                status: newStatus,
                version: action.version
            })
            
            ui.addLog(`Task marked as ${newStatus}`, 'success')
            fetchActions(currentFilters.value)
        } catch (e) {
            // Revert on failure
            action.status = oldStatus 
            ui.addLog("Update failed - reverting status", 'error')
        }
    }

    const myPendingCount = computed(() => {
        const myId = auth.currentUser?.uuid
        if (!myId) return 0
        return actions.value.filter(a => 
            a.assignee?.uuid === myId && 
            a.status !== EActionStatus.DONE &&
            a.status !== EActionStatus.REJECTED &&
            a.status !== EActionStatus.ON_HOLD
        ).length
    })

    return {
        actions,
        isLoading,
        myPendingCount,
        fetchActions,
        fetchActionsForItem,
        toggleComplete
    }
})