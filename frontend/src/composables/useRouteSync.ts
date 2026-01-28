import { watch, onMounted, onUnmounted } from 'vue'
import { useUIStore } from '@/stores/ui'
import { useDmsStore } from '@/stores/dms'
import { ViewMode } from '@/enums'

export function useRouteSync() {
    const ui = useUIStore()
    const dms = useDmsStore()
    
    let isPopState = false

    // 1. SERIALIZE: Store -> URL
    const updateUrl = () => {
        if (isPopState) return

        const params = new URLSearchParams()
        
        // Main View & Sidebar
        if (ui.currentView !== 'dms') params.set('view', ui.currentView)
        if (ui.sidebarMode !== 'tags') params.set('sidebar', ui.sidebarMode)

        // Context Selection
        if (dms.selectedContext?.uuid) {
            params.set('context', dms.selectedContext.uuid)
        }

        // Right Panel (Forms/Details)
        if (ui.rightPanelView && ui.rightPanelView !== ViewMode.DETAILS) {
            params.set('panel', ui.rightPanelView)
            
            // Serialize Panel Data (Ids, etc)
            if (ui.panelData) {
                if (ui.panelData.id) params.set('id', ui.panelData.id)
                if (ui.panelData.initialData?.uuid) params.set('entityId', ui.panelData.initialData.uuid)
                if (ui.panelData.source) params.set('source', ui.panelData.source)
            }
        }

        const newUrl = `${window.location.pathname}?${params.toString()}`
        if (window.location.search !== `?${params.toString()}`) {
            window.history.pushState(null, '', newUrl)
        }
    }

    // 2. RESTORE: URL -> Store
    const restoreFromUrl = async () => {
        const params = new URLSearchParams(window.location.search)
        
        isPopState = true
        try {
            // View & Sidebar
            const view = params.get('view') as any
            if (view) ui.setView(view)
            
            const sidebar = params.get('sidebar') as any
            if (sidebar) ui.setSidebarMode(sidebar)

            // Context (Async Fetch needed)
            const contextId = params.get('context')
            if (contextId && dms.selectedContext?.uuid !== contextId) {
                try {
                    // Logic to restore context could go here
                } catch(e) { console.warn("Could not restore context", e) }
            } else if (!contextId) {
                dms.setSelectedContext(null)
            }

            // Panel / Forms
            const panel = params.get('panel')
            if (panel) {
                const data: any = {}
                if (params.get('id')) data.id = params.get('id')
                if (params.get('entityId')) data.initialData = { uuid: params.get('entityId') }
                if (params.get('source')) data.source = params.get('source')
                
                ui.setRightPanel(panel, data)
            } else {
                ui.setRightPanelView(ViewMode.DETAILS)
            }

        } finally {
            setTimeout(() => { isPopState = false }, 100)
        }
    }

    // Setup Listeners
    onMounted(() => {
        restoreFromUrl()
        window.addEventListener('popstate', restoreFromUrl)
    })

    onUnmounted(() => {
        window.removeEventListener('popstate', restoreFromUrl)
    })

    // Watchers for State Changes
    watch(
        () => [
            ui.currentView, 
            ui.sidebarMode, 
            ui.rightPanelView, 
            ui.panelData, 
            dms.selectedContext?.uuid
        ], 
        updateUrl,
        { deep: true }
    )
}