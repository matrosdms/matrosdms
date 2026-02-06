import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUIStore } from '@/stores/ui'
import { useStorage } from '@vueuse/core'
import { UserService } from '@/services/UserService'
import { client } from '@/api/client'
import type { components } from '@/types/schema'

type User = components['schemas']['MUser'];

export const useAuthStore = defineStore('auth', () => {
  const ui = useUIStore() 
  
  const currentUser = useStorage<User | null>('matros-user-v1', null, localStorage, {
      serializer: {
          read: (v) => v ? JSON.parse(v) : null,
          write: (v) => JSON.stringify(v),
      },
  })

  const token = useStorage<string | null>('matros-token-v1', null)
  const refreshToken = useStorage<string | null>('matros-refresh-token-v1', null)
  
  // Track if a refresh is in progress to avoid concurrent refreshes
  const isRefreshing = ref(false)

  const isAuthenticated = computed(() => !!currentUser.value)
  const isAdmin = computed(() => !!currentUser.value)
  
  const isConfigLoaded = ref(false)

  // Login now accepts both the User object AND the Token + Refresh Token
  function login(userObj: User, jwtToken?: string, jwtRefreshToken?: string) {
    currentUser.value = userObj
    if (jwtToken) {
        token.value = jwtToken
    }
    if (jwtRefreshToken) {
        refreshToken.value = jwtRefreshToken
    }
    ui.addLog(`User logged in: ${userObj.name}`, 'success')
  }

  // Logout now notifies the backend to revoke the refresh token
  async function logout() {
    try {
        if (refreshToken.value) {
            await client.POST("/api/auth/logout", {
                body: { refreshToken: refreshToken.value }
            })
        }
    } catch (e) {
        console.warn("Logout API call failed", e)
    } finally {
        // Always clear local state even if API fails
        currentUser.value = null
        token.value = null
        refreshToken.value = null
        ui.addLog('User logged out.', 'info')
    }
  }
  
  // Update tokens after a refresh
  function updateTokens(accessToken: string, newRefreshToken: string) {
    token.value = accessToken
    refreshToken.value = newRefreshToken
  }

  async function validateSession() {
    if (!currentUser.value?.uuid) return
    try {
        // This call will now use the Token via the client interceptor
        const user = await UserService.getById(currentUser.value.uuid)
        if (user) {
            currentUser.value = user
        } else {
            throw new Error("User not found")
        }
    } catch (e) {
        console.error("Session validation failed:", e)
        // If validation fails (401/403), clear local storage immediately
        currentUser.value = null
        token.value = null
        refreshToken.value = null
        ui.addLog("Session expired or invalid.", 'error')
    }
  }

  async function loadConfig() {
    // Check session on load
    if (currentUser.value) {
        await validateSession()
    }
    
    isConfigLoaded.value = true
    if (isAuthenticated.value) {
         ui.addLog('System initialized.', 'success')
    }
  }

  return { 
      currentUser, 
      token,
      refreshToken,
      isRefreshing,
      isAuthenticated, 
      isAdmin, 
      isConfigLoaded, 
      login,
      logout,
      updateTokens,
      loadConfig, 
      validateSession 
  }
})