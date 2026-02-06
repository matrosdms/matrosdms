<script setup>
import { ref, computed, watch } from 'vue'
import { Database, Users, Box, Plus, RefreshCw, List, UploadCloud, Activity } from 'lucide-vue-next'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { useStorage } from '@vueuse/core'
import { push } from 'notivue'

import DataTable from '@/components/ui/DataTable.vue'
import AppPane from '@/components/ui/BasePane.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import MasterDetailLayout from '@/components/layout/MasterDetailLayout.vue'
import { SETTINGS_TABS } from '@/config/settingsTabs'

const activeTab = ref('stores') 
const selectedId = ref(null)
const isCreating = ref(false)
const isEditing = ref(false)
const queryClient = useQueryClient()
const customFilter = ref('')

// Layout State (Standardized)
const STORAGE_KEY = 'matros-settings-layout-v2'
const defaultState = { sidebar: [20, 80], workspace: [40, 60] }
const layoutStorage = useStorage(STORAGE_KEY, defaultState)

// Safety Init
if (!layoutStorage.value || !Array.isArray(layoutStorage.value.sidebar) || !Array.isArray(layoutStorage.value.workspace)) {
    layoutStorage.value = JSON.parse(JSON.stringify(defaultState))
}

const flatLayout = computed(() => [
    layoutStorage.value.sidebar[0], 
    layoutStorage.value.sidebar[1], 
    layoutStorage.value.workspace[0], 
    layoutStorage.value.workspace[1]
])

const handleLayoutUpdate = ({ key, sizes }) => {
    if (key === 'sidebar') layoutStorage.value.sidebar = sizes
    else if (key === 'workspace') layoutStorage.value.workspace = sizes
}

// Actions
const toggleSidebar = () => {
  if (layoutStorage.value.sidebar[0] > 2) layoutStorage.value.sidebar = [0, 100]
  else layoutStorage.value.sidebar = [20, 80]
}

const saveLayout = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(layoutStorage.value))
    push.success('Settings layout saved')
}

const resetLayout = () => {
    layoutStorage.value = JSON.parse(JSON.stringify(defaultState))
    push.info('Layout reset')
}

defineExpose({ toggleSidebar, saveLayout, resetLayout })

// --- Data Logic ---

const currentConfig = computed(() => SETTINGS_TABS[activeTab.value])

const queryDefinition = computed(() => {
    if (currentConfig.value?.queryResolver) {
        return currentConfig.value.queryResolver(customFilter.value)
    }
    return currentConfig.value?.query || { queryKey: ['static', activeTab.value], queryFn: async () => currentConfig.value?.staticData || [] }
})

const { data: apiData, isLoading: isQueryLoading, refetch } = useQuery({
  queryKey: computed(() => queryDefinition.value.queryKey),
  queryFn: computed(() => queryDefinition.value.queryFn),
  enabled: computed(() => !!currentConfig.value),
  refetchInterval: computed(() => activeTab.value === 'jobs' && (customFilter.value === 'RUNNING' || customFilter.value === '') ? 2000 : false),
  staleTime: 5 * 60 * 1000
})

const listData = computed(() => apiData.value || [])
const selectedItem = computed(() => listData.value.find(i => (i.uuid || i.id) === selectedId.value))

const currentViewComponent = computed(() => {
    const cfg = currentConfig.value?.components
    if (!cfg) return null
    if (isCreating.value && cfg.create) return cfg.create
    if (selectedId.value && selectedItem.value) {
        if (isEditing.value && cfg.edit) return cfg.edit
        return cfg.detail
    }
    // Special case: Import tab shows detail immediately
    if (activeTab.value === 'import') return cfg.detail
    return null
})

// Auto-select first item when data loads and nothing is selected
watch(listData, (items) => {
  if (items?.length > 0 && !selectedId.value) {
    selectedId.value = items[0].uuid || items[0].id
  }
}, { immediate: true })

const onTabChange = (tab) => { 
  activeTab.value = tab
  selectedId.value = null
  isCreating.value = false
  isEditing.value = false
  customFilter.value = ''
  
  const config = SETTINGS_TABS[tab]
  if (config?.staticData?.length > 0) {
      selectedId.value = config.staticData[0].uuid
  }
}

const onSelectNext = () => {
  const items = listData.value
  if (!items?.length) return
  const currentIndex = items.findIndex(i => (i.uuid || i.id) === selectedId.value)
  if (currentIndex < items.length - 1) {
    selectedId.value = items[currentIndex + 1].uuid || items[currentIndex + 1].id
  }
}

const onRefresh = () => { refetch(); push.info('Refreshed') }
const onCloseEditor = () => { isCreating.value = false; isEditing.value = false }

