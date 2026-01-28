<script setup>
import { Save, RotateCcw, Settings, PanelLeftClose, Inbox, Tags, CheckSquare, Info, Search, Sparkles } from 'lucide-vue-next'
import { useUIStore } from '@/stores/ui'
import { useInboxQueries } from '@/composables/queries/useInboxQueries'
import BaseButton from '@/components/ui/BaseButton.vue'

const ui = useUIStore()
defineEmits(['fold-sidebar', 'save-layout', 'reset-layout', 'switch-view'])

// Fetch inbox data to show count badge
const { inboxFiles } = useInboxQueries()

const setMode = (mode) => {
  ui.setSidebarMode(mode)
  ui.setView('dms')
}
</script>

<template>
<aside class="w-14 border-r border-border bg-background flex flex-col items-center py-3 justify-between flex-shrink-0 z-30 shadow-sm h-full transition-colors">
  <!-- TOP SECTION: Navigation -->
  <div class="flex flex-col gap-3 items-center w-full">
    <BaseButton variant="ghost" size="icon" @click="$emit('fold-sidebar')" title="Toggle Sidebar">
        <PanelLeftClose :size="20" />
    </BaseButton>

    <div class="w-8 h-px bg-border"></div>
    
    <BaseButton 
        variant="ghost" 
        size="icon" 
        @click="setMode('tags')" 
        :class="ui.currentView === 'dms' && ui.sidebarMode === 'tags' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
        title="Tags & Categories"
    >
        <Tags :size="22" />
    </BaseButton>

    <div class="relative">
        <BaseButton 
            variant="ghost" 
            size="icon" 
            @click="setMode('inbox')" 
            :class="ui.currentView === 'dms' && ui.sidebarMode === 'inbox' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
            title="Inbox"
        >
            <Inbox :size="22" />
        </BaseButton>
        <!-- Inbox Count Badge -->
        <span v-if="inboxFiles.length > 0" class="absolute top-0 right-0 w-4 h-4 bg-blue-600 text-white text-[9px] font-bold flex items-center justify-center rounded-full border border-white dark:border-gray-900 pointer-events-none">
            {{ inboxFiles.length }}
        </span>
    </div>

    <BaseButton 
        variant="ghost" 
        size="icon" 
        @click="setMode('actions')" 
        :class="ui.currentView === 'dms' && ui.sidebarMode === 'actions' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
        title="Tasks / Actions"
    >
        <CheckSquare :size="22" />
    </BaseButton>

    <div class="w-8 h-px bg-border mt-2"></div>

    <!-- TOOLS BUTTONS -->
    <BaseButton 
        variant="ghost" 
        size="icon" 
        @click="$emit('switch-view', 'search')" 
        :class="ui.currentView === 'search' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
        title="Advanced Search"
    >
        <Search :size="22" />
    </BaseButton>

    <BaseButton 
        variant="ghost" 
        size="icon" 
        @click="$emit('switch-view', 'ai')" 
        :class="ui.currentView === 'ai' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
        title="AI Assistant"
    >
        <Sparkles :size="22" />
    </BaseButton>

    <div class="w-8 h-px bg-border mt-2"></div>

    <!-- SAVE / RESET GROUP -->
    <div class="flex flex-col gap-2 bg-muted/30 p-1.5 rounded-lg border border-border">
        <BaseButton variant="ghost" size="iconSm" class="text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/30" @click="$emit('save-layout')" title="Save Window Layout">
            <Save :size="18" />
        </BaseButton>
        <BaseButton variant="ghost" size="iconSm" class="text-orange-600 hover:bg-orange-50 dark:hover:bg-orange-900/30" @click="$emit('reset-layout')" title="Reset Layout">
            <RotateCcw :size="18" />
        </BaseButton>
    </div>
  </div>

  <!-- BOTTOM SECTION: Meta -->
  <div class="flex flex-col gap-4 items-center w-full pb-2">
    <BaseButton 
        variant="ghost" 
        size="icon" 
        @click="$emit('switch-view', 'about')" 
        :class="ui.currentView === 'about' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
        title="System Info"
    >
        <Info :size="20" />
    </BaseButton>

    <BaseButton 
        variant="ghost" 
        size="icon" 
        @click="$emit('switch-view', 'settings')" 
        :class="ui.currentView === 'settings' ? 'bg-primary/10 text-primary' : 'text-muted-foreground'" 
        title="Settings"
    >
        <Settings :size="20" />
    </BaseButton>
  </div>
</aside>
</template>