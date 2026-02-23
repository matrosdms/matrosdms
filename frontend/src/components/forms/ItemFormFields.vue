<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import AttributeGroup from '@/components/forms/AttributeGroup.vue'
import CategoryTree from '@/components/navigation/CategoryTree.vue' 
import IconPicker from '@/components/ui/IconPicker.vue'
import { useAdminQueries } from '@/composables/queries/useAdminQueries'
import { StoreService } from '@/services/StoreService'
import { ChevronDown, Check, Target, Bell, Sparkles } from 'lucide-vue-next'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { EStageList, EStageLabels, ERootCategory } from '@/enums'

const props = defineProps<{
  modelValue: any;
  touched?: boolean;
  reminderState?: {
      active: boolean;
      data: { name: string, dueDate: string, assigneeIdentifier: string };
  };
  aiHighlights?: {
      name?: boolean;
      date?: boolean;
      category?: boolean;
      context?: boolean;
      store?: boolean;
  }
}>()

const emit = defineEmits(['update:modelValue', 'update:reminderState'])
const dms = useDmsStore()
const ui = useUIStore()

const { useStores, useUsers } = useAdminQueries()
const { data: stores } = useStores()
const { data: users } = useUsers()

const isTypeDropdownOpen = ref(false)
const isHighlighted = ref(false)
const autoStoreNumber = ref(false)

const updateField = (key: string, value: any) => {
    emit('update:modelValue', { ...props.modelValue, [key]: value })
}

const updateReminder = (key: string, value: any) => {
    if (!props.reminderState) return
    const newData = { ...props.reminderState.data, [key]: value }
    emit('update:reminderState', { active: props.reminderState.active, data: newData })
}

const toggleReminder = () => {
    if (!props.reminderState) return
    emit('update:reminderState', { ...props.reminderState, active: !props.reminderState.active })
}

const onTypeSelected = (node: any) => {
    emit('update:modelValue', { 
        ...props.modelValue, 
        kindId: node.id, 
        kindName: node.label 
    })
    isTypeDropdownOpen.value = false
    isHighlighted.value = true
    setTimeout(() => isHighlighted.value = false, 800)
}

const fetchNextStoreNumber = async (storeId: string) => {
    if (!storeId) return
    try {
        const nextNum = await StoreService.getNextNumber(storeId)
        if (nextNum !== undefined && nextNum !== null) {
            updateField('storeItemNumber', String(nextNum))
        }
    } catch(e) { console.error("Next number fetch failed", e) }
}

const jumpToKind = () => {
    ui.setSidebarMode('tags')
    dms.setActiveContext(ERootCategory.KIND)
}

watch(() => props.modelValue.storeId, async (newId) => {
    if (newId && autoStoreNumber.value) await fetchNextStoreNumber(newId)
    else if (!newId) updateField('storeItemNumber', '')
})

watch(autoStoreNumber, async (val) => {
    if (val && props.modelValue.storeId) await fetchNextStoreNumber(props.modelValue.storeId)
})

onMounted(() => {
    if (props.modelValue.storeId && !props.modelValue.storeItemNumber) {
        autoStoreNumber.value = true
    }
})
</script>

