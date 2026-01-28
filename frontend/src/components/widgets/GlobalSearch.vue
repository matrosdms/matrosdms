<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { Search, X, Terminal, Settings, Plus, Moon, Sun, Sparkles, Command, Folder, Box, Tag, Calendar, Clock, Radio } from 'lucide-vue-next'
import { onClickOutside } from '@vueuse/core'
import { useHotkeys } from '@/composables/useHotkeys'
import { useListNavigation } from '@/composables/useListNavigation'
import { useDmsStore } from '@/stores/dms'
import { useUIStore } from '@/stores/ui'
import { usePreferencesStore } from '@/stores/preferences'
import { SearchService } from '@/services/SearchService'
import { ItemService } from '@/services/ItemService'
import { push } from 'notivue'
import { ViewMode } from '@/enums' 
import { useSearch } from '@/composables/useSearch'
import SearchResultItem from '@/components/ui/SearchResultItem.vue'

const dms = useDmsStore()
const ui = useUIStore()
const prefs = usePreferencesStore()

const { 
    searchQuery, activeFilters, suggestions, currentTrigger, 
    fetchSuggestions, applySuggestion, removeFilter, buildQueryPayload, clearSearch 
} = useSearch()

const containerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

const searchResults = ref<any[]>([])
const isSearching = ref(false)
const showResults = ref(false)
const activeIndex = ref(-1)
const activeSuggestionIndex = ref(-1)

const COMMANDS = [
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

const navigableItems = computed(() => {
    if (suggestions.value.length > 0) return suggestions.value.map(s => ({ type: 'suggestion', data: s }))
    if (isCommandMode.value) return filteredCommands.value.map(c => ({ type: 'command', data: c }))
    return searchResults.value.map(r => ({ type: 'result', data: r }))
})

// --- REFACTORED: Use shared navigation logic ---
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
        else if (item.type === 'command') { (item.data as any).action(); showResults.value = false; }
        else if (item.type === 'result') onSelectResult(item.data)
    }
})

const { handleKey: handleSuggestionKey } = useListNavigation({
    listLength: suggestionLength,
    activeIndex: activeSuggestionIndex,
    onSelect: (idx) => {
        applySuggestion(suggestions.value[idx])
        inputRef.value?.focus()
    }
})