const onDelete = async () => {
  if (!selectedItem.value || !currentConfig.value?.service?.delete) return
  if (!confirm(`Delete ${selectedItem.value.name}?`)) return
  try {
    await currentConfig.value.service.delete(selectedItem.value.uuid)
    queryClient.invalidateQueries({ queryKey: queryDefinition.value.queryKey })
    selectedId.value = null
    push.success('Deleted successfully')
  } catch (e) { push.error('Delete failed: ' + e.message) }
}

const getRowClass = (row) => (row.uuid === selectedId.value || row.id === selectedId.value) ? '!border-l-blue-600 bg-blue-50 dark:bg-blue-900/20 dark:text-white' : 'dark:text-gray-300'
</script>

<template>
  <MasterDetailLayout 
    :layout="flatLayout" 
    @update:layout="handleLayoutUpdate"
  >
      <!-- 1. NAVIGATION -->
      <template #sidebar>
        <div class="p-3 border-b border-border font-bold text-foreground flex items-center gap-2 h-[40px] bg-muted/30 shrink-0">
          <Database :size="16" class="text-primary" /> <span class="text-xs uppercase tracking-wide">Master Data</span>
        </div>
        
        <div class="flex-1 p-2 space-y-1 overflow-hidden">
          <BaseButton variant="ghost" class="w-full justify-start" :class="{ 'bg-primary/10 text-primary': activeTab === 'stores' }" @click="onTabChange('stores')">
             <Box :size="18" class="mr-2"/> Stores
          </BaseButton>
          <BaseButton variant="ghost" class="w-full justify-start" :class="{ 'bg-primary/10 text-primary': activeTab === 'users' }" @click="onTabChange('users')">
             <Users :size="18" class="mr-2"/> Users
          </BaseButton>
          <BaseButton variant="ghost" class="w-full justify-start" :class="{ 'bg-primary/10 text-primary': activeTab === 'attributes' }" @click="onTabChange('attributes')">
             <List :size="18" class="mr-2"/> Attributes
          </BaseButton>
          
          <div class="my-2 border-t border-border"></div>
          
          <BaseButton variant="ghost" class="w-full justify-start" :class="{ 'bg-primary/10 text-primary': activeTab === 'import' }" @click="onTabChange('import')">
             <UploadCloud :size="18" class="mr-2"/> Import Categories
          </BaseButton>
          <BaseButton variant="ghost" class="w-full justify-start" :class="{ 'bg-primary/10 text-primary': activeTab === 'jobs' }" @click="onTabChange('jobs')">
             <Activity :size="18" class="mr-2"/> System Jobs
          </BaseButton>
        </div>
      </template>

      <!-- 2. LIST -->
      <template #list>
        <AppPane :title="currentConfig?.title" :count="listData.length">
          <template #actions>
            <component 
                v-if="currentConfig?.components?.toolbar" 
                :is="currentConfig.components.toolbar" 
                v-model="customFilter"
            />
            <BaseButton variant="ghost" size="iconSm" @click="onRefresh" :loading="isQueryLoading" title="Refresh">
                <RefreshCw :size="14" />
            </BaseButton>
            <div class="w-px h-3 bg-border mx-1"></div>
            <BaseButton v-if="currentConfig?.components?.create" variant="ghost" size="iconSm" class="text-primary" @click="isCreating = true; selectedId = null; isEditing = false" title="Create New">
                <Plus :size="16" stroke-width="3" />
            </BaseButton>
            <component 
                v-if="currentConfig?.components?.actions" 
                :is="currentConfig.components.actions" 
                @job-started="onRefresh"
            />
          </template>

          <div class="h-full !bg-background">
             <DataTable :key="activeTab" :data="listData" :columns="currentConfig?.columns || []" :row-class-name="getRowClass" :selected-id="selectedId" @row-click="(item) => { selectedId = item.uuid || item.id; isCreating = false; isEditing = false }" />
          </div>
        </AppPane>
      </template>

      <!-- 3. DETAILS -->
      <template #detail>
        <div class="h-full flex flex-col relative bg-muted/10">
            <template v-if="currentViewComponent">
                <component 
                    :is="currentViewComponent" 
                    :item="selectedItem"
                    :initial-data="isEditing ? selectedItem : null"
                    @close="onCloseEditor"
                    @edit="isEditing = true"
                    @delete="onDelete"
                    @next="onSelectNext"
                />
            </template>
            
            <div v-else class="h-full flex flex-col items-center justify-center text-muted-foreground bg-muted/20">
                <p class="text-sm font-medium">Select an item to view details</p>
                <BaseButton v-if="currentConfig?.components?.create" variant="outline" class="mt-4" @click="isCreating = true; selectedId = null">
                   Create New
                </BaseButton>
            </div>
        </div>
      </template>
  </MasterDetailLayout>
</template>