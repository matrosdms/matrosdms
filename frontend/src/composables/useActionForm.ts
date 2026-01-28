import { ref, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useActionStore } from '@/stores/action'
import { ActionService } from '@/services/ActionService'
import { formatDateForInput } from '@/lib/utils'
import { push } from 'notivue'
import { 
    EActionPriority, type EActionPriorityType,
    EActionStatus, type EActionStatusType 
} from '@/enums'

export interface ActionFormData {
    name: string;
    description: string;
    priority: EActionPriorityType;
    dueDate: string;
    assigneeIdentifier: string;
    status: EActionStatusType;
    itemIdentifier: string;
    contextIdentifier: string;
    resolution: string;
    externalActionTracker: string;
    externalId: string;
    history: any[];
    version: number;
}

export function useActionForm(initialData: any, contextId?: string, itemId?: string, onSuccess?: () => void) {
    const auth = useAuthStore()
    const actionStore = useActionStore()
    
    const isLoading = ref(false)
    const originalState = ref<{ status: string, priority: string, resolution: string } | null>(null)

    const form = ref<ActionFormData>({
        name: '',
        description: '',
        priority: EActionPriority.NORMAL,
        dueDate: '',
        assigneeIdentifier: auth.currentUser?.uuid || '',
        status: EActionStatus.OPEN,
        itemIdentifier: itemId || '',
        contextIdentifier: contextId || '',
        resolution: '',
        externalActionTracker: 'NONE',
        externalId: '',
        history: [],
        version: 0
    })

    const isEdit = computed(() => !!initialData?.uuid)
    const isDone = computed(() => form.value.status === EActionStatus.DONE)
    const hasExternal = computed(() => form.value.externalActionTracker && form.value.externalActionTracker !== 'NONE')

    // Initialize Form
    const initialize = () => {
        if (initialData) {
            originalState.value = {
                status: initialData.status,
                priority: initialData.priority,
                resolution: initialData.resolution || ''
            }

            form.value = {
                name: initialData.name,
                description: initialData.description || '',
                priority: (initialData.priority as EActionPriorityType) || EActionPriority.NORMAL,
                dueDate: formatDateForInput(initialData.dueDate),
                assigneeIdentifier: initialData.assignee?.uuid || '',
                status: (initialData.status as EActionStatusType) || EActionStatus.OPEN,
                itemIdentifier: initialData.itemIdentifier || '',
                contextIdentifier: initialData.contextIdentifier || '',
                resolution: initialData.resolution || '',
                externalActionTracker: initialData.externalActionTracker || 'NONE',
                externalId: initialData.externalId || '',
                history: initialData.history ? [...initialData.history] : [],
                version: initialData.version || 0
            }
        }
    }

    watch(() => initialData, initialize, { immediate: true })

    const addHistoryEntry = (msg: string, user: string) => {
        form.value.history.push({
            user: user,
            message: msg,
            timestamp: new Date().toISOString()
        })
    }

    const runAutoAudit = () => {
        if (!isEdit.value || !originalState.value) return

        const s = form.value
        const o = originalState.value
        
        if (s.status !== o.status) {
            if (s.status === 'DONE') {
                const resText = s.resolution && s.resolution !== o.resolution 
                    ? `\nResolution: "${s.resolution}"` 
                    : ''
                addHistoryEntry(`Marked task as DONE.${resText}`, 'System')
            } else {
                addHistoryEntry(`Changed status to ${s.status}`, 'System')
            }
        } 
        else if (s.resolution !== o.resolution && s.resolution) {
            addHistoryEntry(`Updated resolution: "${s.resolution}"`, 'System')
        }

        if (s.priority !== o.priority) {
            addHistoryEntry(`Changed priority to ${s.priority}`, 'System')
        }
    }

    const save = async () => {
        if (!form.value.name) {
            push.warning("Task name is required")
            return
        }

        runAutoAudit()

        isLoading.value = true
        try {
            const payload = {
                ...form.value,
                dueDate: form.value.dueDate ? new Date(form.value.dueDate).toISOString() : undefined,
                assigneeIdentifier: form.value.assigneeIdentifier || undefined,
                itemIdentifier: form.value.itemIdentifier || undefined,
                contextIdentifier: form.value.contextIdentifier || undefined
            }

            if (isEdit.value) {
                await ActionService.update(initialData.uuid, payload)
                push.success("Task updated")
            } else {
                await ActionService.create(payload)
                push.success("Task created")
            }

            await actionStore.fetchActions()
            if (onSuccess) onSuccess()
        } catch (e: any) {
            if (!e.message?.includes('409')) push.error(e.message)
        } finally {
            isLoading.value = false
        }
    }

    const setStatus = (status: EActionStatusType) => {
        form.value.status = status
    }

    return {
        form,
        isEdit,
        isDone,
        isLoading,
        hasExternal,
        addHistoryEntry,
        save,
        setStatus
    }
}