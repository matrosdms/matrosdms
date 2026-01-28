<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { client } from '@/api/client'
import { useMatrosData } from '@/composables/useMatrosData'
import ContextVisTimeline from '@/components/visualizations/ContextVisTimeline.vue'
import ContextFlow from '@/components/visualizations/ContextFlow.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import { Loader2, CheckSquare, Square, FolderOpen, BarChartHorizontal, GitCommit, LayoutList, ArrowLeft, ArrowUpRight } from 'lucide-vue-next'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { ERootCategoryList } from '@/enums'
import { stringToColor, stringToBorderColor } from '@/lib/utils'

const dms = useDmsStore()
const ui = useUIStore()
const { contexts } = useMatrosData()

const searchQuery = ref('')
const selectedContextIds = ref<Set<string>>(new Set())
const loadedItems = ref<Map<string, any[]>>(new Map()) 
const isLoadingItems = ref(false)

const visualizationMode = ref<'gantt' | 'flow'>('gantt')
const ganttGroupBy = ref<'type' | 'context'>('context')

const processedContexts = computed(() => {
  if (!contexts.value) return []
  return contexts.value.map((ctx: any) => {
    const categoryIds = new Set<string>()
    if (ctx.dictionary) {
      Object.values(ctx.dictionary).forEach((tagList: any) => {
        if (Array.isArray(tagList)) {
          tagList.forEach((tag: any) => {
            if (tag.uuid) categoryIds.add(tag.uuid)
            if (tag.parents) {
              tag.parents.forEach((p: any) => { if (p.uuid) categoryIds.add(p.uuid) })
            }
          })
        }
      })
    }
    return { ...ctx, _categoryIds: categoryIds }
  })
})

const filteredContexts = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  const hasGlobalFilters = ERootCategoryList.some(cat => dms.filters[cat]?.length > 0)

  return processedContexts.value.filter(ctx => {
    if (query && !(ctx.name || '').toLowerCase().includes(query)) return false
    if (!hasGlobalFilters) return true

    for (const cat of ERootCategoryList) {
        const activeFilters = dms.filters[cat]
        if (activeFilters && activeFilters.length > 0) {
            const match = activeFilters.some(filter => {
                if (filter.transitiveChildrenAndSelf) {
                    for (const tagId of ctx._categoryIds) {
                        if (filter.transitiveChildrenAndSelf.has(tagId)) return true;
                    }
                    return false;
                } else {
                    return ctx._categoryIds.has(filter.id);
                }
            })
            if (!match) return false
        }
    }
    return true
  })
})

const goBack = () => {
    ui.setView('dms')
}

const toggleContext = async (context: any) => {
    const id = context.uuid
    if (selectedContextIds.value.has(id)) {
        selectedContextIds.value.delete(id)
        loadedItems.value.delete(id)
    } else {
        selectedContextIds.value.add(id)
        if (!loadedItems.value.has(id)) {
            await fetchItems(id, context.name)
        }
    }
}

const jumpToContext = (context: any) => {
    dms.setSelectedContext(context)
    ui.setView('dms') 
}

const fetchItems = async (contextId: string, contextName: string) => {
    isLoadingItems.value = true
    try {
        const pageable = { size: 500, sort: ['issueDate,asc'] }
        const { data } = await client.GET("/api/items", { 
            params: { query: { context: contextId, pageable } as any } 
        })
        
        const rawData = (data as any)
        const content = rawData?.content || rawData 
        
        if (Array.isArray(content)) {
            const color = stringToColor(contextId)
            const borderColor = stringToBorderColor(contextId)
            const enriched = content.map((item: any) => ({
                ...item,
                contextId,
                contextName,
                _color: color,
                _borderColor: borderColor
            }))
            loadedItems.value.set(contextId, enriched)
        }
    } catch (e) {
        console.error("Failed to load items", e)
    } finally {
        isLoadingItems.value = false
    }
}

const timelineData = computed(() => {
    const all: any[] = []
    loadedItems.value.forEach(items => all.push(...items))
    return all
})

watch(() => selectedContextIds.value.size, (count) => {
    if (count === 1) ganttGroupBy.value = 'type'
    else ganttGroupBy.value = 'context'
})

onMounted(() => {
    if (dms.selectedContext) toggleContext(dms.selectedContext)
})
</script>

