<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { AttributeTypeService } from '@/services/AttributeTypeService'
import { Check, X } from 'lucide-vue-next'
import { getAttributeInputComponent } from '@/utils/attributeRegistry'

const props = defineProps<{
  initialData: { typeKey: string; value: any }
}>()

const emit = defineEmits(['save', 'cancel'])

const { data: attributeTypes } = useQuery({
  queryKey: ['attribute-types'],
  queryFn: AttributeTypeService.getAll,
  staleTime: 5 * 60 * 1000
})

const value = ref<any>('')

// Resolve definition
const def = computed(() => {
    if (!attributeTypes.value) return null
    // Cast to any because 'key' might be missing in generated types
    return attributeTypes.value.find(t => (t as any).key === props.initialData.typeKey)
})

// GENERIC RESOLVER
const inputComponent = computed(() => getAttributeInputComponent(def.value?.dataType))

onMounted(() => {
    value.value = props.initialData.value
})

const onSave = () => {
    emit('save', {
        typeKey: props.initialData.typeKey,
        value: value.value
    })
}
</script>

<template>
  <div class="bg-gray-100 border border-gray-300 rounded-lg p-3 mb-2 animate-in fade-in zoom-in-95 duration-200 shadow-sm">
     <div class="flex flex-col gap-2">
         <!-- Header -->
         <div class="flex justify-between items-center mb-1">
             <div class="text-[11px] font-bold text-gray-700 uppercase tracking-wide truncate pr-2 flex items-center gap-2">
                 <div class="w-1.5 h-1.5 rounded-full bg-green-500"></div>
                 {{ def?.name || initialData.typeKey }}
             </div>
             <div class="text-[10px] text-gray-400 font-mono bg-white px-1.5 py-0.5 rounded border border-gray-200">
                {{ def?.dataType }}
             </div>
         </div>

         <!-- Inline Input Group -->
         <div class="flex items-stretch gap-2">
             <div class="flex-1">
                 <!-- Dynamic Component -->
                 <component 
                      :is="inputComponent" 
                      v-model="value" 
                      :unit="def?.unit" 
                      :autofocus="true"
                      @save="onSave"
                 />
             </div>

             <!-- Action Buttons -->
             <button 
                @click="onSave" 
                class="w-9 h-9 flex items-center justify-center rounded-md bg-green-600 text-white hover:bg-green-700 shadow-sm transition-colors"
                title="Update"
             >
                <Check :size="18" stroke-width="3" />
             </button>

             <button 
                @click="$emit('cancel')" 
                class="w-9 h-9 flex items-center justify-center rounded-md bg-white border border-gray-300 text-gray-500 hover:text-red-600 hover:bg-red-50 hover:border-red-200 shadow-sm transition-all"
                title="Cancel"
             >
                <X :size="18" stroke-width="2.5" />
             </button>
         </div>
     </div>
  </div>
</template>