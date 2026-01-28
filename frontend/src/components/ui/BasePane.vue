<script setup lang="ts">
import { computed } from 'vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'

// ============================================================================
// TYPES
// ============================================================================

interface BasePaneProps {
  title: string
  count?: number
  total?: number
  headerHeight?: number
}

interface BasePaneSlots {
  default?: (props: {}) => any
  actions?: (props: {}) => any
  filter?: (props: {}) => any
}

// ============================================================================
// PROPS & SLOTS
// ============================================================================

const props = withDefaults(defineProps<BasePaneProps>(), {
  headerHeight: 40
})

defineSlots<BasePaneSlots>()

// ============================================================================
// COMPUTED
// ============================================================================

const badgeText = computed(() => {
  if (props.count === undefined) return ''
  if (props.total === undefined) return props.count.toString()
  return `${props.count} / ${props.total}`
})

const showBadge = computed(() => props.count !== undefined)

const headerStyle = computed(() => ({
  height: `${props.headerHeight}px`
}))
</script>

<template>
  <div class="h-full flex flex-col bg-background border-r border-border transition-colors">
    <!-- Header Section -->
    <div class="shrink-0">
      <!-- Title & Actions Bar -->
      <div 
        class="px-3 py-2 flex justify-between items-center border-b border-border 
               bg-muted/30 select-none transition-colors"
        :style="headerStyle"
      >
        <!-- Left: Title & Count -->
        <div class="flex items-center gap-2 overflow-hidden min-w-0">
          <h2 
            class="text-xs font-bold text-foreground uppercase tracking-wide 
                   truncate transition-colors" 
            :title="title"
          >
            {{ title }}
          </h2>
          
          <BaseBadge 
            v-if="showBadge"
            variant="secondary" 
            class="font-mono text-[10px] px-1.5 h-5 shrink-0"
          >
            {{ badgeText }}
          </BaseBadge>
        </div>

        <!-- Right: Actions -->
        <div 
          v-if="$slots.actions" 
          class="flex items-center gap-1 ml-auto shrink-0"
        >
          <slot name="actions" />
        </div>
      </div>
      
      <!-- Filter Bar -->
      <div 
        v-if="$slots.filter" 
        class="p-2 border-b border-border bg-background transition-colors"
      >
        <slot name="filter" />
      </div>
    </div>
    
    <!-- Content Body -->
    <div class="flex-1 overflow-auto relative custom-scrollbar bg-muted/10 transition-colors">
      <slot />
    </div>
  </div>
</template>

<style scoped>
/* Custom scrollbar styling */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.3);
  border-radius: 3px;
  transition: background 0.2s ease;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.5);
}

/* Firefox scrollbar */
.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: hsl(var(--muted-foreground) / 0.3) transparent;
}
</style>