import { ref, computed } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useDmsStore } from '@/stores/dms'
import { useAdminQueries } from '@/composables/queries/useAdminQueries'
import { useContextQueries } from '@/composables/queries/useContextQueries'
import { SearchService } from '@/services/SearchService'
import { CategoryService } from '@/services/CategoryService'
import { ESearchDimension, EOperator, ERootCategory } from '@/enums' 
import { useQueryClient } from '@tanstack/vue-query'
import { queryKeys } from '@/composables/queries/queryKeys'
import type { components } from '@/types/schema'

// Strict Types
type MCategory = components['schemas']['MCategory']
type SearchCriteria = components['schemas']['SearchCriteria']

export function useSearch() {
    const dms = useDmsStore()
    const queryClient = useQueryClient()
    const { useStores } = useAdminQueries()
    const { data: availableStores } = useStores()
    const { contexts } = useContextQueries()

    // --- STATE ---
    const searchQuery = ref('')
    const activeFilters = ref<{ field: ESearchDimension; operator: EOperator; value: string; label: string }[]>([])
    const suggestions = ref<string[]>([])
    
    // --- CONSTANTS ---
    const DIMENSION_MAP: Record<string, ESearchDimension> = {
        'folder': ESearchDimension.CONTEXT,
        'context': ESearchDimension.CONTEXT,
        'store': ESearchDimension.STORE,
        'box': ESearchDimension.STORE,
        
        [ERootCategory.KIND.toLowerCase()]: ESearchDimension.KIND,
        'tag': ESearchDimension.KIND,
        'cat': ESearchDimension.KIND,
        
        [ERootCategory.WHO.toLowerCase()]: ESearchDimension.WHO,
        [ERootCategory.WHAT.toLowerCase()]: ESearchDimension.WHAT,
        [ERootCategory.WHERE.toLowerCase()]: ESearchDimension.WHERE,
        
        'date': ESearchDimension.ISSUE_DATE,
        'issued': ESearchDimension.ISSUE_DATE,
        'created': ESearchDimension.CREATED,
        'source': ESearchDimension.SOURCE,
        'text': ESearchDimension.FULLTEXT,
        'uuid': ESearchDimension.UUID,
        'id': ESearchDimension.UUID,
    }

    const keysPattern = Object.keys(DIMENSION_MAP).join('|')
    const triggerRegex = new RegExp(`(?:^|\\s)(${keysPattern}):([^\\s]*)$`, 'i')

    // --- HELPERS ---
    const parseInput = (input: string) => {
        if (!input) return { op: EOperator.EQ, val: '' }
        if (input.startsWith('>=')) return { op: EOperator.GTE, val: input.substring(2) }
        if (input.startsWith('<=')) return { op: EOperator.LTE, val: input.substring(2) }
        if (input.startsWith('>')) return { op: EOperator.GT, val: input.substring(1) }
        if (input.startsWith('<')) return { op: EOperator.LT, val: input.substring(1) }
        return { op: EOperator.EQ, val: input }
    }

    const currentTrigger = computed(() => {
        const match = searchQuery.value.match(triggerRegex)
        return match ? { raw: match[0], key: match[1].toLowerCase(), partial: match[2] } : null
    })

    // Typed Tree Traversal
    const flattenTree = (nodes: MCategory[], list: MCategory[] = []) => {
        for (const node of nodes) {
            if (node.name) list.push(node)
            if (node.children) flattenTree(node.children, list)
        }
        return list
    }

    const resolveUUID = (key: string, name: string): string => {
        // Local Context
        if (DIMENSION_MAP[key] === ESearchDimension.CONTEXT) {
            const found = contexts.value.find((c: any) => c.name === name)
            return found && found.uuid ? found.uuid : name
        }
        // Local Store
        if (DIMENSION_MAP[key] === ESearchDimension.STORE) {
            const found = availableStores.value?.find((s: any) => s.name === name || s.shortname === name)
            return found && found.uuid ? found.uuid : name
        }
        
        // Category Trees - Cache Lookup
        let treeKey: ERootCategory | null = null
        if (DIMENSION_MAP[key] === ESearchDimension.WHO) treeKey = ERootCategory.WHO
        if (DIMENSION_MAP[key] === ESearchDimension.WHAT) treeKey = ERootCategory.WHAT
        if (DIMENSION_MAP[key] === ESearchDimension.WHERE) treeKey = ERootCategory.WHERE
        if (DIMENSION_MAP[key] === ESearchDimension.KIND) treeKey = ERootCategory.KIND

        if (treeKey) {
            const tree = queryClient.getQueryData<MCategory>(queryKeys.category.tree(treeKey))
            if (tree && tree.children) {
                const flat = flattenTree(tree.children)
                const found = flat.find(n => n.name === name)
                if (found && found.uuid) return found.uuid
            }
        }
        return name
    }

    // --- ACTIONS ---
    const fetchSuggestions = useDebounceFn(async (force: boolean = false) => {
        if (currentTrigger.value) {
            const { key, partial } = currentTrigger.value
            const lowerPartial = partial.toLowerCase()
            const dim = DIMENSION_MAP[key]

            // 1. Local Contexts
            if (dim === ESearchDimension.CONTEXT) {
                suggestions.value = contexts.value
                    .filter((c: any) => (c.name || '').toLowerCase().includes(lowerPartial))
                    .map((c: any) => c.name || '')
                    .slice(0, 10)
                return
            }

            // 2. Local Stores
            if (dim === ESearchDimension.STORE) {
                suggestions.value = (availableStores.value || [])
                    .filter(s => (s.name || '').toLowerCase().includes(lowerPartial))
                    .map(s => s.name as string)
                    .filter(Boolean)
                return
            }
            
            // 3. Static Sources
            if (dim === ESearchDimension.SOURCE) {
                const sources = ['EMAIL', 'UPLOAD', 'SCAN', 'API'];
                suggestions.value = sources.filter(s => s.toLowerCase().includes(lowerPartial));
                return;
            }

            // 4. Category Trees (Local Cache preferred)
            let treeKey: ERootCategory | null = null
            if (dim === ESearchDimension.WHO) treeKey = ERootCategory.WHO
            if (dim === ESearchDimension.WHAT) treeKey = ERootCategory.WHAT
            if (dim === ESearchDimension.WHERE) treeKey = ERootCategory.WHERE
            if (dim === ESearchDimension.KIND) treeKey = ERootCategory.KIND

            if (treeKey) {
                try {
                    // Fetch from cache or API (Cached for 5min via staleTime)
                    const tree = await queryClient.ensureQueryData({
                        queryKey: queryKeys.category.tree(treeKey),
                        queryFn: () => CategoryService.getTree(treeKey as ERootCategory),
                        staleTime: 1000 * 60 * 5 
                    })

                    if (tree && tree.children) {
                        const flat = flattenTree(tree.children)
                        suggestions.value = flat
                            .filter(n => n.name && n.name.toLowerCase().includes(lowerPartial))
                            .map(n => n.name as string)
                            .slice(0, 20)
                        return
                    }
                } catch (e) {
                    console.warn(`Failed to load tree for ${treeKey}`, e)
                }
            }

            // 5. Fallback to Backend Autocomplete (e.g. Attributes)
            try {
                const sugs = await SearchService.getSuggestions(key, partial)
                suggestions.value = sugs || []
            } catch(e) { suggestions.value = [] }
            return
        }

        // Property Completion Mode
        const query = searchQuery.value
        const lastTokenMatch = query.match(/(?:^|\s)([^:\s]*)$/)
        const lastToken = lastTokenMatch ? lastTokenMatch[1] : ''

        if (lastToken || force) {
            const lowerToken = lastToken.toLowerCase()
            const keys = Object.keys(DIMENSION_MAP)
            
            const matches = keys
                .filter(k => k.toLowerCase().startsWith(lowerToken))
                .sort()
                .map(k => `${k}:`) 
            
            if (matches.length > 0) {
                suggestions.value = matches
                return
            }
        }

        suggestions.value = []
    }, 200)

    const applySuggestion = (val: string) => {
        if (val.endsWith(':')) {
            const query = searchQuery.value
            const lastTokenMatch = query.match(/(?:^|\s)([^:\s]*)$/)
            if (lastTokenMatch) {
                const lastToken = lastTokenMatch[1]
                const index = query.lastIndexOf(lastToken)
                searchQuery.value = index !== -1 ? query.substring(0, index) + val : searchQuery.value + val
            } else {
                searchQuery.value += val
            }
            suggestions.value = []
            
            // Automatically fetch values for the new property (debounced)
            fetchSuggestions()
            return
        }

        if (!currentTrigger.value) return

        const { key, raw } = currentTrigger.value
        const backendField = DIMENSION_MAP[key]
        const { op, val: cleanVal } = parseInput(val)
        const finalValue = resolveUUID(key, cleanVal)

        activeFilters.value.push({
            field: backendField,
            operator: op, 
            value: finalValue,
            label: `${key}:${op !== EOperator.EQ ? (op === EOperator.GT ? '>' : op === EOperator.LT ? '<' : '') : ''} ${cleanVal}`
        })

        searchQuery.value = searchQuery.value.replace(raw, '').trim() + ' '
        suggestions.value = []
    }

    const removeFilter = (index: number) => {
        activeFilters.value.splice(index, 1)
    }

    const buildQueryPayload = (): SearchCriteria => {
        const root: SearchCriteria = {
            type: 'GROUP',
            logic: 'AND',
            children: []
        }
        activeFilters.value.forEach(f => {
            root.children?.push({ type: 'FILTER', field: f.field, operator: f.operator, value: f.value })
        })
        const text = searchQuery.value.trim()
        if (text) {
            root.children?.push({ type: 'FILTER', field: ESearchDimension.FULLTEXT, operator: EOperator.CONTAINS, value: text })
        }
        return root
    }

    const clearSearch = () => {
        searchQuery.value = ''
        activeFilters.value = []
        suggestions.value = []
    }

    return {
        searchQuery,
        activeFilters,
        suggestions,
        currentTrigger,
        fetchSuggestions,
        applySuggestion,
        removeFilter,
        buildQueryPayload,
        clearSearch
    }
}