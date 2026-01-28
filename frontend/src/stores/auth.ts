import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUIStore } from '@/stores/ui'
import { useStorage } from '@vueuse/core'
import { UserService } from '@/services/UserService'
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

  const isAuthenticated = computed(() => !!currentUser.value)
  const isAdmin = computed(() => !!currentUser.value)
  
  const isConfigLoaded = ref(false)

  // Login now accepts both the User object AND the Token
  function login(userObj: User, jwtToken?: string) {
    currentUser.value = userObj
    if (jwtToken) {
        token.value = jwtToken
    }
    ui.addLog(`User logged in: ${userObj.name}`, 'success')
  }

  function logout() {
    currentUser.value = null
    token.value = null
    ui.addLog('User logged out.', 'info')
    // Removed window.location.reload() to prevent "Booting System" flash
    // State is reactive, so App.vue will switch to LoginView immediately
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
        ui.addLog("Session expired or invalid.", 'error')
    }
  }

  async function loadConfig() {
    await new Promise(r => setTimeout(r, 100))
    
    // Robustness: Only validate if we have a user
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
      isAuthenticated, 
      isAdmin, 
      isConfigLoaded, 
      login, 
      logout, 
      loadConfig, 
      validateSession 
  }
})