watch(navigableItems, () => {
    activeIndex.value = -1
    activeSuggestionIndex.value = -1
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

onClickOutside(containerRef, () => showResults.value = false)

const performSearch = async () => {
  fetchSuggestions()
  if (currentTrigger.value) return

  if (activeFilters.value.length === 0 && (!searchQuery.value || searchQuery.value.length < 2)) {
      searchResults.value = []
      return
  }

  isSearching.value = true
  showResults.value = true
  
  try {
    const payload = buildQueryPayload()
    const results = await SearchService.search(payload)
    searchResults.value = results
  } catch (e) { console.error(e) } finally { isSearching.value = false }
}

const onSelectResult = async (result: any) => {
  if (!result || !result.uuid) return
  try {
    const fullItem = await ItemService.getById(result.uuid)
    dms.setSelectedContext(fullItem.context || null)
    dms.setSelectedItem(fullItem)
    ui.setView('dms')
    ui.setRightPanelView(ViewMode.DETAILS)
    ui.setZoom(false) 
  } catch (e) { push.error("Could not load document") }
  showResults.value = false
}

const handleKeyNav = (e: KeyboardEvent) => {
    // 1. Special Case: Backspace
    if (e.key === 'Backspace' && !searchQuery.value && activeFilters.value.length > 0) {
        activeFilters.value.pop()
        performSearch()
        return
    }

    // 2. Special Case: Ctrl+Space (Suggestions)
    if (e.ctrlKey && e.code === 'Space') {
        e.preventDefault()
        fetchSuggestions(true) 
        showResults.value = true
        return
    }

    if (!showResults.value && !currentTrigger.value) return

    // 3. Delegate to Composable
    // Priority: Suggestion Dropdown -> Main List -> Default Enter behavior
    if (suggestions.value.length > 0 && showResults.value) {
        handleSuggestionKey(e)
        // If the composable handled it (prevented default), stop here
        if (e.defaultPrevented) return
    }

    // Main List Navigation
    if (['ArrowDown', 'ArrowUp', 'Home', 'End', 'Enter'].includes(e.key)) {
        handleMainListKey(e)
    }

    // Fallback: Default Enter behavior (Search)
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

const removeFilterAndSearch = (idx: number) => {
    removeFilter(idx)
    performSearch()
}

const clearAndSearch = () => {
    clearSearch()
    searchResults.value = []
    showResults.value = false
}
</script>

<template>
<div ref="containerRef" class="relative w-full z-30" @keydown="handleKeyNav">
    <!-- Input Field (Same as before) -->
    <div 
        class="relative group flex items-center w-full min-h-[40px] px-3 py-1 bg-muted/50 hover:bg-background focus-within:bg-background border border-border rounded-xl transition-all shadow-sm focus-within:ring-2 focus-within:ring-primary/20 focus-within:border-primary"
        @click="inputRef?.focus()"
    >
        <div class="mr-2 text-muted-foreground group-focus-within:text-primary transition-colors">
            <Terminal v-if="isCommandMode" :size="16" />
            <Search v-else :size="16" />
        </div>

        <div class="flex flex-wrap gap-1.5 items-center mr-1">
            <div 
                v-for="(filter, idx) in activeFilters" 
                :key="idx"
                class="flex items-center gap-1 px-1.5 py-0.5 rounded text-[11px] font-bold border animate-in fade-in zoom-in-95 duration-200 select-none"
                :class="{
                    'bg-blue-50 text-blue-700 border-blue-200': filter.field === 'CONTEXT',
                    'bg-orange-50 text-orange-700 border-orange-200': filter.field === 'STORE',
                    'bg-green-50 text-green-700 border-green-200': ['ISSUE_DATE', 'CREATED'].includes(filter.field),
                    'bg-indigo-50 text-indigo-700 border-indigo-200': filter.field === 'SOURCE',
                    'bg-purple-50 text-purple-700 border-purple-200': !['CONTEXT', 'STORE', 'ISSUE_DATE', 'CREATED', 'FULLTEXT', 'SOURCE'].includes(filter.field)
                }"
            >
                <Folder v-if="filter.field === 'CONTEXT'" :size="10" />
                <Box v-else-if="filter.field === 'STORE'" :size="10" />
                <Calendar v-else-if="filter.field === 'ISSUE_DATE'" :size="10" />
                <Clock v-else-if="filter.field === 'CREATED'" :size="10" />
                <Radio v-else-if="filter.field === 'SOURCE'" :size="10" />
                <Tag v-else :size="10" />
                
                <span class="truncate max-w-[150px]">{{ filter.label.split(':')[1] }}</span>
                <button @click.stop="removeFilterAndSearch(idx)" class="hover:text-red-600"><X :size="10" /></button>
            </div>
        </div>

        <input 
            ref="inputRef"
            v-model="searchQuery" 
            @input="performSearch"
            @focus="showResults = true"
            placeholder="Search... (Try 'source:EMAIL', 'date:>2024')" 
            class="flex-1 bg-transparent border-none outline-none text-sm h-8 min-w-[120px] placeholder:text-muted-foreground/70"
            :class="isCommandMode ? 'font-mono text-primary' : ''"
            autocomplete="off"
        />

        <button v-if="searchQuery || activeFilters.length" @click.stop="clearAndSearch" class="ml-2 text-muted-foreground hover:text-foreground p-1 rounded-full hover:bg-muted transition-colors">
            <X :size="14" />
        </button>
        
        <div v-else class="ml-auto text-[10px] text-muted-foreground opacity-50 pointer-events-none border border-border px-1.5 rounded bg-muted/30 hidden md:block">
            Ctrl+K
        </div>
    </div>

    <!-- Dropdown Results -->
    <div v-if="showResults && (navigableItems.length > 0 || currentTrigger)" 
         class="absolute top-full left-0 w-full mt-2 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl shadow-2xl ring-1 ring-black/5 overflow-hidden animate-in fade-in slide-in-from-top-1 duration-200 z-50 max-h-[65vh] flex flex-col"
    >
        <!-- Suggestions Area -->
        <div v-if="suggestions.length > 0" class="bg-primary/5 border-b border-primary/20 shrink-0">
            <div class="px-3 py-2 text-[10px] font-bold text-primary uppercase tracking-wider flex items-center gap-1">
                <Sparkles :size="12"/> Suggestions
            </div>
            <div 
                v-for="(sugg, idx) in suggestions" 
                :key="sugg" 
                @click="applySuggestion(sugg); inputRef?.focus()" 
                class="w-full text-left px-3 py-1.5 text-xs text-primary transition-colors font-medium cursor-pointer flex justify-between items-center"
                :class="idx === activeSuggestionIndex ? 'bg-primary/10' : 'hover:bg-primary/5'"
            >
                <span>{{ sugg }}</span>
                <span v-if="idx === activeSuggestionIndex" class="text-[9px] opacity-60">Enter to select</span>
            </div>
        </div>
        
        <!-- Filter Hint -->
        <div v-else-if="currentTrigger && !suggestions.length" class="bg-primary/5 border-b border-primary/20 shrink-0 px-3 py-2">
             <div class="text-[10px] font-bold text-primary uppercase tracking-wider flex items-center gap-1 mb-1">
                <Plus :size="12"/> Add Filter
            </div>
            <div class="text-xs text-primary/80 italic">
                Press <strong>Enter</strong> to filter by 
                <span class="font-bold">{{ currentTrigger.key }}</span>: 
                <span class="font-mono bg-white px-1 rounded ml-1">{{ currentTrigger.partial }}</span>
            </div>
        </div>

        <!-- Commands List -->
        <div v-if="isCommandMode" class="bg-muted/10 overflow-y-auto">
             <div 
                v-for="(cmd, idx) in filteredCommands" 
                :key="cmd.id" 
                @click="cmd.action(); showResults = false" 
                class="w-full text-left px-3 py-2 flex items-center gap-3 transition-colors group cursor-pointer"
                :class="activeIndex === idx ? 'bg-primary/10' : 'hover:bg-muted/50'"
             >
                <div class="p-1.5 rounded transition-colors" :class="activeIndex === idx ? 'bg-primary text-white' : 'bg-muted text-muted-foreground'">
                    <component :is="cmd.icon" :size="16" />
                </div>
                <div class="flex-1 min-w-0">
                    <div class="text-sm font-medium text-foreground" :class="activeIndex === idx ? 'text-primary' : ''">{{ cmd.label }}</div>
                    <div class="text-[10px] text-muted-foreground">{{ cmd.desc }}</div>
                </div>
                <Command v-if="activeIndex === idx" :size="14" class="text-primary opacity-50" />
             </div>
        </div>

        <!-- Search Results List -->
        <div v-if="!isCommandMode && searchResults.length > 0" class="overflow-y-auto custom-scrollbar bg-background">
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
                @click="onSelectResult(res)"
            />
        </div>
    </div>
</div>
</template>