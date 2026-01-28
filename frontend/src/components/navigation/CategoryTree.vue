<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import AppPane from '@/components/ui/BasePane.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import DynamicIcon from '@/components/ui/DynamicIcon.vue' 
import { ChevronRight, ChevronDown, Folder, Plus, Check, Pencil, ArrowLeftRight, RefreshCw, Layers } from 'lucide-vue-next'
import { useDmsStore } from '@/stores/dms' 
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { useNotifications } from '@/composables/useNotifications'
import { ERootCategoryList } from '@/enums'
import { useQueryClient } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'
import type { Category } from '@/types/models'
import { useDragDrop } from '@/composables/useDragDrop' // Import drag helper

const props = defineProps<{
    overrideContext?: string | null,
    selectionMode?: boolean,
    selectedId?: string | null
}>()

const emit = defineEmits(['node-selected'])
const dms = useDmsStore()
const { notify } = useNotifications()
const queryClient = useQueryClient()
const { useCategoryTree } = useContextQueries()
const { startDrag } = useDragDrop()

const searchQuery = ref('')
const expandedIds = ref(new Set<string>())
const treeContainer = ref<HTMLElement | null>(null)
const focusedNodeId = ref<string | null>(null)

const activeContextKey = computed(() => props.overrideContext || dms.activeContext)
const { data: rootCategory, isLoading } = useCategoryTree(activeContextKey)

interface FlatNode {
    id: string;
    label: string;
    level: number;
    hasChildren: boolean;
    isOpen: boolean;
    node: Category;
}

const flattenTree = (nodes: any[], level = 0, result: FlatNode[] = []) => {
    for (const node of nodes) {
        const matchesSearch = !searchQuery.value.trim() || node.label.toLowerCase().includes(searchQuery.value.toLowerCase());
        const hasVisibleChildren = node.children && node.children.length > 0;
        const isExpanded = searchQuery.value.trim() ? true : expandedIds.value.has(node.id);

        if (matchesSearch || (searchQuery.value && hasVisibleChildren)) {
             result.push({
                id: node.id,
                label: node.label,
                level,
                hasChildren: hasVisibleChildren,
                isOpen: isExpanded,
                node // Original Data
            })
        }

        if (hasVisibleChildren && isExpanded) {
            flattenTree(node.children, level + 1, result)
        }
    }
    return result
}

const mapCategory = (cat: Category): any => ({ 
    id: cat.uuid, 
    label: cat.name, 
    icon: cat.icon, // Map icon
    children: (cat.children || []).map(mapCategory)
})

const visibleNodes = computed(() => {
    if (!rootCategory.value) return []
    const rawNodes = (rootCategory.value.children || []).map(mapCategory)
    return flattenTree(rawNodes)
})

const toggle = (id: string) => { 
    if (expandedIds.value.has(id)) expandedIds.value.delete(id); 
    else expandedIds.value.add(id); 
}

const selectNode = (id: string, label: string) => {
    focusedNodeId.value = id
    if (props.selectionMode) emit('node-selected', { id, label })
    else dms.setSelectedCategory(id, label)
}

// --- Drag Support (Category -> Filter Bar) ---
const onDragStart = (event: DragEvent, node: any) => {
    event.stopPropagation()
    // UPDATED: Include 'rootType' in payload so ContextFilterBar can validate the drop
    const payload = { 
        id: node.id, 
        label: node.label, 
        name: node.label,
        rootType: activeContextKey.value // <--- Added Dimension Type
    }
    startDrag(event, 'category-node', payload)
}

const onKeyDown = (e: KeyboardEvent) => {
    if (!visibleNodes.value.length) return

    e.preventDefault()
    e.stopPropagation()

    const currentIndex = visibleNodes.value.findIndex(n => n.id === focusedNodeId.value)
    let nextIndex = currentIndex

    switch (e.key) {
        case 'ArrowDown':
            nextIndex = Math.min(currentIndex + 1, visibleNodes.value.length - 1)
            if (currentIndex === -1) nextIndex = 0
            break
        case 'ArrowUp':
            nextIndex = Math.max(currentIndex - 1, 0)
            break
        case 'ArrowRight':
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                if (node.hasChildren && !expandedIds.value.has(node.id)) expandedIds.value.add(node.id)
            }
            break
        case 'ArrowLeft':
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                if (node.hasChildren && expandedIds.value.has(node.id)) expandedIds.value.delete(node.id)
            }
            break
        case 'Enter':
        case ' ':
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                selectNode(node.id, node.label)
            }
            break
    }

    if (nextIndex !== currentIndex && nextIndex !== -1) {
        focusedNodeId.value = visibleNodes.value[nextIndex].id
        const el = document.getElementById(`tree-node-${focusedNodeId.value}`)
        el?.scrollIntoView({ block: 'nearest' })
    }
}

watch(() => dms.parentCategoryForCreation, (pid) => { if (!props.selectionMode && pid) expandedIds.value.add(pid) })

const cycleContext = () => {
  if (props.selectionMode) return
  const currentIdx = ERootCategoryList.indexOf(activeContextKey.value as any)
  const next = ERootCategoryList[(currentIdx + 1) % ERootCategoryList.length]
  dms.setActiveContext(next)
}

const onRefresh = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.category.tree(activeContextKey.value as any) })
    notify.info(`Refreshing ${activeContextKey.value}...`)
}

const onEditCategory = () => { if (!dms.selectedCategoryId) return notify.warning('Select a category'); dms.startCategoryEditing() }
const onAddCategory = () => { 
    const parentId = dms.selectedCategoryId || rootCategory.value?.uuid;
    if (!parentId) return notify.warning('Category tree not loaded.')
    dms.startCategoryCreation(parentId)
}

