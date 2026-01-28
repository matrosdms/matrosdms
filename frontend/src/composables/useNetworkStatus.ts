import { ref } from 'vue'
import { client } from '@/api/client'
import { useUIStore } from '@/stores/ui'
import { EArchivedState } from '@/enums'

// Singleton State
const isBackendDisconnected = ref(false)

// EXPORTED HELPER: Allows client.ts to toggle this state directly
export const setBackendDisconnected = (status: boolean) => {
    isBackendDisconnected.value = status
}

export function useNetworkStatus() {
  const ui = useUIStore()

  const checkConnection = async () => {
    try {
      // Simple ping to see if we can reach the API
      await client.GET("/api/auth/status") 
      isBackendDisconnected.value = false
    } catch (e) {
      isBackendDisconnected.value = true
    }
  }

  const handleQueryError = (err: any, contextName: string) => {
    console.error(`Error in ${contextName}:`, err)
    
    const msg = err.message || 'Unknown Error'
    
    // Check for Network Errors (Fetch failure)
    if (msg.includes('Failed to fetch') || msg.includes('Network Error')) {
      if (!isBackendDisconnected.value) {
          ui.addLog(`Backend connection lost (${contextName})`, 'error')
      }
      isBackendDisconnected.value = true
    } else {
        ui.addLog(`Error: ${msg}`, 'error')
    }
  }

  return {
    isBackendDisconnected,
    checkConnection,
    handleQueryError
  }
}