<script setup lang="ts">
import { computed } from 'vue'
import { Calendar, FileText, ArrowDown, Tag, GripVertical, Box, Hash } from 'lucide-vue-next'
import { parseBackendDate } from '@/lib/utils'
import { useDmsStore } from '@/stores/dms'
import { useDragDrop } from '@/composables/useDragDrop'
import { useAdminQueries } from '@/composables/queries/useAdminQueries'

const props = defineProps<{
  items: any[]
}>()

const emit = defineEmits(['select'])
const dms = useDmsStore()
const { startDrag, endDrag } = useDragDrop()
const { useStores } = useAdminQueries()
const { data: stores } = useStores()

const storeMap = computed(() => {
    const map = new Map<string, string>()
    if (stores.value) {
        stores.value.forEach((s: any) => map.set(s.uuid, s.shortname || s.name))
    }
    return map
})

// Helper: Calculate gap between dates
const getGapText = (current: Date, prev: Date) => {
    const diffTime = Math.abs(current.getTime() - prev.getTime())
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) 
    if (diffDays === 0) return 'Same day'
    if (diffDays === 1) return '1 day later'
    if (diffDays < 30) return `${diffDays} days later`
    if (diffDays < 365) return `${Math.floor(diffDays / 30)} months later`
    return `${Math.floor(diffDays / 365)} years later`
}

const timelineItems = computed(() => {
  // Sort oldest first
  const sorted = [...props.items].sort((a, b) => {
    const da = parseBackendDate(a.issueDate) || new Date(0)
    const db = parseBackendDate(b.issueDate) || new Date(0)
    return da.getTime() - db.getTime()
  })

  return sorted.map((item, index) => {
    const date = parseBackendDate(item.issueDate)
    let gapText = null
    
    // Calculate gap from previous item
    if (index > 0 && date) {
      const prevDate = parseBackendDate(sorted[index - 1].issueDate)
      if (prevDate) gapText = getGapText(date, prevDate)
    }
    
    // Resolve store name
    const storeName = item.storeIdentifier ? storeMap.value.get(item.storeIdentifier) : null
    
    // Process Attributes (extract first 3 key/value pairs for display)
    const topAttributes = item.attributeList ? item.attributeList.slice(0, 3).map((attr: any) => ({
        name: attr.name,
        value: attr.value?.value || attr.value
    })) : []

    return { ...item, _dateObj: date, _gap: gapText, _storeName: storeName, _topAttributes: topAttributes }
  })
})

const onItemClick = (item: any) => {
    // Optimistic UI update
    dms.setSelectedItem(item)
    // Delegate action to parent
    emit('select', item)
}

const handleDragStart = (event: DragEvent, item: any) => {
  startDrag(event, 'dms-item', item)
}
</script>

