<script setup lang="ts">
import { ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { Tag, Trash2, Plus, Check, X, AlertCircle } from 'lucide-vue-next'
import { getAttributeInputComponent } from '@/utils/attributeRegistry'
import BaseSelect from '@/components/ui/BaseSelect.vue'

const props = defineProps<{
  modelValue: any[]
}>()

const emit = defineEmits(['update:modelValue'])

const isAdding = ref(false)
const selectedDefId = ref('')
const newVal = ref<any>('')

const { data: attributeTypes } = useQuery({
  queryKey: ['attribute-types'],
  queryFn: AttributeTypeService.getAll,
  staleTime: 5 * 60 * 1000
})

const getDef = (attr: any) => {
    if (!attributeTypes.value) return null
    if (attr.definitionId) return attributeTypes.value.find(t => t.uuid === attr.definitionId)
    return attributeTypes.value.find(t => (t as any).key === attr.typeKey)
}

const selectedDef = computed(() => attributeTypes.value?.find(t => t.uuid === selectedDefId.value))

const startAdd = () => {
    isAdding.value = true
    selectedDefId.value = ''
    newVal.value = ''
}

const cancelAdd = () => {
    isAdding.value = false
    selectedDefId.value = ''
    newVal.value = ''
}

const confirmAdd = () => {
    if (!selectedDef.value) return
    const def = selectedDef.value as any
    
    const newAttrs = [...props.modelValue, {
        typeKey: def.key,
        definitionId: def.uuid,
        value: newVal.value,
        name: def.name
    }]
    
    emit('update:modelValue', newAttrs)
    cancelAdd()
}

const removeAttr = (index: number) => {
    const arr = [...props.modelValue]
    arr.splice(index, 1)
    emit('update:modelValue', arr)
}

const updateValue = (index: number, val: any) => {
    const arr = [...props.modelValue]
    arr[index] = { ...arr[index], value: val }
    emit('update:modelValue', arr)
}
</script>

<template>
  <div class="mt-2 pt-2 border-t border-border">
    <div class="flex items-center justify-between mb-2">
        <label class="text-xs font-bold text-muted-foreground uppercase tracking-wider flex items-center gap-1.5">
            <Tag :size="14" class="text-blue-500" /> Custom Attributes
        </label>
        <button 
            v-if="!isAdding"
            @click="startAdd" 
            class="text-[10px] text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/30 hover:bg-blue-100 dark:hover:bg-blue-900/50 border border-blue-200 dark:border-blue-800 px-2 py-0.5 rounded flex items-center gap-1 transition-colors"
        >
            <Plus :size="12" /> Add
        </button>
    </div>

    <div class="space-y-1">
        <div v-for="(attr, index) in modelValue" :key="index" class="group flex items-center gap-3 p-1.5 rounded hover:bg-muted/50 border border-transparent hover:border-border transition-colors">
            
            <div class="w-1/3 min-w-[100px] max-w-[160px] shrink-0 flex flex-col justify-center overflow-hidden">
                <div class="flex items-center gap-2">
                    <div class="w-1.5 h-1.5 rounded-full bg-blue-400 shrink-0"></div>
                    <span class="text-xs font-medium text-foreground truncate" :title="getDef(attr)?.name || attr.typeKey">
                        {{ getDef(attr)?.name || attr.name || attr.typeKey }}
                    </span>
                    <div v-if="!getDef(attr) && !attr.definitionId" class="text-destructive" title="Invalid Definition ID">
                        <AlertCircle :size="12" />
                    </div>
                </div>
            </div>
            
            <div class="flex-1 min-w-0">
                 <component 
                    :is="getAttributeInputComponent(getDef(attr)?.dataType)"
                    :model-value="attr.value"
                    @update:model-value="(val: any) => updateValue(index, val)"
                    :unit="getDef(attr)?.unit"
                    class="h-7 text-xs"
                 />
            </div>
            
            <button @click="removeAttr(index)" class="opacity-0 group-hover:opacity-100 p-1.5 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded transition-all" title="Remove">
                <Trash2 :size="14" />
            </button>
        </div>

        <div v-if="modelValue.length === 0 && !isAdding" class="text-xs text-muted-foreground italic text-center py-2 opacity-60">
            No attributes defined.
        </div>
    </div>

    <div v-if="isAdding" class="mt-3 p-2 bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800 rounded-md shadow-sm animate-in fade-in slide-in-from-top-1">
         <div class="flex gap-2 items-center">
             <div class="flex-1">
                 <!-- Replaced native Select with BaseSelect for consistency -->
                 <BaseSelect 
                    v-model="selectedDefId" 
                    placeholder="Select Type..." 
                    class="h-8 text-xs"
                 >
                     <option 
                        v-for="t in attributeTypes" 
                        :key="t.uuid" 
                        :value="t.uuid" 
                        :disabled="modelValue.some(a => a.definitionId === t.uuid)"
                     >
                        {{ t.name }} {{ modelValue.some(a => a.definitionId === t.uuid) ? '(Added)' : '' }}
                     </option>
                 </BaseSelect>
             </div>
             
             <div v-if="selectedDefId && selectedDef" class="flex-1">
                 <component 
                    :is="getAttributeInputComponent(selectedDef.dataType)"
                    v-model="newVal"
                    :unit="selectedDef.unit"
                    :autofocus="true"
                    @save="confirmAdd"
                    class="h-8 text-xs"
                 />
             </div>
             
             <div class="flex gap-1">
                <button v-if="selectedDefId" @click="confirmAdd" class="p-1.5 bg-green-600 text-white rounded hover:bg-green-700 shadow-sm" title="Confirm"><Check :size="14" /></button>
                <button @click="cancelAdd" class="p-1.5 bg-background border border-border text-muted-foreground rounded hover:text-destructive hover:bg-destructive/10" title="Cancel"><X :size="14" /></button>
             </div>
         </div>
    </div>
  </div>
</template>