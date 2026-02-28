<script setup lang="ts">
import { ref, h, computed, nextTick, type Component } from 'vue'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { useDebounceFn, onClickOutside, useStorage } from '@vueuse/core'
import { Search, Save, Trash2, Star, Terminal, Sparkles, Plus, Folder, Box, Tag, Calendar, Clock, X, Radio, Sun, Moon, Settings, Command, FileText, ExternalLink, Pencil, ArrowRight } from 'lucide-vue-next'
import { push } from 'notivue'
import { SearchService } from '@/services/SearchService'
import { SavedSearchService } from '@/services/SavedSearchService'
import { ItemService } from '@/services/ItemService'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { usePreferencesStore } from '@/stores/preferences'
import { useSearch } from '@/composables/useSearch'
import { useListNavigation } from '@/composables/useListNavigation'
import { useHotkeys } from '@/composables/useHotkeys'
import { ViewMode } from '@/enums'
import type { SearchResult } from '@/types/models'

import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import MasterDetailLayout from '@/components/layout/MasterDetailLayout.vue'
import DataTable from '@/components/ui/DataTable.vue'
import AppPane from '@/components/ui/BasePane.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import DocumentPreview from '@/components/ui/DocumentPreview.vue'
import DateCell from '@/components/ui/cells/DateCell.vue'
import BadgeCell from '@/components/ui/cells/BadgeCell.vue'
import SearchResultItem from '@/components/ui/SearchResultItem.vue'
import ItemEditForm from '@/components/forms/ItemEditForm.vue'

// --- CONSTANTS ---
const STORAGE_KEY = 'matros-search-layout-v2' as const
const DEFAULT_SIDEBAR_WIDTH = 20
const MIN_SIDEBAR_WIDTH = 2

const dms = useDmsStore()
const ui = useUIStore()
const prefs = usePreferencesStore()
const queryClient = useQueryClient()

const { 
    searchQuery, activeFilters, suggestions, currentTrigger, 
    fetchSuggestions, applySuggestion, removeFilter, buildQueryPayload, clearSearch 
} = useSearch()

// --- LAYOUT ---
const defaultLayout = { sidebar:[20, 80], workspace: [35, 65] }
const layoutStorage = useStorage(STORAGE_KEY, defaultLayout)

// Defensive layout initialization
if (!layoutStorage.value || !Array.isArray(layoutStorage.value.sidebar)) {
    layoutStorage.value = JSON.parse(JSON.stringify(defaultLayout))
}

const flatLayout = computed(() => [
    layoutStorage.value.sidebar[0], 
    layoutStorage.value.sidebar[1], 
    layoutStorage.value.workspace[0], 
    layoutStorage.value.workspace[1]
])

const handleLayoutUpdate = ({ key, sizes }: { key: string, sizes: number[] }) => {
    if (key === 'sidebar') layoutStorage.value.sidebar = sizes
    else if (key === 'workspace') layoutStorage.value.workspace = sizes
}

const toggleSidebar = () => {
    if (layoutStorage.value.sidebar[0] > MIN_SIDEBAR_WIDTH) {
        layoutStorage.value.sidebar = [0, 100]
    } else {
        layoutStorage.value.sidebar =[DEFAULT_SIDEBAR_WIDTH, 100 - DEFAULT_SIDEBAR_WIDTH]
    }
}

defineExpose({ toggleSidebar })

// --- STATE ---
const containerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
// Strict typing for results
const searchResults = ref<SearchResult[]>([])
const activeIndex = ref(-1)
const activeSuggestionIndex = ref(-1)
const isSearching = ref(false)
const hasSearched = ref(false) 
const saveName = ref('')
const isSaveMode = ref(false)
const showResults = ref(false)

interface CommandItem {
    id: string;
    label: string;
    desc: string;
    icon: any; 
    keywords: string[];
    action: () => void;
}