<template>
  <div class="p-4 bg-gray-50 dark:bg-gray-900 min-h-full transition-colors duration-300">
    <div class="w-full relative">
      <!-- Continuous Line - starts at first dot center -->
      <div class="absolute left-[15px] top-[22px] bottom-0 w-0.5 bg-gray-200 dark:bg-gray-800"></div>

      <div v-for="(item, index) in timelineItems" :key="item.uuid" class="relative pl-10 pb-6 group">
        
        <!-- Dot -->
        <div 
            class="absolute left-0 top-1.5 w-8 h-8 rounded-full border-4 flex items-center justify-center z-10 transition-all shadow-sm cursor-pointer bg-white dark:bg-gray-800"
            :class="dms.selectedItem?.uuid === item.uuid ? 'border-blue-200 dark:border-blue-900' : 'border-gray-50 dark:border-gray-900'"
            @click="onItemClick(item)"
        >
            <div class="w-2.5 h-2.5 rounded-full" :class="dms.selectedItem?.uuid === item.uuid ? 'bg-blue-600' : 'bg-blue-400 dark:bg-blue-600'"></div>
        </div>

        <!-- Gap Label -->
        <div v-if="item._gap" class="absolute left-[-6px] -top-4 flex items-center gap-1 bg-gray-100 dark:bg-gray-800 text-[9px] text-gray-500 dark:text-gray-400 font-bold px-2 py-0.5 rounded-full border border-white dark:border-gray-700 shadow-sm z-20 whitespace-nowrap">
            <ArrowDown :size="10" /> {{ item._gap }}
        </div>

        <!-- Card -->
        <div 
            @click="onItemClick(item)"
            draggable="true"
            @dragstart="handleDragStart($event, item)"
            @dragend="endDrag"
            class="bg-white dark:bg-gray-800 border rounded-lg p-3 shadow-sm hover:shadow-md cursor-pointer transition-all relative top-0 hover:-top-0.5 group-hover:border-blue-300 dark:group-hover:border-blue-700"
            :class="dms.selectedItem?.uuid === item.uuid ? 'ring-1 ring-blue-500 border-blue-500 dark:border-blue-500' : 'border-gray-200 dark:border-gray-700'"
        >
            <!-- Top Row: Date & Kind -->
            <div class="flex justify-between items-start mb-1.5">
                <div class="flex items-center gap-1.5">
                    <GripVertical :size="12" class="text-gray-300 dark:text-gray-600 cursor-grab active:cursor-grabbing" />
                    <span class="text-[10px] font-bold text-gray-600 dark:text-gray-300 flex items-center gap-1.5 bg-gray-50 dark:bg-gray-700 px-1.5 py-0.5 rounded">
                        <Calendar :size="10" />
                        {{ item._dateObj ? item._dateObj.toLocaleDateString() : 'No Date' }}
                    </span>
                </div>
                
                <span v-if="item.kindList?.length" class="text-[9px] bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300 px-1.5 py-0.5 rounded border border-purple-100 dark:border-purple-800 font-bold uppercase tracking-wide flex items-center gap-1 max-w-[100px] truncate">
                    <Tag :size="9" /> {{ item.kindList[0].name }}
                </span>
            </div>

            <!-- Main Content: Name -->
            <h3 class="text-xs font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2 mb-1 leading-tight">
                <FileText :size="14" class="text-blue-500 dark:text-blue-400 shrink-0" />
                <span class="truncate">{{ item.name }}</span>
            </h3>
            
            <p v-if="item.description" class="text-[10px] text-gray-500 dark:text-gray-400 line-clamp-2 pl-5 mt-1 leading-snug">
                {{ item.description }}
            </p>

            <!-- Metadata Row (Store, Attributes) -->
            <div v-if="item._storeName || item.storeItemNumber || item._topAttributes.length > 0" class="mt-2 pt-2 border-t border-gray-100 dark:border-gray-700 flex flex-wrap gap-2 pl-5">
                
                <!-- Store Pill -->
                <div v-if="item._storeName" class="flex items-center gap-1 text-[9px] font-medium bg-orange-50 dark:bg-orange-900/10 text-orange-700 dark:text-orange-300 px-1.5 py-0.5 rounded border border-orange-100 dark:border-orange-800">
                    <Box :size="10" />
                    {{ item._storeName }} <span v-if="item.storeItemNumber">#{{ item.storeItemNumber }}</span>
                </div>

                <!-- Attributes Pills -->
                <div v-for="attr in item._topAttributes" :key="attr.name" class="flex items-center gap-1 text-[9px] text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700/50 px-1.5 py-0.5 rounded border border-gray-200 dark:border-gray-700">
                    <Hash :size="10" class="opacity-50" />
                    <span class="opacity-70">{{ attr.name }}:</span> 
                    <span class="font-bold">{{ attr.value }}</span>
                </div>
            </div>
        </div>
      </div>
      
      <!-- End Cap -->
      <div class="relative pl-10">
          <div class="absolute left-[11px] top-0 w-2 h-2 bg-gray-300 dark:bg-gray-700 rounded-full"></div>
          <span class="text-[10px] text-gray-400 dark:text-gray-500 italic">Start of timeline</span>
      </div>
    </div>
  </div>
</template>