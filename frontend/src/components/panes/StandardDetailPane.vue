<script setup lang="ts">
import { computed } from 'vue'
import type { AnyEntity } from '@/types/models'
import EntityHeader from '@/components/panels/EntityHeader.vue'
import LinkedTasksPanel from '@/components/panels/LinkedTasksPanel.vue'

const props = defineProps<{
  item: AnyEntity
}>()

defineEmits(['edit', 'delete'])

const uuid = computed(() => props.item?.uuid)
const isEmailItem = computed(() => 'email' in props.item)
const isDataTypeItem = computed(() => 'dataType' in props.item)
const hasDescription = computed(() => !!props.item?.description)
const safeItem = computed(() => props.item as any)

// Extract Attributes safely if available
const attributes = computed(() => {
    if (safeItem.value.attributeList && Array.isArray(safeItem.value.attributeList)) {
        return safeItem.value.attributeList;
    }
    return [];
})
</script>

<template>
  <div v-if="item" class="h-full flex flex-col bg-background animate-in slide-in-from-right-4 duration-300">
      
      <!-- 1. Header Component -->
      <EntityHeader 
        :item="item" 
        @edit="$emit('edit')" 
        @delete="$emit('delete')" 
      />
      
      <!-- 2. Scrollable Body -->
      <div class="p-6 space-y-8 overflow-y-auto flex-1 custom-scrollbar">
          
          <!-- Description Section -->
          <div v-if="hasDescription" class="grid gap-2">
               <label class="text-xs font-bold text-muted-foreground uppercase tracking-wide">Description</label>
               <div class="text-sm text-foreground bg-muted/30 p-3 rounded-lg border leading-relaxed whitespace-pre-wrap">
                   {{ item.description }}
               </div>
          </div>

          <!-- Document Attributes (Generic Agnostic Rendering) -->
          <div v-if="attributes.length > 0" class="grid grid-cols-2 gap-4 p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg border border-gray-100 dark:border-gray-800">
              <div v-for="attr in attributes" :key="attr.uuid">
                   <label class="text-xs font-bold text-muted-foreground uppercase block mb-1 truncate" :title="attr.name">{{ attr.name }}</label>
                   <div class="text-sm font-medium truncate" :title="attr.value?.value || attr.value">
                       {{ attr.value?.value || attr.value || '-' }}
                   </div>
              </div>
          </div>

          <!-- Tasks Section (Only for core items) -->
          <div v-if="!isEmailItem && !isDataTypeItem && uuid"> 
             <LinkedTasksPanel :item-id="uuid" />
          </div>

          <!-- Specific: User Details -->
          <div v-if="isEmailItem" class="grid grid-cols-2 gap-6 p-4 bg-muted/20 rounded-lg border">
              <div>
                  <label class="text-xs font-bold text-muted-foreground uppercase block mb-1">Email</label>
                  <div class="text-sm font-medium">{{ safeItem.email }}</div>
              </div>
              <div>
                  <label class="text-xs font-bold text-muted-foreground uppercase block mb-1">Full Name</label>
                  <div class="text-sm font-medium">{{ safeItem.firstname || '-' }}</div>
              </div>
          </div>

          <!-- Specific: Attribute Details -->
          <div v-if="isDataTypeItem" class="grid grid-cols-2 gap-6 p-4 bg-purple-50/50 dark:bg-purple-900/10 rounded-lg border border-purple-100 dark:border-purple-800">
              <div v-if="safeItem.unit">
                  <label class="text-xs font-bold text-purple-700 dark:text-purple-400 uppercase block mb-1">Unit</label>
                  <div class="text-sm font-bold">{{ safeItem.unit }}</div>
              </div>
              <div v-if="safeItem.pattern">
                  <label class="text-xs font-bold text-purple-700 dark:text-purple-400 uppercase block mb-1">Regex Pattern</label>
                  <code class="text-xs bg-white dark:bg-black px-1.5 py-0.5 rounded border border-purple-200 dark:border-purple-800 font-mono">{{ safeItem.pattern }}</code>
              </div>
          </div>
      </div>
  </div>
  
  <div v-else class="h-full flex items-center justify-center text-muted-foreground italic">
      No item selected
  </div>
</template>