<script setup lang="ts">
import { ref, computed } from 'vue'
import BaseFormPanel from '@/components/ui/BaseFormPanel.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import BaseTextarea from '@/components/ui/BaseTextarea.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
import ActionHistory from '@/components/ui/ActionHistory.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { useAuthStore } from '@/stores/auth'
import { useMatrosData } from '@/composables/useMatrosData'
import { useActionForm } from '@/composables/useActionForm'
import { useAdminQueries } from '@/composables/queries/useAdminQueries'
import { push } from 'notivue'
import { FileText, Folder, ListChecks, Eye, Check, RotateCcw, X, CheckCircle2 } from 'lucide-vue-next'
import { EActionPriorityList, EActionPriorityLabels, EActionStatusList, EActionStatusLabels, EActionStatus } from '@/enums'

const props = defineProps<{ initialData?: any; contextId?: string; itemId?: string; }>()
const emit = defineEmits(['close'])
const workflow = useWorkflowStore() 
const auth = useAuthStore()
const { contexts } = useMatrosData()
const { form, isEdit, isDone, isLoading, hasExternal, addHistoryEntry, save, setStatus } = useActionForm(props.initialData, props.contextId, props.itemId, () => { emit('close'); workflow.cancelCreation(); })
const activeTab = ref<'form' | 'preview'>('form')

const { useUsers } = useAdminQueries()
const { data: users } = useUsers()

const contextName = computed(() => {
    if (!form.value.contextIdentifier) return 'Context'
    const found = contexts.value.find((c: any) => c.uuid === form.value.contextIdentifier)
    return found ? found.name : 'Unknown Context'
})

const completeTask = () => { setStatus(EActionStatus.DONE); push.info('Status set to Done.') }
const reopenTask = () => { setStatus(EActionStatus.OPEN); push.info('Task reopened.') }
const onAddComment = (msg: string) => { addHistoryEntry(msg, auth.currentUser?.name || 'User') }
</script>

<template>
  <BaseFormPanel :title="isEdit ? 'Edit Task' : 'New Task'" :subtitle="hasExternal ? `Synced: ${form.externalActionTracker}` : ''" :is-loading="isLoading" @submit="save" @cancel="workflow.cancelCreation" :wide="activeTab === 'preview'">
    <template #header-actions>
        <div v-if="form.itemIdentifier" class="flex bg-muted/50 rounded-md p-0.5 border border-border">
            <button @click="activeTab = 'form'" class="p-1.5 rounded-sm transition-all" :class="activeTab === 'form' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'"><ListChecks :size="14" /></button>
            <button @click="activeTab = 'preview'" class="p-1.5 rounded-sm transition-all" :class="activeTab === 'preview' ? 'bg-background shadow-sm text-primary' : 'text-muted-foreground hover:text-foreground'"><Eye :size="14" /></button>
        </div>
    </template>

    <div v-show="activeTab === 'form'" class="flex flex-col gap-4 animate-in fade-in slide-in-from-left-1 duration-200">
        
        <!-- Links Header -->
        <div class="flex flex-wrap gap-2 mb-1" v-if="form.itemIdentifier || form.contextIdentifier">
            <div v-if="form.itemIdentifier" class="flex items-center gap-1.5 px-2 py-1 rounded text-[10px] font-bold uppercase border bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-900/30 dark:text-purple-300 dark:border-purple-800">
                <FileText :size="12" /> Linked Document <button @click="form.itemIdentifier = ''" class="hover:text-red-500"><X :size="12"/></button>
            </div>
            <div v-if="form.contextIdentifier" class="flex items-center gap-1.5 px-2 py-1 rounded text-[10px] font-bold uppercase border bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800">
                <Folder :size="12" /> {{ contextName }} <button @click="form.contextIdentifier = ''" class="hover:text-red-500"><X :size="12"/></button>
            </div>
        </div>

        <BaseInput v-model="form.name" label="Subject" autofocus placeholder="Task subject..." />
        
        <div class="flex gap-4">
            <div class="flex-1">
                <BaseSelect 
                    v-model="form.status" 
                    label="Status" 
                    :class="isDone ? 'text-green-600 font-medium' : ''"
                >
                    <option v-for="opt in EActionStatusList" :key="opt" :value="opt">{{ EActionStatusLabels[opt] }}</option>
                </BaseSelect>
            </div>
            <div class="flex-1">
                <BaseSelect v-model="form.priority" label="Priority">
                    <option v-for="opt in EActionPriorityList" :key="opt" :value="opt">{{ EActionPriorityLabels[opt] }}</option>
                </BaseSelect>
            </div>
        </div>

        <div v-if="isDone" class="bg-green-50/50 dark:bg-green-900/10 p-3 rounded border border-green-200 dark:border-green-900">
            <BaseTextarea 
                v-model="form.resolution" 
                label="Resolution" 
                class="bg-background border-green-300 dark:border-green-800" 
            />
        </div>

        <BaseTextarea v-model="form.description" label="Description" :rows="4" />

        <div class="flex gap-4">
            <div class="flex-1">
                <BaseSelect v-model="form.assigneeIdentifier" label="Assignee">
                    <option value="">-- Unassigned --</option>
                    <option v-for="u in users" :key="u.uuid" :value="u.uuid">{{ u.firstname || u.name }}</option>
                </BaseSelect>
            </div>
            <div class="flex-1">
                <BaseInput v-model="form.dueDate" label="Due Date" type="date" />
            </div>
        </div>

        <ActionHistory :history="form.history" @add-comment="onAddComment" />
    </div>

    <div v-if="activeTab === 'preview'" class="h-full w-full bg-muted/20 min-h-[400px]">
        <DocumentPreview :identifier="form.itemIdentifier" source="item" />
    </div>

    <template #footer>
        <div class="flex justify-between items-center w-full">
            <div>
                <BaseButton v-if="!isDone" variant="success" size="sm" @click="completeTask">
                    <Check :size="14" class="mr-1" /> Complete Task
                </BaseButton>
                <BaseButton v-else variant="outline" size="sm" @click="reopenTask">
                    <RotateCcw :size="14" class="mr-1" /> Reopen
                </BaseButton>
            </div>
            <div class="flex gap-3">
                <BaseButton variant="outline" @click="workflow.cancelCreation" :disabled="isLoading">Cancel</BaseButton>
                <BaseButton variant="default" @click="save" :loading="isLoading" :disabled="!form.name">Save Changes</BaseButton>
            </div>
        </div>
    </template>
  </BaseFormPanel>
</template>