<template>
  <div class="flex flex-col gap-4 relative" @click="isTypeDropdownOpen = false">
      
      <!-- Filename (Smart Highlight) -->
      <BaseInput 
          :model-value="modelValue.name" 
          @update:model-value="updateField('name', $event)"
          label="Filename" 
          :error="touched && !modelValue.name ? 'Name is required' : ''"
          :suggestion="aiHighlights?.name"
          autofocus 
      />

      <!-- Icon & Type & Stage -->
      <div class="flex gap-4 items-end">
          <div class="w-32 shrink-0">
             <IconPicker 
                :model-value="modelValue.icon"
                @update:model-value="updateField('icon', $event)"
             />
          </div>

          <div class="flex-1 flex flex-col gap-1.5">
             <label class="text-sm font-medium text-foreground flex justify-between items-center">
                <span>Type (Art)</span>
                <Sparkles v-if="aiHighlights?.category" class="text-purple-500 animate-pulse" :size="10" />
             </label>
             <div class="flex items-center gap-2" @click.stop>
                <div class="flex-1 relative">
                    <div 
                        @click="isTypeDropdownOpen = !isTypeDropdownOpen" 
                        class="flex h-9 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm cursor-pointer hover:bg-muted/50 transition-colors"
                        :class="[
                            touched && !modelValue.kindId ? 'border-destructive' : '', 
                            isHighlighted ? 'ring-2 ring-green-500 border-green-500' : '',
                            aiHighlights?.category ? 'border-purple-300 bg-purple-50/50 dark:border-purple-800 dark:bg-purple-900/10' : ''
                        ]"
                    >
                        <span :class="modelValue.kindName ? (aiHighlights?.category ? 'text-purple-900 dark:text-purple-100 font-medium' : 'text-foreground font-medium') : 'text-muted-foreground'">
                            {{ modelValue.kindName || 'Select Type...' }}
                        </span>
                        <Check v-if="isHighlighted" class="text-green-600" :size="16" />
                        <ChevronDown v-else :size="14" class="opacity-50" />
                    </div>
                    
                    <div v-if="isTypeDropdownOpen" class="absolute top-full left-0 w-full mt-1 bg-popover border rounded-md shadow-xl z-50 overflow-hidden animate-in fade-in zoom-in-95 duration-100">
                        <CategoryTree 
                            :overrideContext="ERootCategory.KIND" 
                            :selectionMode="true" 
                            :selectedId="modelValue.kindId" 
                            @node-selected="onTypeSelected" 
                        />
                    </div>
                </div>
                
                <button @click="jumpToKind" class="h-9 w-9 flex items-center justify-center rounded-md border border-input hover:bg-muted text-muted-foreground hover:text-primary transition-colors" title="Manage Types">
                    <Target :size="16" />
                </button>
             </div>
          </div>

          <div class="w-1/3">
             <label class="text-sm font-medium text-foreground block mb-1.5">Stage</label>
             <select 
                :value="modelValue.stage"
                @change="updateField('stage', ($event.target as HTMLSelectElement).value)"
                class="flex h-9 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
             >
                <option v-for="opt in EStageList" :key="opt" :value="opt">{{ EStageLabels[opt] }}</option>
             </select>
          </div>
      </div>

      <!-- Store Selection (Smart Highlight) -->
      <div class="flex gap-4 items-end">
            <div class="flex-1">
                <label class="text-sm font-medium text-foreground flex justify-between items-center mb-1.5">
                    <span>Physical Store</span>
                    <Sparkles v-if="aiHighlights?.store" class="text-purple-500 animate-pulse" :size="10" />
                </label>
                <select 
                    :value="modelValue.storeId"
                    @change="updateField('storeId', ($event.target as HTMLSelectElement).value)"
                    class="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring transition-colors"
                    :class="aiHighlights?.store ? 'border-purple-300 bg-purple-50/50 dark:border-purple-800 dark:bg-purple-900/10 text-purple-900 dark:text-purple-100 font-medium' : ''"
                    @click.stop
                >
                    <option value="" class="text-muted-foreground">None / Digital Only</option>
                    <option v-for="s in stores" :key="s.uuid" :value="s.uuid">{{ s.name }} ({{ s.shortname }})</option>
                </select>
            </div>
            
            <div class="w-32">
                <BaseInput 
                    :model-value="modelValue.storeItemNumber"
                    @update:model-value="updateField('storeItemNumber', $event)"
                    placeholder="No." 
                    :disabled="!modelValue.storeId || autoStoreNumber"
                />
            </div>
            
            <div class="h-9 flex items-center pb-1">
                <label class="flex items-center gap-2 cursor-pointer select-none text-xs font-medium text-muted-foreground hover:text-foreground transition-colors" :class="{'opacity-50 pointer-events-none': !modelValue.storeId}">
                    <input type="checkbox" v-model="autoStoreNumber" :disabled="!modelValue.storeId" class="rounded border-input text-primary focus:ring-primary" />
                    Auto-Gen
                </label>
            </div>
      </div>

      <!-- Dates (Smart Highlight) -->
      <div class="flex gap-4">
        <div class="flex-1">
            <BaseInput 
                :model-value="modelValue.issueDate" 
                @update:model-value="updateField('issueDate', $event)"
                label="Issue Date" 
                type="date" 
                :suggestion="aiHighlights?.date"
            />
        </div>
        <div class="flex-1">
             <BaseInput 
                :model-value="modelValue.dateExpire" 
                @update:model-value="updateField('dateExpire', $event)"
                label="Expires Date" 
                type="date" 
            />
        </div>
      </div>

      <div>
          <label class="text-sm font-medium text-foreground block mb-1.5">Description</label>
          <textarea 
            :value="modelValue.description" 
            @input="updateField('description', ($event.target as HTMLTextAreaElement).value)"
            rows="3" 
            class="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 resize-y"
          ></textarea>
      </div>

      <AttributeGroup 
        :model-value="modelValue.attributes || []" 
        @update:model-value="updateField('attributes', $event)" 
      />

      <!-- Reminder Section -->
      <div v-if="reminderState" class="mt-2 pt-4 border-t border-border">
        <div class="flex items-center justify-between mb-3">
            <label class="text-xs font-bold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
                <Bell :size="14" class="text-orange-500" /> Follow-up Task
            </label>
            <button 
                @click="toggleReminder" 
                class="text-[10px] px-2 py-0.5 rounded border transition-colors font-bold uppercase tracking-wider"
                :class="reminderState.active ? 'bg-orange-50 dark:bg-orange-900/30 text-orange-700 dark:text-orange-300 border-orange-200 dark:border-orange-800' : 'bg-muted text-muted-foreground border-transparent hover:bg-muted/80'"
            >
                {{ reminderState.active ? 'Active' : 'Disabled' }}
            </button>
        </div>

        <div v-if="reminderState.active" class="animate-in fade-in slide-in-from-top-2 bg-orange-50/30 dark:bg-orange-900/10 border border-orange-100 dark:border-orange-900 rounded-lg p-4 space-y-4">
             <BaseInput 
                :model-value="reminderState.data.name" 
                @update:model-value="updateReminder('name', $event)"
                label="Task Subject"
                placeholder="e.g. Renew Contract"
             />
             
             <div class="flex gap-4">
                 <div class="flex-1">
                     <BaseInput 
                        :model-value="reminderState.data.dueDate" 
                        @update:model-value="updateReminder('dueDate', $event)"
                        label="Due Date"
                        type="date"
                     />
                 </div>
                 <div class="flex-1">
                    <label class="text-sm font-medium text-foreground block mb-1.5">Assignee</label>
                    <select 
                        :value="reminderState.data.assigneeIdentifier"
                        @change="updateReminder('assigneeIdentifier', ($event.target as HTMLSelectElement).value)"
                        class="flex h-9 w-full rounded-md border border-input bg-background px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    >
                        <option value="">-- Unassigned --</option>
                        <option v-for="u in users" :key="u.uuid" :value="u.uuid">{{ u.firstname || u.name }}</option>
                    </select>
                 </div>
             </div>
        </div>
      </div>

      <!-- Document ID (UUID) -->
      <div v-if="modelValue.uuid" class="mt-1 pt-4 border-t border-border flex items-center justify-between text-[10px] text-muted-foreground font-mono select-all">
          <span class="uppercase tracking-wider font-bold opacity-70">Document ID (UUID)</span>
          <span>{{ modelValue.uuid }}</span>
      </div>
  </div>
</template>