<script setup lang="ts">
import { ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { ChevronRight, Tag, Trash2, ChevronDown, Save, X } from 'lucide-vue-next'

const props = defineProps<{
  modelValue: any[]
}>()

const emit = defineEmits(['update:modelValue'])

// Fetch Definitions
const { data: attributeTypes } = useQuery({
  queryKey: ['attribute-types'],
  queryFn: AttributeTypeService.getAll,
  staleTime: 5 * 60 * 1000
})

// State
const isOpen = ref(true) // Default open
const selectedTypeId = ref('')
const newAttrValue = ref('')

// Helpers
const getAttrDef = (key: string) => attributeTypes.value?.find(t => (t as any).key === key)
const currentDef = computed(() => getAttrDef(selectedTypeId.value))

// Actions
const onSaveAttribute = () => {
    if (!selectedTypeId.value) return
    
    // Create new array to trigger reactivity
    const newArray = [...props.modelValue, {
        typeKey: selectedTypeId.value,
        value: newAttrValue.value
    }]
    
    emit('update:modelValue', newArray)
    
    // Reset inputs
    cancelAdd()
}

const cancelAdd = () => {
    selectedTypeId.value = ''
    newAttrValue.value = ''
}

const removeAttribute = (index: number) => {
    const newArray = [...props.modelValue]
    newArray.splice(index, 1)
    emit('update:modelValue', newArray)
}
</script>

<template>
  <div class="border border-gray-200 rounded-md overflow-hidden bg-white mt-2">
    <!-- Header -->
    <div 
        @click="isOpen = !isOpen" 
        class="flex justify-between items-center p-2.5 bg-gray-50 hover:bg-gray-100 cursor-pointer select-none transition-colors border-b border-gray-200"
        :class="{ 'border-b-0': !isOpen }"
    >
        <h3 class="text-xs font-bold text-gray-600 uppercase flex items-center gap-2">
            <ChevronRight 
                :size="16" 
                class="transition-transform duration-200 text-gray-400" 
                :class="{ 'rotate-90': isOpen }" 
            />
            <Tag :size="14" class="text-blue-500" />
            Custom Attributes
        </h3>
        <span v-if="modelValue.length" class="bg-blue-100 text-blue-700 text-[10px] font-bold px-2 py-0.5 rounded-full">
            {{ modelValue.length }}
        </span>
    </div>
    
    <!-- Body -->
    <div v-show="isOpen" class="p-3 bg-gray-50/50 space-y-4">
        
        <!-- List -->
        <div v-if="modelValue.length > 0" class="space-y-2">
            <div v-for="(attr, index) in modelValue" :key="index" class="flex items-center gap-0 border border-gray-300 rounded-md bg-white shadow-sm overflow-hidden h-[32px]">
                <div class="w-32 bg-gray-50 border-r border-gray-200 px-3 flex items-center h-full text-xs font-bold text-gray-700 uppercase tracking-wide truncate" :title="attr.typeKey">
                    {{ getAttrDef(attr.typeKey)?.name || attr.typeKey }}
                </div>
                
                <div class="flex-1 h-full relative flex items-center">
                    <template v-if="getAttrDef(attr.typeKey)?.dataType === 'BOOLEAN'">
                        <div class="flex items-center h-full px-2">
                           <input type="checkbox" v-model="attr.value" class="h-4 w-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500" />
                        </div>
                    </template>
                    <template v-else-if="['NUMBER', 'CURRENCY'].includes(getAttrDef(attr.typeKey)?.dataType || '')">
                        <input type="number" v-model="attr.value" class="w-full h-full px-2 text-sm border-0 focus:ring-0 bg-transparent text-right pr-8" step="any" />
                        <span v-if="getAttrDef(attr.typeKey)?.unit" class="absolute right-2 text-xs text-gray-400 pointer-events-none font-bold">{{ getAttrDef(attr.typeKey)?.unit }}</span>
                    </template>
                    <template v-else-if="getAttrDef(attr.typeKey)?.dataType === 'DATE'">
                        <input type="date" v-model="attr.value" class="w-full h-full px-2 text-sm border-0 focus:ring-0 bg-transparent" />
                    </template>
                    <template v-else>
                        <input type="text" v-model="attr.value" class="w-full h-full px-2 text-sm border-0 focus:ring-0 bg-transparent" />
                    </template>
                </div>
                
                <button @click="removeAttribute(index)" class="w-8 h-full flex items-center justify-center border-l border-gray-200 hover:bg-red-50 text-gray-400 hover:text-red-600 transition-colors" title="Remove">
                    <Trash2 :size="14" />
                </button>
            </div>
        </div>
        <div v-else class="text-center py-2 text-xs text-gray-400 italic">No attributes assigned.</div>

        <!-- Add Attribute UI -->
        <div class="space-y-3">
            <!-- 1. Select Type -->
            <div class="relative">
                <select v-model="selectedTypeId" @change="newAttrValue = ''" class="w-full appearance-none bg-white border border-gray-300 text-gray-700 text-sm rounded-md py-2 px-3 pr-8 leading-tight focus:outline-none focus:border-blue-500 shadow-sm transition-colors cursor-pointer">
                    <option value="" disabled>+ Add Attribute...</option>
                    <option 
                        v-for="type in attributeTypes" 
                        :key="type.uuid" 
                        :value="(type as any).key" 
                        :disabled="modelValue.some(a => a.typeKey === (type as any).key)"
                    >
                        {{ type.name }}
                    </option>
                </select>
                <div class="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-500">
                    <ChevronDown :size="14" />
                </div>
            </div>

            <!-- 2. Subform for Value (Visible when type selected) -->
            <div v-if="selectedTypeId" class="bg-blue-50 border border-blue-200 rounded-md p-3 animate-in fade-in slide-in-from-top-2">
                <div class="flex flex-col gap-2">
                    <label class="text-xs font-bold text-blue-800 uppercase">
                        Value for {{ currentDef?.name }}
                    </label>
                    
                    <div class="relative bg-white rounded-md border border-blue-300 shadow-sm">
                        <template v-if="currentDef?.dataType === 'BOOLEAN'">
                            <div class="flex items-center h-9 px-3">
                                <label class="flex items-center cursor-pointer gap-2 w-full">
                                    <input type="checkbox" v-model="newAttrValue" class="h-4 w-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500" />
                                    <span class="text-sm text-gray-700 select-none">{{ newAttrValue ? 'Yes / True' : 'No / False' }}</span>
                                </label>
                            </div>
                        </template>
                        <template v-else-if="['NUMBER', 'CURRENCY'].includes(currentDef?.dataType || '')">
                            <input 
                                type="number" 
                                v-model="newAttrValue" 
                                class="w-full h-9 px-3 text-sm rounded-md outline-none" 
                                step="any" 
                                placeholder="0.00" 
                                autofocus
                                @keydown.enter.prevent="onSaveAttribute"
                            />
                            <span v-if="currentDef?.unit" class="absolute right-3 top-2.5 text-xs text-gray-400 font-bold pointer-events-none">{{ currentDef?.unit }}</span>
                        </template>
                        <template v-else-if="currentDef?.dataType === 'DATE'">
                            <input 
                                type="date" 
                                v-model="newAttrValue" 
                                class="w-full h-9 px-3 text-sm rounded-md outline-none" 
                                autofocus
                                @keydown.enter.prevent="onSaveAttribute" 
                            />
                        </template>
                        <template v-else>
                            <input 
                                type="text" 
                                v-model="newAttrValue" 
                                class="w-full h-9 px-3 text-sm rounded-md outline-none" 
                                placeholder="Enter text..." 
                                autofocus
                                @keydown.enter.prevent="onSaveAttribute"
                            />
                        </template>
                    </div>

                    <div class="flex justify-end gap-2 mt-1">
                        <button @click="cancelAdd" class="px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-white hover:text-red-600 rounded border border-transparent hover:border-gray-200 transition-colors flex items-center gap-1">
                            <X :size="14" /> Cancel
                        </button>
                        <button @click="onSaveAttribute" class="px-3 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 rounded shadow-sm transition-colors flex items-center gap-1">
                            <Save :size="14" /> Save Attribute
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>