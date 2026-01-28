<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { Check, ChevronDown, X } from 'lucide-vue-next'
import { push } from 'notivue'
import { getAttributeInputComponent, getAttributeInitialValue } from '@/utils/attributeRegistry'

const props = defineProps<{
  existingKeys: string[]
}>()

const emit = defineEmits(['save', 'cancel'])

const { data: attributeTypes, isLoading } = useQuery({
  queryKey: ['attribute-types'],
  queryFn: AttributeTypeService.getAll,
  staleTime: 5 * 60 * 1000
})

const selectedUuid = ref('') 
const value = ref<any>('')

const selectedDef = computed(() => {
    if (!attributeTypes.value || !selectedUuid.value) return null
    return attributeTypes.value.find(t => t.uuid === selectedUuid.value)
})

// 1. GENERIC RESOLVER
const inputComponent = computed(() => getAttributeInputComponent(selectedDef.value?.dataType))

// 2. GENERIC VALUE RESET
watch(selectedUuid, (newId) => {
    if (!newId) return
    value.value = getAttributeInitialValue(selectedDef.value?.dataType)
})

const onSave = () => {
    if (!selectedDef.value) { push.warning('Please select an attribute type'); return; }
    
    // Cast to any because 'key' might be missing in generated types but present at runtime
    const def = selectedDef.value as any;

    emit('save', {
        typeKey: def.key,
        definitionId: def.uuid,
        name: def.name,
        value: value.value
    })
}
</script>

<template>
  <div class="bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-800 rounded-lg p-3 shadow-sm mb-2 animate-in fade-in slide-in-from-top-1 transition-colors">
      
      <!-- Select Type -->
      <div v-if="!selectedDef" class="flex gap-2">
         <div class="relative flex-1">
             <select 
                v-model="selectedUuid" 
                class="w-full appearance-none bg-white dark:bg-gray-800 border border-blue-300 dark:border-blue-700 text-gray-800 dark:text-gray-200 text-sm rounded-md py-2 px-3 pr-8 leading-tight focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 dark:focus:ring-blue-900 shadow-sm transition-all cursor-pointer"
             >
                 <option value="" disabled selected>-- Select Attribute Type --</option>
                 <template v-if="!isLoading && attributeTypes">
                     <option 
                        v-for="t in attributeTypes" 
                        :key="t.uuid" 
                        :value="t.uuid"
                        :disabled="existingKeys.includes((t as any).key || '')"
                     >
                        {{ t.name }}
                     </option>
                 </template>
             </select>
             <div class="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-500">
                <ChevronDown :size="16" />
             </div>
         </div>
         
         <button @click="$emit('cancel')" class="p-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/30 rounded-md transition-colors shadow-sm" title="Cancel">
            <X :size="18" />
         </button>
      </div>

      <!-- Value Input -->
      <div v-else class="animate-in zoom-in-95 duration-200">
           <div class="flex justify-between items-center mb-1">
               <label class="text-[11px] font-bold text-gray-600 dark:text-gray-300 uppercase flex items-center gap-1.5">
                  <div class="w-1.5 h-1.5 rounded-full bg-blue-500"></div>
                  Value for <span class="text-blue-700 dark:text-blue-400">{{ selectedDef.name }}</span>
               </label>
               <span class="text-[9px] text-gray-400 font-mono bg-white dark:bg-gray-800 px-1.5 py-0.5 rounded border border-gray-200 dark:border-gray-700">
                  {{ selectedDef.dataType }}
               </span>
           </div>
           
           <div class="flex items-stretch gap-2">
               
               <div class="flex-1">
                   <!-- Dynamic Component -->
                   <component 
                        :is="inputComponent" 
                        v-model="value" 
                        :unit="selectedDef.unit" 
                        :autofocus="true"
                        @save="onSave"
                   />
               </div>

               <button @click="onSave" class="w-9 h-9 flex items-center justify-center rounded-md bg-green-600 text-white hover:bg-green-700 shadow-sm transition-colors" title="Save">
                  <Check :size="18" stroke-width="3" />
               </button>
               
               <button @click="$emit('cancel')" class="w-9 h-9 flex items-center justify-center rounded-md bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 text-gray-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 hover:border-red-200 shadow-sm transition-all" title="Cancel">
                  <X :size="18" stroke-width="2.5" />
               </button>
           </div>
      </div>
  </div>
</template>