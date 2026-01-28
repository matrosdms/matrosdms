import { defineStore } from 'pinia'
import { useStorage } from '@vueuse/core'
import { watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { UserService } from '@/services/UserService'

export const usePreferencesStore = defineStore('preferences', () => {
const auth = useAuthStore()

// Default to Dark Mode as requested
const isDarkMode = useStorage('matros-ui-dark', true)

const sidebarCompact = useStorage('matros-ui-sidebar-compact', false)
const showNotifications = useStorage('matros-ui-notifications', true)

// Sync from Backend User Profile if available
watch(() => auth.currentUser, (user) => {
    if (user && user.preferences) {
        if (user.preferences['darkMode'] !== undefined) isDarkMode.value = !!user.preferences['darkMode'];
        if (user.preferences['sidebarCompact'] !== undefined) sidebarCompact.value = !!user.preferences['sidebarCompact'];
    }
}, { immediate: true })

const applyTheme = () => {
    const html = document.documentElement
    if (isDarkMode.value) {
        html.classList.add('dark')
    } else {
        html.classList.remove('dark')
    }
}

watch(isDarkMode, applyTheme)
onMounted(applyTheme)

async function syncPreference(key: string, value: any) {
    if (!auth.currentUser?.uuid) return
    try {
        const currentPrefs = auth.currentUser.preferences || {};
        const updated = { ...currentPrefs, [key]: value };
        
        await UserService.update(auth.currentUser.uuid, {
            email: auth.currentUser.email || '',
            firstname: auth.currentUser.firstname || '',
            name: auth.currentUser.name || '',
            role: auth.currentUser.role || 'USER',
            preferences: updated
        } as any); 
        
        auth.currentUser.preferences = updated;
    } catch (e) {
        console.error("Failed to sync preferences", e)
    }
}

function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    syncPreference('darkMode', isDarkMode.value)
}

return {
    isDarkMode,
    sidebarCompact,
    showNotifications,
    toggleDarkMode,
    applyTheme
}
})