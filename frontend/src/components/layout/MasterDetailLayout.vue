<script setup lang="ts">
import { ref, watch } from 'vue'
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import { useBreakpoints, breakpointsTailwind } from '@vueuse/core'

const props = defineProps<{
  layout: number[]
  sidebarOpen?: boolean
}>()

const emit = defineEmits(['update:layout', 'toggle-sidebar'])

const breakpoints = useBreakpoints(breakpointsTailwind)
const isMobile = breakpoints.smaller('md')

const onResize = (key: 'sidebar' | 'workspace', panes: { size: number }[]) => {
  emit('update:layout', { key, sizes: panes.map(p => p.size) })
}
</script>

<template>
  <div class="h-full w-full font-sans bg-background transition-colors duration-300">
    <splitpanes class="default-theme" @resized="onResize('sidebar', $event)">
      
      <!-- LEFT PANE: SIDEBAR -->
      <pane :size="layout[0]" min-size="0" class="!bg-background relative border-r border-border shadow-sm transition-colors">
        <div class="h-full w-full relative flex flex-col overflow-hidden isolate">
            <slot name="sidebar"></slot>
        </div>
      </pane>

      <!-- RIGHT PANE: WORKSPACE -->
      <pane :size="layout[1]" class="!bg-background">
        <div class="h-full w-full relative flex flex-col overflow-hidden">
            <!-- Nested Split for List/Detail -->
            <splitpanes :horizontal="isMobile" @resized="onResize('workspace', $event)">
              <pane :size="layout[2]" min-size="0" class="!bg-background transition-colors">
                <div class="h-full w-full relative flex flex-col overflow-hidden isolate">
                    <slot name="list"></slot>
                </div>
              </pane>
              <pane :size="layout[3]" min-size="20" class="!bg-background border-l border-border transition-colors">
                <div class="h-full w-full relative flex flex-col overflow-hidden isolate">
                    <slot name="detail"></slot>
                </div>
              </pane>
            </splitpanes>
        </div>
      </pane>
    </splitpanes>
  </div>
</template>

<style scoped>
:deep(.splitpanes__splitter) {
    /* Widen hit area to 6px, keep visual line 1px via border-left */
    @apply bg-transparent hover:bg-blue-500/10 transition-colors w-[6px] !important;
    border-left: 1px solid theme('colors.border');
    margin-left: -1px;
    position: relative;
    z-index: 20;
}
:deep(.splitpanes__splitter:hover) {
    border-left-color: theme('colors.primary.DEFAULT');
}
</style>