const COMMANDS: CommandItem[] =[
  { id: 'new-ctx', label: 'Create Context', desc: 'Start a new folder/context', icon: Plus, keywords: ['new', 'create', 'context', 'folder'], action: () => dms.startContextCreation() },
  { id: 'new-cat', label: 'Create Category', desc: 'Add a new category node', icon: Plus, keywords: ['new', 'create', 'category', 'tag'], action: () => dms.startCategoryCreation(dms.selectedCategoryId || '') },
  { id: 'toggle-theme', label: 'Toggle Dark Mode', desc: 'Switch visual theme', icon: prefs.isDarkMode ? Sun : Moon, keywords: ['dark', 'light', 'theme', 'mode'], action: () => prefs.toggleDarkMode() },
  { id: 'go-settings', label: 'Go to Settings', desc: 'System configuration', icon: Settings, keywords: ['settings', 'config', 'admin', 'users'], action: () => ui.setView('settings') },
]

const isCommandMode = computed(() => searchQuery.value.trim().startsWith('>'))

const filteredCommands = computed(() => {
  const q = searchQuery.value.replace(/^>\s*/, '').toLowerCase().trim()
  if (!q && isCommandMode.value) return COMMANDS
  return COMMANDS.filter(cmd => cmd.label.toLowerCase().includes(q) || cmd.keywords.some(k => k.includes(q)))
})

const { data: savedSearches } = useQuery({
    queryKey: ['saved-searches'],
    queryFn: SavedSearchService.getAll
})

const navigableItems = computed(() => {
    if (suggestions.value.length > 0) return suggestions.value.map(s => ({ type: 'suggestion', data: s }))
    if (isCommandMode.value) return filteredCommands.value.map(c => ({ type: 'command', data: c }))
    return searchResults.value.map(r => ({ type: 'result', data: r }))
})

// --- HELPERS ---
const getFilterBadgeClasses = (field: string) => {
    const map: Record<string, string> = {
        'CONTEXT': 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800',
        'STORE': 'bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-900/30 dark:text-orange-300 dark:border-orange-800',
        'ISSUE_DATE': 'bg-green-50 text-green-700 border-green-200 dark:bg-green-900/30 dark:text-green-300 dark:border-green-800',
        'CREATED': 'bg-green-50 text-green-700 border-green-200 dark:bg-green-900/30 dark:text-green-300 dark:border-green-800',
        'SOURCE': 'bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-900/30 dark:text-indigo-300 dark:border-indigo-800'
    }
    return map[field] || 'bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-900/30 dark:text-purple-300 dark:border-purple-800'
}

const jumpToContext = () => {
    const ctx = dms.selectedItem?.context
    if (ctx) {
        dms.setSelectedContext(ctx)
        dms.setSelectedItem(dms.selectedItem)
        ui.setView('dms')
        ui.setRightPanelView(ViewMode.DETAILS)
        push.info(`Jumped to folder: ${ctx.name}`)
    } else {
        push.warning("Item has no assigned folder")
    }
}

const startEdit = () => {
    if (dms.selectedItem) dms.startItemEditing() 
}

const onEditClose = () => {
    ui.setRightPanelView(ViewMode.DETAILS)
}

const isReadableText = (text: string | undefined | null): boolean => {
    if (!text) return false
    const plain = text.replace(/<[^>]*>/g, '')
    if (plain.length === 0) return false
    let bad = 0
    for (let i = 0; i < plain.length; i++) {
        const code = plain.charCodeAt(i)
        if (code < 0x20 && code !== 0x09 && code !== 0x0A && code !== 0x0D) bad++
        else if (code === 0xFFFD) bad++
    }
    return bad / plain.length < 0.1 
}