</script>

<template>
  <div class="h-full flex flex-col transition-colors" :class="selectionMode ? 'bg-white dark:bg-gray-800' : ''">
    <!-- Header only if not in selection mode -->
    <AppPane title="Categories" v-if="!selectionMode">
        <template #actions>
          <div 
            @click="cycleContext"
            class="flex items-center gap-1.5 text-[10px] bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 dark:text-gray-300 px-2 py-0.5 rounded cursor-pointer hover:bg-blue-50 dark:hover:bg-gray-700 hover:border-blue-200 hover:text-blue-600 transition-all select-none mr-2 group"
            title="Switch Dimension"
          >
            <span class="font-mono font-bold uppercase tracking-wider">{{ activeContextKey }}</span>
            <ArrowLeftRight :size="10" class="opacity-30 group-hover:opacity-100 transition-opacity" />
          </div>

          <div class="w-px h-3 bg-gray-300 dark:bg-gray-600 mx-1"></div>
          
          <BaseButton variant="ghost" size="iconSm" @click="onRefresh" title="Refresh Tree"><RefreshCw :size="14" /></BaseButton>
          <BaseButton variant="ghost" size="iconSm" @click="onEditCategory" :disabled="!dms.selectedCategoryId" title="Edit"><Pencil :size="14" /></BaseButton>
          <BaseButton variant="ghost" size="iconSm" class="text-primary" @click="onAddCategory" title="Add"><Plus :size="16" stroke-width="3" /></BaseButton>
        </template>

        <template #filter>
            <SearchInput v-model="searchQuery" placeholder="Filter categories..." />
        </template>

        <div 
            ref="treeContainer"
            class="p-1 space-y-0.5 outline-none flex-1 overflow-auto custom-scrollbar" 
            tabindex="0"
            @keydown="onKeyDown"
            role="tree"
        >
            <div v-if="isLoading" class="p-4 text-xs text-gray-400 italic text-center">Loading...</div>
            <div v-else-if="visibleNodes.length === 0" class="p-6 text-center flex flex-col items-center opacity-50">
                <Layers :size="24" class="mb-2 opacity-50" />
                <span class="text-xs text-gray-500 italic">No categories found.</span>
            </div>
            
            <div v-else>
                <div 
                    v-for="node in visibleNodes" 
                    :key="node.id"
                    :id="`tree-node-${node.id}`"
                    class="flex items-center py-1 cursor-pointer select-none group rounded-sm transition-colors relative"
                    :class="[
                        (selectionMode && selectedId === node.id) || (!selectionMode && dms.selectedCategoryId === node.id) ? 'bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 font-medium' : 'hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-700 dark:text-gray-300',
                        focusedNodeId === node.id ? 'ring-1 ring-inset ring-blue-400 dark:ring-blue-500' : ''
                    ]"
                    :style="{ paddingLeft: (node.level * 16 + 8) + 'px' }"
                    role="treeitem"
                    draggable="true"
                    @dragstart="onDragStart($event, node)"
                    :aria-expanded="node.hasChildren ? node.isOpen : undefined"
                    :aria-selected="(selectionMode && selectedId === node.id) || (!selectionMode && dms.selectedCategoryId === node.id)"
                    @click="selectNode(node.id, node.label)"
                >
                    <!-- Expander -->
                    <div 
                        @click.stop="toggle(node.id)" 
                        class="w-4 h-4 flex items-center justify-center mr-1 text-gray-400 hover:text-blue-500 shrink-0"
                    >
                        <ChevronDown v-if="node.isOpen && node.hasChildren" :size="14" />
                        <ChevronRight v-else-if="node.hasChildren" :size="14" />
                    </div>
                    
                    <!-- Icon / Bullet -->
                    <div class="mr-2 text-muted-foreground shrink-0">
                        <DynamicIcon v-if="node.node.icon" :name="node.node.icon" :size="14" />
                        <Folder v-else :size="14" />
                    </div>
                    
                    <span class="text-[13px] truncate flex-1">{{ node.label }}</span>
                    
                    <Check v-if="selectionMode && selectedId === node.id" :size="14" class="text-blue-600 mr-2" />
                </div>
            </div>
        </div>
    </AppPane>
    
    <!-- Simplified Selection Mode (Flat List for Dropdowns) -->
    <div v-else class="overflow-auto p-2" tabindex="0" @keydown="onKeyDown">
       <div 
            v-for="node in visibleNodes" 
            :key="node.id"
            :id="`tree-node-${node.id}`"
            class="flex items-center py-1.5 cursor-pointer rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            :class="[
                selectedId === node.id ? 'bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 font-bold' : 'text-gray-700 dark:text-gray-300',
                focusedNodeId === node.id ? 'ring-1 ring-inset ring-blue-400' : ''
            ]"
            :style="{ paddingLeft: (node.level * 12 + 8) + 'px' }"
            @click="selectNode(node.id, node.label)"
       >
           <div @click.stop="toggle(node.id)" class="w-4 mr-1 shrink-0 text-gray-400">
               <ChevronDown v-if="node.isOpen && node.hasChildren" :size="14"/>
               <ChevronRight v-else-if="node.hasChildren" :size="14"/>
           </div>
           
           <!-- Icon in Dropdown -->
           <div class="mr-2 text-muted-foreground shrink-0">
                <DynamicIcon v-if="node.node.icon" :name="node.node.icon" :size="14" />
                <Folder v-else :size="14" />
           </div>

           <span class="text-xs truncate">{{ node.label }}</span>
       </div>
    </div>
  </div>
</template>