<script setup lang="ts">
import { User, ChevronDown, LogOut, Settings, ShieldCheck } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import AppPopover from '@/components/ui/AppPopover.vue'
import BaseButton from '@/components/ui/BaseButton.vue'

const auth = useAuthStore()
const ui = useUIStore()

const openProfile = (close: () => void) => {
    ui.setView('profile')
    close()
}

const openAdminSettings = (close: () => void) => {
    ui.setView('settings')
    close()
}
</script>

<template>
  <AppPopover align="right" width="w-64">
    <!-- TRIGGER -->
    <template #trigger="{ isOpen }">
        <BaseButton variant="ghost" class="px-2 h-auto py-1.5 gap-2 group border border-transparent hover:border-border">
            <div class="w-8 h-8 rounded-full bg-primary/10 text-primary flex items-center justify-center border border-primary/20 shadow-sm group-hover:bg-background group-hover:border-primary/50 transition-colors">
                <User :size="16" />
            </div>
            <div class="flex flex-col items-start hidden md:flex">
                <span class="text-xs font-bold text-foreground leading-tight">{{ auth.currentUser?.firstname || auth.currentUser?.name }}</span>
                <span class="text-[9px] text-muted-foreground uppercase tracking-wide">{{ auth.currentUser?.role || 'User' }}</span>
            </div>
            <ChevronDown :size="14" class="text-muted-foreground transition-transform duration-200" :class="{'rotate-180': isOpen}" />
        </BaseButton>
    </template>

    <!-- CONTENT -->
    <template #content="{ close }">
        <div class="p-4 border-b border-border bg-muted/30">
            <div class="text-sm font-bold text-foreground">{{ auth.currentUser?.name }}</div>
            <div class="text-xs text-muted-foreground truncate">{{ auth.currentUser?.email || 'No email' }}</div>
        </div>
        
        <div class="p-1.5 space-y-1">
            <BaseButton variant="ghost" class="w-full justify-start h-8 text-xs" @click="openProfile(close)">
                <User :size="14" class="mr-2" /> Profile & Preferences
            </BaseButton>
            
            <BaseButton v-if="auth.currentUser?.role === 'ADMIN'" variant="ghost" class="w-full justify-start h-8 text-xs text-purple-600 hover:text-purple-700 hover:bg-purple-50 dark:hover:bg-purple-900/30" @click="openAdminSettings(close)">
                <ShieldCheck :size="14" class="mr-2" /> System Administration
            </BaseButton>
        </div>

        <div class="border-t border-border p-1.5">
            <BaseButton variant="ghost" class="w-full justify-start h-8 text-xs text-destructive hover:bg-destructive/10" @click="auth.logout()">
                <LogOut :size="14" class="mr-2" /> Sign Out
            </BaseButton>
        </div>
    </template>
  </AppPopover>
</template>