<script setup lang="ts">
import { computed } from 'vue'
import { useDmsStore } from '@/stores/dms'
import { useConfigStore } from '@/stores/config'
import { Eraser, PlusCircle } from 'lucide-vue-next'
import { push } from 'notivue'
import { ERootCategoryList, type ERootCategoryType } from '@/enums'
import AppTag from '@/components/ui/AppTag.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

// ============================================================================
// STORES
// ============================================================================

const dms = useDmsStore()
const config = useConfigStore()

// ============================================================================
// CONSTANTS
// ============================================================================

const DIMENSION_STYLES = {
  WHO: {
    container: 'border-blue-200 bg-blue-50/30 dark:bg-blue-900/10',
    containerActive: 'ring-blue-100 dark:ring-blue-800 border-blue-400',
    containerInactive: 'border-blue-100 dark:border-blue-800',
    label: 'text-blue-500 dark:text-blue-400',
  },
  WHAT: {
    container: 'border-green-200 bg-green-50/30 dark:bg-green-900/10',
    containerActive: 'ring-green-100 dark:ring-green-800 border-green-400',
    containerInactive: 'border-green-100 dark:border-green-800',
    label: 'text-green-600 dark:text-green-400',
  },
  WHERE: {
    container: 'border-orange-200 bg-orange-50/30 dark:bg-orange-900/10',
    containerActive: 'ring-orange-100 dark:ring-orange-800 border-orange-400',
    containerInactive: 'border-orange-100 dark:border-orange-800',
    label: 'text-orange-600 dark:text-orange-400',
  },
} as const

const FILTER_BOX_CONFIG = {
  width: 180,
  minHeight: 36,
  maxHeight: 60,
} as const

// ============================================================================
// COMPUTED
// ============================================================================

const visibleDimensions = computed(() => config.contextDimensions)

const hasAnyFilter = computed(() => 
  ERootCategoryList.some(cat => dms.filters[cat]?.length > 0)
)

// ============================================================================
// STYLING HELPERS
// ============================================================================

const getContainerClass = (key: ERootCategoryType): string => {
  const isActive = dms.activeContext === key
  const baseClass = isActive 
    ? 'ring-2 ring-offset-0' 
    : 'hover:border-opacity-100 hover:shadow-sm'
  
  const styles = DIMENSION_STYLES[key as keyof typeof DIMENSION_STYLES]
  
  if (!styles) {
    const defaultBorder = isActive 
      ? 'ring-ring border-primary' 
      : 'border-border'
    return `${baseClass} border-border bg-background ${defaultBorder}`
  }
  
  const borderClass = isActive 
    ? styles.containerActive 
    : styles.containerInactive
  
  return `${baseClass} ${styles.container} ${borderClass}`
}

const getLabelClass = (key: ERootCategoryType): string => {
  const styles = DIMENSION_STYLES[key as keyof typeof DIMENSION_STYLES]
  return styles?.label || 'text-muted-foreground'
}

// ============================================================================
// EVENT HANDLERS
// ============================================================================

interface DroppedItem {
  id: string
  label: string
  rootType?: string
}

const validateDrop = (item: DroppedItem, targetKey: ERootCategoryType): boolean => {
  if (!item.rootType) return true
  
  if (item.rootType !== targetKey) {
    push.error({
      title: "Invalid Drop",
      message: `Cannot drop '${item.label}' (${item.rootType}) into ${targetKey}.`
    })
    return false
  }
  
  return true
}

const handleDrop = (event: DragEvent, targetKey: ERootCategoryType) => {
  const rawData = event.dataTransfer?.getData('application/json')
  if (!rawData) return

  try {
    const item: DroppedItem = JSON.parse(rawData)
    
    if (!validateDrop(item, targetKey)) return
    
    dms.addFilter(targetKey, { id: item.id, label: item.label })
    dms.setActiveContext(targetKey)
  } catch (error) {
    console.error('Failed to parse dropped data:', error)
  }
}

const handleKeyDown = (event: KeyboardEvent, key: ERootCategoryType) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    dms.setActiveContext(key)
  }
}

const handleClearAll = () => {
  dms.clearAllFilters()
}

const handleSaveContext = () => {
  dms.startContextCreation()
}

// ============================================================================
// COMPUTED STYLES
// ============================================================================

const filterBoxStyle = computed(() => ({
  width: `${FILTER_BOX_CONFIG.width}px`,
  minHeight: `${FILTER_BOX_CONFIG.minHeight}px`,
  maxHeight: `${FILTER_BOX_CONFIG.maxHeight}px`,
}))
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
        @click="handleClearAll"
      >
        <Eraser :size="16" />
      </BaseButton>
      
      <!-- Spacer when no filters -->
      <div v-else class="w-[28px]" />
    </div>

    <!-- Filter Dimension Boxes -->
    <div class="flex gap-2">
      <div
        v-for="key in visibleDimensions"
        :key="key"
        class="flex flex-col group flex-shrink-0"
        :style="{ width: `${FILTER_BOX_CONFIG.width}px` }"
      >
        <!-- Dimension Label -->
        <span
          class="text-[9px] font-extrabold uppercase mb-1 ml-1 tracking-wider 
                 transition-colors truncate"
          :class="getLabelClass(key)"
        >
          {{ config.getLabel(key) }}
        </span>

        <!-- Filter Drop Zone -->
        <div
          class="w-full border rounded-md cursor-pointer transition-all px-1.5 py-1 
                 flex flex-wrap items-center content-start gap-1.5 overflow-y-auto 
                 focus:outline-none focus:ring-2 focus:ring-primary/50"
          :class="getContainerClass(key)"
          :style="filterBoxStyle"
          tabindex="0"
          role="button"
          :aria-label="`${config.getLabel(key)} filter box`"
          @click="dms.setActiveContext(key)"
          @keydown="handleKeyDown($event, key)"
          @dragover.prevent
          @drop="handleDrop($event, key)"
        >
          <!-- Active Filters -->
          <AppTag
            v-for="tag in dms.filters[key]"
            :key="tag.id"
            :root-id="key"
            :label="tag.label"
            removable
            @remove="dms.removeFilter(key, tag.id)"
          />

          <!-- Empty State -->
          <div
            v-if="!dms.filters[key]?.length"
            class="text-[10px] text-muted-foreground/50 italic w-full 
                   text-center select-none pointer-events-none font-medium"
          >
            Drop here
          </div>
        </div>
      </div>
    </div>

    <!-- Save Context Button -->
    <div class="h-full flex flex-col justify-center pt-5 ml-1 flex-shrink-0">
      <BaseButton
        variant="ghost"
        size="iconSm"
        class="text-primary hover:text-primary-hover hover:bg-primary/10"
        title="Save as Context"
        @click="handleSaveContext"
      >
        <PlusCircle :size="22" stroke-width="2.5" />
      </BaseButton>
    </div>
  </div>
</template>

<style scoped>
/* Custom scrollbar for filter boxes */
.overflow-y-auto::-webkit-scrollbar {
  width: 4px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.2);
  border-radius: 2px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.3);
}
</style>