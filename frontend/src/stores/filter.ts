import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { ERootCategory, ERootCategoryList, type ERootCategoryType } from '@/enums'

export interface TagFilter {
    id: string;
    label: string;
    transitiveChildrenAndSelf?: Set<string>;
}

export const useFilterStore = defineStore('filter', () => {
  
  // Default active context (fallback)
  const activeContext = ref<ERootCategoryType>(ERootCategory.WHO)
  
  // Dynamic Initialization: Create an entry for every known Enum value
  const filters = reactive({}) as Record<ERootCategoryType, TagFilter[]>
  
  // Initialize all keys from the Auto-Generated Enum List
  // This ensures that if the backend adds 'OWNER', it exists in the state automatically
  ERootCategoryList.forEach(key => {
      filters[key] = []
  })

  function setActiveContext(ctx: ERootCategoryType) {
    activeContext.value = ctx
  }

  function addFilter(categoryKey: ERootCategoryType, labelObj: TagFilter) {
    if (!labelObj || !labelObj.id) return
    
    // Safety check if key exists (in case of runtime mismatch)
    if (!filters[categoryKey]) filters[categoryKey] = []
    
    const exists = filters[categoryKey].some(t => t.id === labelObj.id)
    if (!exists) filters[categoryKey].push(labelObj)
  }

  function removeFilter(categoryKey: ERootCategoryType, id: string) {
    if (filters[categoryKey]) {
      filters[categoryKey] = filters[categoryKey].filter(t => t.id !== id)
    }
  }
  
  function clearAllFilters() {
    ERootCategoryList.forEach(key => {
        filters[key] = []
    })
  }

  return { activeContext, filters, setActiveContext, addFilter, removeFilter, clearAllFilters }
})