<template>
  <div class="h-full w-full flex bg-gray-100 dark:bg-black overflow-hidden transition-colors duration-300">
      
      <!-- Sidebar -->
      <div class="w-80 bg-white dark:bg-gray-900 border-r border-gray-200 dark:border-gray-800 flex flex-col shrink-0 z-10 shadow-sm transition-colors">
          
          <div class="p-3 border-b border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800/50 flex items-center gap-2">
              <button @click="goBack" class="p-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-100 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-200 transition-colors shadow-sm" title="Back to Folders">
                  <ArrowLeft :size="16" />
              </button>
              <span class="text-xs font-bold text-gray-600 dark:text-gray-300 uppercase tracking-wide">Timeline Analysis</span>
          </div>

          <div class="p-4 border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900">
              <SearchInput v-model="searchQuery" placeholder="Find contexts..." />
              <div v-if="filteredContexts.length !== contexts.length" class="mt-2 text-[10px] text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 px-2 py-1 rounded flex items-center gap-1 border border-blue-100 dark:border-blue-900">
                  <LayoutList :size="12" />
                  Filtered by global tags
              </div>
          </div>
          
          <div class="flex-1 overflow-y-auto custom-scrollbar p-2">
              <div 
                v-for="ctx in filteredContexts" 
                :key="ctx.uuid"
                class="flex items-center gap-2 p-2 rounded-md transition-colors group select-none border-l-4"
                :class="selectedContextIds.has(ctx.uuid) ? 'bg-blue-50 dark:bg-blue-900/20' : 'bg-white dark:bg-gray-900 border-transparent hover:bg-gray-50 dark:hover:bg-gray-800'"
                :style="{ borderLeftColor: selectedContextIds.has(ctx.uuid) ? stringToBorderColor(ctx.uuid) : 'transparent' }"
              >
                  <div @click="toggleContext(ctx)" class="flex-1 flex items-center gap-2 cursor-pointer min-w-0">
                      <div :class="selectedContextIds.has(ctx.uuid) ? 'text-blue-600 dark:text-blue-400' : 'text-gray-300 dark:text-gray-600 group-hover:text-blue-400'">
                          <CheckSquare v-if="selectedContextIds.has(ctx.uuid)" :size="18" />
                          <Square v-else :size="18" />
                      </div>
                      <div class="flex-1 min-w-0">
                          <div class="text-sm font-medium text-gray-800 dark:text-gray-200 truncate">{{ ctx.name }}</div>
                          <div class="text-[10px] text-gray-400 dark:text-gray-500">{{ ctx.itemCount || 0 }} items</div>
                      </div>
                  </div>
                  <button @click="jumpToContext(ctx)" class="p-1.5 text-gray-400 dark:text-gray-500 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-white dark:hover:bg-gray-700 rounded transition-all opacity-0 group-hover:opacity-100" title="Open in Folder View">
                      <ArrowUpRight :size="16" />
                  </button>
              </div>
          </div>
      </div>

      <!-- Main Canvas -->
      <div class="flex-1 flex flex-col min-w-0 bg-white dark:bg-gray-900 relative transition-colors">
          <div v-if="isLoadingItems" class="absolute top-16 right-6 z-30 flex items-center gap-2 bg-white/90 dark:bg-gray-800/90 px-3 py-1.5 rounded-full shadow border border-blue-100 dark:border-blue-900 text-blue-600 dark:text-blue-400 text-xs font-bold animate-in fade-in">
              <Loader2 class="animate-spin" :size="14" /> Loading data...
          </div>

          <div v-if="selectedContextIds.size === 0" class="flex-1 flex flex-col items-center justify-center text-gray-400 dark:text-gray-600 opacity-60">
              <FolderOpen :size="48" class="mb-4" stroke-width="1.5" />
              <p class="text-lg font-medium">Select Contexts to visualize</p>
          </div>

          <template v-else>
              <div class="border-b border-gray-200 dark:border-gray-800 p-2 flex justify-between items-center bg-white dark:bg-gray-900 z-20">
                  <div class="flex items-center gap-4">
                      <div class="flex bg-gray-100 dark:bg-gray-800 rounded-lg p-1 border border-gray-200 dark:border-gray-700">
                          <button @click="visualizationMode = 'gantt'" class="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-bold transition-all" :class="visualizationMode === 'gantt' ? 'bg-white dark:bg-gray-700 text-blue-600 dark:text-blue-400 shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'">
                              <BarChartHorizontal :size="14" /> Gantt Chart
                          </button>
                          <button @click="visualizationMode = 'flow'" class="flex items-center gap-2 px-3 py-1.5 rounded-md text-xs font-bold transition-all" :class="visualizationMode === 'flow' ? 'bg-white dark:bg-gray-700 text-blue-600 dark:text-blue-400 shadow-sm' : 'text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'">
                              <GitCommit :size="14" class="rotate-90" /> Vertical Flow
                          </button>
                      </div>
                      <div class="h-6 w-px bg-gray-200 dark:bg-gray-700 mx-2"></div>
                      <div v-if="visualizationMode === 'gantt'" class="flex items-center gap-2 text-xs">
                          <span class="text-gray-400 dark:text-gray-500 font-medium uppercase tracking-wide text-[10px]">Group By:</span>
                          <button @click="ganttGroupBy = 'context'" :class="ganttGroupBy === 'context' ? 'text-blue-600 dark:text-blue-400 font-bold bg-blue-50 dark:bg-blue-900/30 px-2 py-0.5 rounded' : 'text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'">Context</button>
                          <button @click="ganttGroupBy = 'type'" :class="ganttGroupBy === 'type' ? 'text-blue-600 dark:text-blue-400 font-bold bg-blue-50 dark:bg-blue-900/30 px-2 py-0.5 rounded' : 'text-gray-500 dark:text-gray-400 hover:text-gray-800 dark:hover:text-gray-200'">Type</button>
                      </div>
                  </div>
                  <div class="text-xs text-gray-400 dark:text-gray-500 font-mono">{{ timelineData.length }} Events</div>
              </div>

              <div class="flex-1 overflow-auto bg-gray-50 dark:bg-gray-900 relative min-h-0">
                  <ContextVisTimeline v-if="visualizationMode === 'gantt'" :items="timelineData" :group-by="ganttGroupBy" height="100%" />
                  <div v-else class="h-full overflow-y-auto custom-scrollbar"><ContextFlow :items="timelineData" /></div>
              </div>
          </template>
      </div>
  </div>
</template>