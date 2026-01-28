<script setup lang="ts">
import { onMounted, ref, watch, onUnmounted } from 'vue'
import { DataSet, Timeline } from 'vis-timeline/standalone'
import 'vis-timeline/styles/vis-timeline-graph2d.min.css'
import { parseBackendDate } from '@/lib/utils'
import { useDmsStore } from '@/stores/dms'

const props = withDefaults(defineProps<{
  items: any[],
  height?: string,
  groupBy?: 'type' | 'context'
}>(), {
  height: '100%',
  groupBy: 'type'
})

const dms = useDmsStore()
const container = ref<HTMLElement | null>(null)
let timeline: Timeline | null = null

const initTimeline = () => {
    if (!container.value) return

    const visItems = new DataSet<any>([])
    const visGroups = new DataSet<any>([])
    const groupsSet = new Set()

    props.items.forEach(item => {
        const start = parseBackendDate(item.issueDate)
        if (!start) return 

        const end = parseBackendDate(item.dateExpire || item.dueDate)
        
        let groupId = 'uncategorized'
        let groupContent = 'General'
        
        if (props.groupBy === 'context') {
             if (item.contextName) {
                 groupId = item.contextId
                 groupContent = item.contextName
             }
        } else {
             if (item.kindList && item.kindList.length > 0) {
                groupId = item.kindList[0].uuid
                groupContent = item.kindList[0].name
            }
        }

        if (!groupsSet.has(groupId)) {
            visGroups.add({ id: groupId, content: groupContent })
            groupsSet.add(groupId)
        }

        let style = ''
        // Use colors from item if available
        if (item._color && item._borderColor) {
            style = `background-color: ${item._color}; border-color: ${item._borderColor};`
        }

        visItems.add({
            id: item.uuid,
            group: groupId,
            content: `<span class="text-xs font-medium truncate text-gray-800 dark:text-gray-900">${item.name}</span>`,
            start: start,
            end: end,
            type: end ? 'range' : 'point',
            className: dms.selectedItem?.uuid === item.uuid ? 'vis-selected-custom' : '',
            style: style,
            title: `${item.name}\n${item.description || ''}`
        })
    })

    const options = {
        stack: true,
        horizontalScroll: true,
        zoomKey: 'ctrlKey' as const,
        maxHeight: props.height, 
        height: props.height,    
        orientation: 'top',
        selectable: true,
        multiselect: false,
        template: (item: any) => item.content, 
    }

    timeline = new Timeline(container.value, visItems, visGroups, options)

    timeline.on('select', (properties) => {
        const selectedId = properties.items[0]
        if (selectedId) {
            const item = props.items.find(i => i.uuid === selectedId)
            if (item) dms.setSelectedItem(item)
        } else {
            dms.setSelectedItem(null)
        }
    })
    
    if (props.items.length > 0) (timeline as any).fit()
}

watch(() => [props.items, props.groupBy], () => {
    if (timeline) timeline.destroy()
    initTimeline()
}, { deep: true })

onMounted(() => initTimeline())
onUnmounted(() => { if(timeline) timeline.destroy() })
</script>

<template>
  <div class="h-full w-full flex flex-col bg-white dark:bg-gray-900 transition-colors">
      <div class="p-2 bg-yellow-50 dark:bg-yellow-900/20 text-yellow-800 dark:text-yellow-400 text-xs border-b border-yellow-200 dark:border-yellow-900 flex justify-between shrink-0">
          <span><strong>Interactive:</strong> Scroll to zoom, Drag to move. Ctrl+Scroll for fast zoom.</span>
          <button @click="(timeline as any)?.fit()" class="underline font-bold hover:text-yellow-600 dark:hover:text-yellow-300">Reset Zoom</button>
      </div>
      <div ref="container" class="flex-1 w-full h-full relative timeline-container min-h-0"></div>
  </div>
</template>

<style>
.vis-timeline { border: none; font-family: inherit; }
.vis-item {
    border-radius: 4px;
    font-size: 11px;
    border-color: #93c5fd;
    background-color: #eff6ff;
    color: #1e3a8a;
}
.vis-item.vis-selected {
    z-index: 2;
    box-shadow: 0 0 0 2px rgba(0,0,0, 0.4);
}
.vis-time-axis .vis-text { color: #6b7280; font-size: 10px; }
.vis-labelset .vis-label { border-bottom: 1px solid #f3f4f6; color: #374151; font-weight: 600; font-size: 11px; display: flex; align-items: center; padding-left: 10px; }

/* Dark Mode Overrides */
.dark .vis-time-axis .vis-text { color: #9ca3af; } /* gray-400 */
.dark .vis-labelset .vis-label { border-bottom: 1px solid #374151; color: #e5e7eb; } /* gray-700 border, gray-200 text */
.dark .vis-panel.vis-left { border-right: 1px solid #374151; }
.dark .vis-panel.vis-bottom, .dark .vis-panel.vis-center, .dark .vis-panel.vis-left, .dark .vis-panel.vis-right, .dark .vis-panel.vis-top {
    border-color: #374151;
}
.dark .vis-grid.vis-minor { border-color: #374151; } /* gray-700 */
.dark .vis-grid.vis-major { border-color: #4b5563; } /* gray-600 */
.dark .vis-current-time { background-color: #ef4444; } /* red-500 */
</style>