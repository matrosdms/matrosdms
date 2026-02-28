<script setup>
import { FlexRender } from '@tanstack/vue-table'
import { ArrowUp, ArrowDown, ArrowUpDown } from 'lucide-vue-next'

defineProps({
  headerGroups: { type: Array, required: true }
})
</script>

<template>
  <div class="sticky top-0 z-10 bg-muted/80 backdrop-blur-sm border-b border-border text-muted-foreground font-semibold shadow-sm select-none transition-colors">
    <div v-for="headerGroup in headerGroups" :key="headerGroup.id" class="flex w-full">
      <div 
        v-for="header in headerGroup.headers" 
        :key="header.id" 
        class="relative px-3 py-2 text-left overflow-hidden whitespace-nowrap flex items-center gap-2 group hover:bg-muted transition-colors"
        :class="[
            {'pl-[calc(0.75rem+4px)]': header.index === 0},
            header.column.getCanSort() ? 'cursor-pointer hover:text-foreground' : 'cursor-default'
        ]"
        :style="{ width: header.getSize() + 'px' }"
        @click="header.column.getToggleSortingHandler()?.($event)"
      >
        <FlexRender :render="header.column.columnDef.header" :props="header.getContext()" />
        
        <!-- Sort Icon -->
        <span v-if="header.column.getCanSort()" class="ml-1 opacity-0 group-hover:opacity-100 transition-opacity flex items-center" :class="{ 'opacity-100 text-primary': header.column.getIsSorted() }">
            <ArrowUp v-if="header.column.getIsSorted() === 'asc'" :size="12" />
            <ArrowDown v-else-if="header.column.getIsSorted() === 'desc'" :size="12" />
            <ArrowUpDown v-else :size="12" class="opacity-50" />
        </span>
        
        <div 
          @mousedown.stop="header.getResizeHandler()($event)" 
          @touchstart.stop="header.getResizeHandler()($event)" 
          @click.stop 
          class="resizer" 
          :class="{ 'is-resizing': header.column.getIsResizing() }"
        ></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.resizer { position: absolute; right: 0; top: 0; height: 100%; width: 4px; background: transparent; cursor: col-resize; user-select: none; touch-action: none; z-index: 20; }
.resizer:hover { background: rgba(0, 0, 0, 0.1); }
.resizer.is-resizing { background: #3b82f6; opacity: 1; }
</style>