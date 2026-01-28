<script setup>
import { useUIStore } from '@/stores/ui'
import { useDashboardLayout } from '@/composables/useDashboardLayout'

// Components
import MasterDetailLayout from '@/components/layout/MasterDetailLayout.vue'
import CategoryTree from '@/components/navigation/CategoryTree.vue' 
import InboxList from '@/components/panes/InboxList.vue'
import ActionList from '@/components/panes/ActionList.vue' 
import ContextList from '@/components/panes/ContextList.vue'
import WorkspaceRouter from '@/components/panes/WorkspaceRouter.vue' 
import FloatingItemStack from '@/components/widgets/FloatingItemStack.vue'

// Logic extracted to Composable
const ui = useUIStore()
const { 
    flatLayout, 
    detailLayout, 
    handleLayoutUpdate, 
    toggleSidebar, 
    saveLayout, 
    resetLayout 
} = useDashboardLayout()

// Expose actions for the parent (App.vue)
defineExpose({ toggleSidebar, saveLayout, resetLayout })
</script>

<template>
  <div class="h-full w-full">
    <!-- Global Widget -->
    <FloatingItemStack />
    
    <!-- Main Layout -->
    <MasterDetailLayout 
        :layout="flatLayout"
        @update:layout="handleLayoutUpdate"
    >
        <!-- LEFT SIDEBAR -->
        <template #sidebar>
            <CategoryTree v-if="ui.sidebarMode === 'tags'" />
            <InboxList v-else-if="ui.sidebarMode === 'inbox'" />
            <ActionList v-else-if="ui.sidebarMode === 'actions'" />
        </template>

        <!-- MIDDLE LIST -->
        <template #list>
            <ContextList />
        </template>

        <!-- RIGHT DETAIL / ROUTER -->
        <template #detail>
            <WorkspaceRouter 
                :layout="detailLayout" 
                @update:layout="(sizes) => handleLayoutUpdate({ key: 'detail', sizes })" 
            />
        </template>
    </MasterDetailLayout>
  </div>
</template>