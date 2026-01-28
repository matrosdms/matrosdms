import { watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useActionStore } from '@/stores/action'
import { useConfigStore } from '@/stores/config'
import { useMatrosData } from '@/composables/useMatrosData'

export function useAppBoot() {
    const auth = useAuthStore()
    const actionStore = useActionStore()
    const configStore = useConfigStore()
    const { isBackendDisconnected, checkConnection } = useMatrosData()

    const bootSystem = () => {
        onMounted(async () => { 
            await auth.loadConfig()
        })

        watch(() => auth.isAuthenticated, async (isAuth) => {
            if (isAuth) {
                // Parallel Initialization:
                // 1. Load Actions (Tasks)
                // 2. Load Definitions (Metadata)
                // Note: Category Trees are now lazy-loaded by components via Vue Query
                
                await Promise.all([
                    actionStore.fetchActions(),
                    configStore.loadDefinitions()
                ])
                
                console.log(`[Boot] System initialized.`)
            }
        }, { immediate: true })
    }

    const onRetryConnection = async () => {
        await checkConnection()
        if (!isBackendDisconnected.value) {
            await auth.loadConfig()
        }
    }

    return {
        isBackendDisconnected,
        bootSystem,
        onRetryConnection
    }
}