import { computed, watch, ref, onMounted } from 'vue'
import { useStorage } from '@vueuse/core'
import { useUIStore } from '@/stores/ui'
import { useNotifications } from '@/composables/useNotifications'
import { ViewMode } from '@/enums'

export function useDashboardLayout() {
    const ui = useUIStore()
    const { notify } = useNotifications()

    // --- STATE ---
    const STORAGE_KEY = 'matros-layout-v24'
    const defaultState = { sidebar: [20, 80], workspace: [35, 65], detail: [50, 50] }
    
    // Persistent Storage
    const layoutStorage = useStorage(STORAGE_KEY, defaultState)

    // Safety Init
    if (!layoutStorage.value || !Array.isArray(layoutStorage.value.sidebar)) {
        layoutStorage.value = JSON.parse(JSON.stringify(defaultState))
    }

    const previousWorkspaceLayout = ref<number[] | null>(null)

    // --- COMPUTEDS ---
    const flatLayout = computed(() => [
        layoutStorage.value.sidebar[0], 
        layoutStorage.value.sidebar[1], 
        layoutStorage.value.workspace[0], 
        layoutStorage.value.workspace[1]
    ])

    const detailLayout = computed(() => layoutStorage.value.detail || [50, 50])

    // --- ACTIONS ---
    const handleLayoutUpdate = ({ key, sizes }: { key: string, sizes: number[] }) => {
        if (key === 'sidebar') {
            layoutStorage.value.sidebar = sizes
        }
        else if (key === 'workspace') {
            // Only save workspace layout if NOT in zoomed/maximized mode
            if (!ui.isLayoutZoomed && sizes[0] > 2) {
                layoutStorage.value.workspace = sizes
            }
        }
        else if (key === 'detail') {
            layoutStorage.value.detail = sizes
        }
    }

    const toggleSidebar = () => {
        if (layoutStorage.value.sidebar[0] > 2) layoutStorage.value.sidebar = [0, 100]
        else layoutStorage.value.sidebar = [20, 80]
    }

    const saveLayout = () => {
        if (ui.isLayoutZoomed) {
            notify.warning("Cannot save layout while zoomed.")
            return
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(layoutStorage.value))
        notify.success('Window layout saved')
    }

    const resetLayout = () => {
        if(confirm("Reset Layout to defaults?")) {
            ui.setZoom(false)
            layoutStorage.value = JSON.parse(JSON.stringify(defaultState))
            notify.info('Layout reset')
        }
    }

    // --- WATCHERS (Business Logic) ---
    
    // 1. Maximize / Minimize Logic
    watch(() => ui.isLayoutZoomed, (zoomed) => {
        if (zoomed) {
            // Backup current state before zooming
            if (layoutStorage.value.workspace[0] > 2) {
                previousWorkspaceLayout.value = [...layoutStorage.value.workspace]
            }
            layoutStorage.value.workspace = [0, 100]
        } else {
            // Restore
            if (previousWorkspaceLayout.value && previousWorkspaceLayout.value[0] > 2) {
                layoutStorage.value.workspace = previousWorkspaceLayout.value
            } else {
                layoutStorage.value.workspace = [35, 65]
            }
        }
    })

    // 2. Auto-Fold on Forms
    const WIDE_VIEWS = [ViewMode.EDIT_ITEM, ViewMode.ADD_ITEM, ViewMode.CREATE_ACTION]
    watch(() => ui.rightPanelView, (newView, oldView) => {
        const isNowWide = WIDE_VIEWS.includes(newView as ViewMode)
        const wasWide = WIDE_VIEWS.includes(oldView as ViewMode)

        if (isNowWide && !wasWide) {
            ui.setZoom(true)
        } else if (!isNowWide && wasWide) {
            if (!ui.isContextListLocked) {
                ui.setZoom(false)
            }
        }
    })

    // 3. Init Check
    onMounted(() => {
        if (layoutStorage.value.workspace[0] < 2) {
            layoutStorage.value.workspace = [35, 65]
            ui.setZoom(false)
        }
    })

    return {
        flatLayout,
        detailLayout,
        handleLayoutUpdate,
        toggleSidebar,
        saveLayout,
        resetLayout
    }
}