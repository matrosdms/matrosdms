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

// --- Collect all descendant IDs (O(1) lookup via Set) ---
const collectDescendantIds = (node: any, result: Set<string> = new Set()): Set<string> => {
    result.add(node.id)
    if (node.children) {
        for (const child of node.children) {
            collectDescendantIds(child, result)
        }
    }
    return result
}

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

// --- Add to Filter Bar (Double-click / 'F' key) ---
const addToFilter = (node: FlatNode) => {
    if (props.selectionMode) return
    const rootType = activeContextKey.value as any
    
    // Build Set of this node + all descendants for O(1) matching
    const transitiveChildrenAndSelf = collectDescendantIds(node.node)
    
    dms.addFilter(rootType, { 
        id: node.id, 
        label: node.label,
        transitiveChildrenAndSelf 
    })
    dms.setActiveContext(rootType)
    notify.success(`Added "${node.label}" to ${rootType} filter`)
}

const onDoubleClick = (node: FlatNode) => {
    addToFilter(node)
}

// --- Expand All Children (Windows '*' key behavior) ---
const expandAll = (nodeId: string, nodeLevel: number) => {
    const startIdx = visibleNodes.value.findIndex(n => n.id === nodeId)
    if (startIdx === -1) return
    
    expandedIds.value.add(nodeId)
    
    // Expand all descendants
    for (let i = startIdx + 1; i < visibleNodes.value.length; i++) {
        const n = visibleNodes.value[i]
        if (n.level <= nodeLevel) break
        if (n.hasChildren) expandedIds.value.add(n.id)
    }
}

// --- Drag Support (Category -> Filter Bar) ---
const onDragStart = (event: DragEvent, node: any) => {
    event.stopPropagation()
    
    // Build Set of descendants for filter matching
    const transitiveIds = Array.from(collectDescendantIds(node.node))
    
    const payload = { 
        id: node.id, 
        label: node.label, 
        name: node.label,
        rootType: activeContextKey.value,
        transitiveIds // Array of all descendant IDs including self
    }
    startDrag(event, 'category-node', payload)
}

const onKeyDown = (e: KeyboardEvent) => {
    if (!visibleNodes.value.length) return

    const currentIndex = visibleNodes.value.findIndex(n => n.id === focusedNodeId.value)
    let nextIndex = currentIndex

    switch (e.key) {
        case 'ArrowDown':
            e.preventDefault()
            nextIndex = Math.min(currentIndex + 1, visibleNodes.value.length - 1)
            if (currentIndex === -1) nextIndex = 0
            break
        case 'ArrowUp':
            e.preventDefault()
            nextIndex = Math.max(currentIndex - 1, 0)
            break
        case 'ArrowRight':
            e.preventDefault()
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                if (node.hasChildren && !expandedIds.value.has(node.id)) {
                    expandedIds.value.add(node.id)
                } else if (node.hasChildren && node.isOpen) {
                    // Windows Explorer behavior: move to first child
                    nextIndex = currentIndex + 1
                }
            }
            break
        case 'ArrowLeft':
            e.preventDefault()
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                if (node.hasChildren && expandedIds.value.has(node.id)) {
                    expandedIds.value.delete(node.id)
                } else if (node.level > 0) {
                    // Windows Explorer behavior: jump to parent
                    for (let i = currentIndex - 1; i >= 0; i--) {
                        if (visibleNodes.value[i].level < node.level) {
                            nextIndex = i
                            break
                        }
                    }
                }
            }
            break
        case 'Home':
            e.preventDefault()
            nextIndex = 0
            break
        case 'End':
            e.preventDefault()
            nextIndex = visibleNodes.value.length - 1
            break
        case 'Enter':
        case ' ':
            e.preventDefault()
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                // In selection mode: select. In normal mode: add to filter (like double-click)
                if (props.selectionMode) {
                    selectNode(node.id, node.label)
                } else {
                    addToFilter(node)
                }
            }
            break
        case 'f':
        case 'F':
            // Add to filter bar (alias for Enter in non-selection mode)
            e.preventDefault()
            if (currentIndex !== -1 && !props.selectionMode) {
                const node = visibleNodes.value[currentIndex]
                addToFilter(node)
            }
            break
        case '*':
            // Expand all children (Windows Explorer behavior)
            e.preventDefault()
            if (currentIndex !== -1) {
                const node = visibleNodes.value[currentIndex]
                expandAll(node.id, node.level)
            }
            break
        default:
            // Type-ahead: jump to node starting with typed letter
            if (e.key.length === 1 && /[a-zA-Z0-9]/.test(e.key)) {
                const char = e.key.toLowerCase()
                const startIdx = currentIndex + 1
                for (let i = 0; i < visibleNodes.value.length; i++) {
                    const idx = (startIdx + i) % visibleNodes.value.length
                    if (visibleNodes.value[idx].label.toLowerCase().startsWith(char)) {
                        nextIndex = idx
                        break
                    }
                }
            }
            return // Don't prevent default for unhandled keys
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
                    @dblclick="onDoubleClick(node)"
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