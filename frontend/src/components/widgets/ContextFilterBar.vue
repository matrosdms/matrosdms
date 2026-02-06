<script setup lang="ts">
import { computed } from 'vue'
import { useDmsStore } from '@/stores/dms'
import { useConfigStore } from '@/stores/config'
import { Eraser, PlusCircle } from 'lucide-vue-next'
import { ERootCategoryList, type ERootCategoryType } from '@/enums'
import BaseButton from '@/components/ui/BaseButton.vue'
import DimensionDropZone from '@/components/widgets/DimensionDropZone.vue'

const dms = useDmsStore()
const config = useConfigStore()

const visibleDimensions = computed(() => config.contextDimensions)
const hasAnyFilter = computed(() => ERootCategoryList.some(cat => dms.filters[cat]?.length > 0))

const handleItemDropped = (item: any, key: ERootCategoryType) => {
    // Build Set for O(1) descendant matching
    const transitiveChildrenAndSelf = item.transitiveIds 
      ? new Set<string>(item.transitiveIds) 
      : new Set<string>([item.id])
    
    dms.addFilter(key, { 
      id: item.id, 
      label: item.label,
      transitiveChildrenAndSelf
    })
    dms.setActiveContext(key)
}
</script>

<template>
  <div 
    class="flex items-start gap-3 p-1.5 bg-muted/20 border border-border 
           rounded-lg shadow-sm overflow-hidden overflow-x-auto 
           transition-colors w-fit max-w-full"
  >
    <!-- Clear All Button -->
    <div class="flex flex-col justify-center pt-5 flex-shrink-0">
      <BaseButton
        v-if="hasAnyFilter"
        variant="ghost"
        size="iconSm"
        class="text-muted-foreground hover:text-destructive hover:bg-destructive/10"
        title="Clear All Filters"
        @click="dms.clearAllFilters()"
      >
        <Eraser :size="16" />
      </BaseButton>
      <div v-else class="w-[28px]" />
    </div>

    <!-- Filter Dimension Boxes -->
    <div class="flex gap-2">
      <div
        v-for="key in visibleDimensions"
        :key="key"
        class="w-[180px] flex-shrink-0"
      >
        <!-- Reusable Component -->
        <DimensionDropZone 
            :dimension="key"
            :label="config.getLabel(key)"
            :tags="dms.filters[key]"
            :is-active="dms.activeContext === key"
            @activate="dms.setActiveContext(key)"
            @remove-tag="dms.removeFilter(key, $event)"
            @item-dropped="handleItemDropped($event, key)"
            min-height="min-h-[36px] max-h-[60px] overflow-y-auto custom-scrollbar"
        />
      </div>
    </div>

    <!-- Save Context Button -->
    <div class="h-full flex flex-col justify-center pt-5 ml-1 flex-shrink-0">
      <BaseButton
        variant="ghost"
        size="iconSm"
        class="text-primary hover:text-primary-hover hover:bg-primary/10"
        title="Save as Context"
        @click="dms.startContextCreation()"
      >
        <PlusCircle :size="22" stroke-width="2.5" />
      </BaseButton>
    </div>
  </div>
</template>