// --- COLUMNS ---
const columns =[
    {
        accessorKey: 'name',
        header: 'Document Name',
        size: 280,
        cell: (info: any) => {
            const val = info.getValue()
            const rawHighlight = info.row.original.highlight
            const rawDesc = info.row.original.description
            const highlight = isReadableText(rawHighlight) ? rawHighlight : null
            const description = isReadableText(rawDesc) ? rawDesc : null
            return h('div', { class: 'flex flex-col' },[
                h('span', { class: 'font-medium text-sm text-foreground truncate', innerHTML: highlight || val }),
                description ? h('span', { class: 'text-[10px] text-muted-foreground truncate' }, description) : null
            ])
        }
    },
    {
        accessorKey: 'score',
        header: 'Relevance',
        size: 80,
        cell: (info: any) => {
            const score = info.getValue() || 0
            const pct = Math.round(score * 100)
            let color = 'bg-gray-200 dark:bg-gray-700'
            if (pct > 80) color = 'bg-green-500'
            else if (pct > 50) color = 'bg-blue-500'
            return h('div', { class: 'flex items-center gap-2' },[
                h('div', { class: 'h-1.5 w-12 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden' },[
                    h('div', { class: `h-full ${color}`, style: { width: `${pct}%` } })
                ]),
                h('span', { class: 'text-[10px] text-muted-foreground font-mono' }, `${pct}%`)
            ])
        }
    },
    {
        accessorKey: 'storeName',
        header: () => h('div', { class: 'flex items-center gap-2' },[h(Box, { size: 14, class: 'text-orange-500' }), 'Store']),
        size: 140,
        cell: (info: any) => {
            const val = info.getValue()
            const itemNo = info.row.original.storeItemNumber
            if (!val) return h('span', { class: 'text-muted-foreground text-xs opacity-50' }, '-')
            
            const display = itemNo ? `${val} #${itemNo}` : val
            return h(BadgeCell, { value: display, defaultColor: 'bg-orange-50 text-orange-700 border-orange-200 dark:bg-orange-900/30 dark:text-orange-300 dark:border-orange-800' })
        }
    },
    {
        accessorKey: 'issueDate',
        header: 'Date',
        size: 100,
        cell: (info: any) => h(DateCell, { value: info.getValue() })
    },
    {
        accessorKey: 'contextName',
        header: 'Context',
        size: 120,
        cell: (info: any) => {
            const val = info.getValue()
            return val ? h(BadgeCell, { value: val, defaultColor: 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800' }) : h('span', { class: 'text-muted-foreground text-xs' }, '-')
        }
    }
]

const onSelectResult = async (result: SearchResult) => {
  if (!result || !result.uuid) return
  try {
    const fullItem = await ItemService.getById(result.uuid)
    dms.setSelectedItem(fullItem)
    // Don't auto-switch view, stay in Search to preserve flow
    ui.setRightPanelView(ViewMode.DETAILS)
  } catch (error: any) {
    push.error(error.message || "Could not load document")
  }
  showResults.value = false
}

// ... Navigation and Search Logic ...
const navigableLength = computed(() => navigableItems.value.length)
const suggestionLength = computed(() => suggestions.value.length)

const { handleKey: handleMainListKey } = useListNavigation({
    listLength: navigableLength,
    activeIndex: activeIndex,
    onSelect: (idx) => {
        const item = navigableItems.value[idx]
        if (item.type === 'suggestion') {
            applySuggestion(item.data as string)
            inputRef.value?.focus()
        }
        else if (item.type === 'command') { (item.data as CommandItem).action(); showResults.value = false; }
        else if (item.type === 'result') onSelectResult(item.data as SearchResult)
    }
})

const { handleKey: handleSuggestionKey } = useListNavigation({
    listLength: suggestionLength,
    activeIndex: activeSuggestionIndex,
    onSelect: (idx) => {
        const val = suggestions.value[idx];
        if (val) {
            applySuggestion(val);
            inputRef.value?.focus();
        }
    }
})

useHotkeys(['k', 'K'], (e) => { 
    e.preventDefault()
    showResults.value = true
    nextTick(() => inputRef.value?.focus())
}, { ctrl: true })

useHotkeys('Escape', () => { 
    showResults.value = false
    inputRef.value?.blur() 
})

const performSearch = async () => {
    suggestions.value =[]
    if (!searchQuery.value && activeFilters.value.length === 0) return
    
    isSearching.value = true
    hasSearched.value = true
    showResults.value = false
    
    try {
        const payload = buildQueryPayload()
        const results = await SearchService.search(payload)
        searchResults.value = results as unknown as SearchResult[]
    } catch (error: any) {
        const message = error instanceof Error ? error.message : "Search failed"
        push.error(message)
    } finally {
        isSearching.value = false
    }
}

const handleKeyNav = (e: KeyboardEvent) => {
    if (e.key === 'Backspace' && !searchQuery.value && activeFilters.value.length > 0) {
        activeFilters.value.pop()
        performSearch()
        return
    }
    if (e.ctrlKey && e.code === 'Space') {
        e.preventDefault()
        fetchSuggestions(true) 
        showResults.value = true
        return
    }
    if (suggestions.value.length > 0 && showResults.value) {
        handleSuggestionKey(e)
        if (e.defaultPrevented) return
    }
    if (showResults.value) {
        handleMainListKey(e)
    }
    if (e.key === 'Enter' && !e.defaultPrevented) {
        e.preventDefault()
        if (currentTrigger.value) {
            applySuggestion(currentTrigger.value.partial)
            inputRef.value?.focus()
        } else {
            performSearch()
        }
    }
}

const loadSavedSearch = (query: string) => { searchQuery.value = query; performSearch() }
const deleteSavedSearch = async (name: string) => {
    if (!confirm(`Delete saved search '${name}'?`)) return
    try {
        await SavedSearchService.delete(name)
        queryClient.invalidateQueries({ queryKey: ['saved-searches'] })
        push.success("Search deleted")
    } catch (e: any) { push.error(e.message) }
}
const saveCurrentSearch = async () => {
    if (!saveName.value || !searchQuery.value) return
    try {
        await SavedSearchService.create(saveName.value, searchQuery.value)
        queryClient.invalidateQueries({ queryKey: ['saved-searches'] })
        push.success("Search saved")
        isSaveMode.value = false; saveName.value = ''
    } catch (e: any) { push.error(e.message) }
}
const onRowClick = (item: any) => { if (item.uuid) ItemService.getById(item.uuid).then(fullItem => dms.setSelectedItem(fullItem)) }
const removeFilterAndSearch = (idx: number) => { removeFilter(idx); performSearch() }
const clearAndSearch = () => { clearSearch(); searchResults.value =[]; hasSearched.value = false }

onClickOutside(containerRef, () => suggestions.value =[])
</script>

<template>
  <MasterDetailLayout 
    :layout="flatLayout"
    @update:layout="handleLayoutUpdate"
  >
      <template #sidebar>
          <div class="flex flex-col h-full bg-background border-r border-border">
              <div class="p-3 border-b border-border font-bold text-xs uppercase tracking-wide text-foreground flex items-center gap-2 bg-muted/30">
                  <Star :size="14" class="text-yellow-500" /> Saved Queries
              </div>
              <div class="flex-1 overflow-y-auto p-2 space-y-1">
                  <div v-if="!savedSearches?.length" class="text-xs text-muted-foreground p-4 text-center italic">
                      No saved searches yet.
                  </div>
                  <div 
                    v-for="s in savedSearches" 
                    :key="s.name"
                    class="group flex items-center justify-between p-2 rounded-md hover:bg-muted/50 border border-transparent hover:border-border transition-colors cursor-pointer"
                    @click="loadSavedSearch(s.query || '')"
                  >
                      <div class="flex-1 min-w-0">
                          <div class="text-sm font-medium truncate text-foreground">{{ s.name }}</div>
                          <div class="text-[10px] text-muted-foreground truncate font-mono">{{ s.query }}</div>
                      </div>
                      <button @click.stop="deleteSavedSearch(s.name!)" class="opacity-0 group-hover:opacity-100 p-1.5 text-muted-foreground hover:text-destructive transition-opacity">
                          <Trash2 :size="14" />
                      </button>
                  </div>
              </div>
          </div>
      </template>

      <template #list>
          <AppPane title="Search Results" :count="searchResults.length">
              
              <template #actions>
                  <BaseButton 
                    variant="ghost" 
                    size="iconSm" 
                    @click="isSaveMode = !isSaveMode" 
                    :class="isSaveMode ? 'text-primary bg-primary/10' : 'text-muted-foreground'"
                    title="Save current query"
                  >
                      <Save :size="16" />
                  </BaseButton>
              </template>

              <template #filter>
                  <div class="p-1 space-y-2 relative" ref="containerRef" @keydown="handleKeyNav">
                      <div class="flex gap-2">
                          <div class="relative flex-1">
                              <Search class="absolute left-2.5 top-2 text-muted-foreground" :size="16" />
                              
                              <div 
                                class="w-full min-h-[34px] flex items-center pl-8 pr-2 border rounded-md bg-background focus-within:ring-1 focus-within:ring-primary focus-within:border-primary transition-all"
                                @click="inputRef?.focus()"
                              >
                                  <div class="flex flex-wrap gap-1 items-center mr-1">
                                      <div 
                                          v-for="(filter, idx) in activeFilters" 
                                          :key="idx"
                                          class="flex items-center gap-1 px-1.5 py-0.5 rounded text-[11px] font-bold border select-none"
                                          :class="getFilterBadgeClasses(filter.field)"
                                      >
                                          <span>{{ filter.label.split(':')[1] }}</span>
                                          <button @click.stop="removeFilterAndSearch(idx)" class="hover:text-red-600"><X :size="10" /></button>
                                      </div>
                                  </div>
                                  <input 
                                    ref="inputRef"
                                    v-model="searchQuery" 
                                    @input="() => fetchSuggestions()"
                                    @focus="showResults = true"
                                    placeholder="Enter MQL Query..." 
                                    class="flex-1 bg-transparent border-none outline-none text-sm h-full font-mono text-foreground min-w-[80px]"
                                    :class="isCommandMode ? 'font-mono text-primary' : ''"
                                    autocomplete="off"
                                  />
                                  <button v-if="searchQuery || activeFilters.length" @click="clearAndSearch" class="text-muted-foreground hover:text-foreground"><X :size="14"/></button>
                              </div>
                              
                              <!-- Suggestions / Dropdown -->
                              <div v-if="showResults && (suggestions.length > 0 || currentTrigger || isCommandMode)" 
                                   class="absolute top-full left-0 mt-1 w-full bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl z-50 overflow-hidden animate-in fade-in slide-in-from-top-1"
                              >
                                  <div v-if="suggestions.length > 0">
                                      <div class="px-2 py-1.5 text-[10px] font-bold text-primary uppercase tracking-wider bg-primary/5 border-b border-border flex items-center gap-1">
                                          <Sparkles :size="10" /> Suggestions
                                      </div>
                                      <div class="max-h-[200px] overflow-y-auto">
                                          <div 
                                            v-for="(sugg, idx) in suggestions" 
                                            :key="sugg"
                                            @click="applySuggestion(sugg); inputRef?.focus()"
                                            class="px-3 py-1.5 text-xs cursor-pointer hover:bg-primary/10 text-foreground flex justify-between items-center transition-colors"
                                            :class="idx === activeSuggestionIndex ? 'bg-primary/10' : ''"
                                          >
                                              <span v-html="sugg"></span>
                                              <span v-if="idx === activeSuggestionIndex" class="text-[9px] text-muted-foreground">Enter to select</span>
                                          </div>
                                      </div>
                                  </div>

                                  <div v-if="isCommandMode" class="bg-muted/10 overflow-y-auto">
                                        <div 
                                            v-for="(cmd, idx) in filteredCommands" 
                                            :key="cmd.id" 
                                            @click="cmd.action(); showResults = false" 
                                            class="w-full text-left px-3 py-2 flex items-center gap-3 transition-colors group cursor-pointer"
                                            :class="activeIndex === idx ? 'bg-primary/10' : 'hover:bg-muted/50'"
                                        >
                                            <div class="p-1.5 rounded transition-colors" :class="activeIndex === idx ? 'bg-primary text-white' : 'bg-muted text-muted-foreground'">
                                                <!-- Fixed: explicit casting -->
                                                <component :is="(cmd.icon as any)" :size="16" />
                                            </div>
                                            <div class="flex-1 min-w-0">
                                                <div class="text-sm font-medium text-foreground" :class="activeIndex === idx ? 'text-primary' : ''">{{ cmd.label }}</div>
                                                <div class="text-[10px] text-muted-foreground">{{ cmd.desc }}</div>
                                            </div>
                                            <Command v-if="activeIndex === idx" :size="14" class="text-primary opacity-50" />
                                        </div>
                                  </div>
                              </div>

                          </div>
                          <BaseButton size="sm" @click="performSearch" :loading="isSearching">Search</BaseButton>
                      </div>

                      <div v-if="isSaveMode" class="flex gap-2 items-center bg-muted/30 p-2 rounded border border-border animate-in slide-in-from-top-1">
                          <span class="text-xs font-bold text-foreground whitespace-nowrap">Name:</span>
                          <BaseInput v-model="saveName" placeholder="My Monthly Report..." class="flex-1 h-7 text-xs" />
                          <BaseButton size="sm" class="h-7 px-2" @click="saveCurrentSearch">Save</BaseButton>
                      </div>
                  </div>
              </template>

              <div class="h-full bg-background">
                  <div v-if="searchResults.length === 0" class="h-full flex flex-col items-center justify-center text-muted-foreground">
                      <div v-if="!hasSearched" class="text-center opacity-60">
                          <Search :size="48" class="mx-auto mb-4 opacity-20" />
                          <p class="text-sm">Enter a query to begin.</p>
                      </div>
                      <div v-else class="text-center opacity-60">
                          <Terminal :size="48" class="mx-auto mb-4 opacity-20" />
                          <p class="text-sm">No documents found.</p>
                      </div>
                  </div>

                  <div v-else class="overflow-y-auto custom-scrollbar bg-background h-full">
                    <SearchResultItem 
                        v-for="(res, idx) in searchResults" 
                        :key="res.uuid"
                        :active="activeIndex === idx"
                        :title="res.name"
                        :subtitle="res.highlight || res.description"
                        :score="res.score"
                        :date="res.issueDate"
                        :uuid="res.uuid"
                        :context="res.contextName"
                        :store="res.storeName"
                        :store-item-number="res.storeItemNumber"
                        @click="onSelectResult(res)"
                    />
                  </div>
              </div>
          </AppPane>
      </template>
      
      <!-- DETAIL PANE (Preview / Edit) -->
      <template #detail>
          <div v-if="dms.selectedItem" class="h-full flex flex-col bg-background relative">
             
             <!-- 1. EDIT MODE -->
             <div v-if="ui.rightPanelView === ViewMode.EDIT_ITEM" class="h-full w-full">
                 <ItemEditForm :initial-data="dms.selectedItem" @close="onEditClose" />
             </div>

             <!-- 2. PREVIEW MODE -->
             <div v-else class="h-full flex flex-col">
                 <!-- Header / Toolbar -->
                 <div class="px-4 py-2 border-b bg-background flex items-center justify-between shrink-0 h-[45px] shadow-sm z-10">
                     <div class="flex items-center gap-2 overflow-hidden">
                         <FileText :size="16" class="text-primary" />
                         <span class="font-bold text-sm truncate">{{ dms.selectedItem.name }}</span>
                     </div>
                     <div class="flex items-center gap-2">
                         <BaseButton 
                            v-if="dms.selectedItem.context"
                            variant="secondary" 
                            size="sm" 
                            class="h-7 text-xs bg-blue-50 hover:bg-blue-100 text-blue-700 border-blue-200"
                            @click="jumpToContext"
                            title="Jump to Folder"
                         >
                             <Folder :size="14" class="mr-1" /> {{ dms.selectedItem.context.name }} <ArrowRight :size="12" class="ml-1 opacity-50"/>
                         </BaseButton>
                         
                         <BaseButton variant="outline" size="sm" class="h-7" @click="startEdit">
                             <Pencil :size="14" class="mr-1" /> Edit
                         </BaseButton>
                     </div>
                 </div>

                 <DocumentPreview 
                   :identifier="dms.selectedItem.uuid || ''" 
                   source="item" 
                   :file-name="dms.selectedItem.name"
                 />
             </div>
          </div>
          <EmptyState 
            v-else 
            :icon="FileText"
            title="No Selection"
            description="Select a result to view details"
            class="h-full bg-muted/10"
          />
      </template>
  </MasterDetailLayout>
</template>