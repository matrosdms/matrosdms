<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { client } from '@/api/client'
import { push } from 'notivue'
import { LogIn, Sparkles, UserPlus, ShieldAlert, X, FileText } from 'lucide-vue-next'
import { useMatrosData } from '@/composables/useMatrosData'
import { setBackendDisconnected } from '@/composables/useNetworkStatus'
import { sha256 } from '@/lib/utils'
import LegalText from '@/components/content/LegalText.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseSpinner from '@/components/ui/BaseSpinner.vue'

const auth = useAuthStore()
const { isBackendDisconnected } = useMatrosData()

const viewMode = ref('checking')
const loginForm = ref({ username: '', password: '' })
const setupForm = ref({ name: 'admin', firstname: 'Admin', email: '', password: '' })
const isLoading = ref(false)
const acceptedLegal = ref(false)
const acceptedSensitivePolicy = ref(false)
const showLegalModal = ref(false)

onMounted(async () => {
  try {
    const { data, error, response } = await client.GET("/api/auth/status")
    
    if (response.status === 503 || response.status === 504) {
        setBackendDisconnected(true)
        return
    }

    if (error || !response.ok) {
        viewMode.value = 'login' 
        return
    }

    if (data && (data as any).initialized === false) {
        viewMode.value = 'setup'
    } else {
        viewMode.value = 'login'
    }
  } catch (e) {
    console.error("Connection check exception:", e)
    setBackendDisconnected(true)
  }
})

const performSuccessRedirect = () => { /* Reactive */ }

const handleLogin = async () => {
  if (!loginForm.value.username) return push.warning('Please enter a username')
  isLoading.value = true
  
  try {
    const passwordHash = await sha256(loginForm.value.password)
    const payload = { username: loginForm.value.username, password: passwordHash }
    const { data, error, response } = await client.POST("/api/auth/login", { body: payload })
    
    if (response.status === 503 || response.status === 504) throw new Error("Backend Unavailable")

    if (error) {
        const msg = (error as any).message || (error as any).error || "Invalid Credentials"
        throw new Error(msg)
    }
    
    const safeData = data as any
    if (safeData && safeData.token && safeData.user) {
      auth.login(safeData.user, safeData.token)
      push.success(`Welcome back, ${safeData.user.firstname || safeData.user.name}!`)
      performSuccessRedirect()
    } else throw new Error("Invalid response from server")
  } catch (e: any) { 
      if (!isBackendDisconnected.value) {
          push.error(e.message) 
      }
  } finally { 
      isLoading.value = false 
  }
}

const handleSetup = async () => {
    if (!setupForm.value.name || !setupForm.value.password || !setupForm.value.email) return push.warning("All fields required")
    if (!acceptedLegal.value) return push.warning("You must read and accept the Legal Disclaimer")
    if (!acceptedSensitivePolicy.value) return push.warning("You must acknowledge the Sensitive Data Policy")
    
    isLoading.value = true
    try {
        const passwordHash = await sha256(setupForm.value.password)
        const payload = { ...setupForm.value, password: passwordHash, role: 'ADMIN' }
        const { error } = await client.POST("/api/auth/register" as any, { body: payload })
        if (error) throw new Error((error as any).message || "Setup failed")
        
        const loginPayload = { username: setupForm.value.name, password: passwordHash }
        const { data } = await client.POST("/api/auth/login", { body: loginPayload })
        
        const safeData = data as any
        if (safeData && safeData.token && safeData.user) {
            auth.login(safeData.user, safeData.token)
            push.success("System Initialized! Redirecting...")
            performSuccessRedirect()
        } else {
            viewMode.value = 'login'
            push.success("Account created. Please log in.")
        }
    } catch(e: any) { push.error("Setup failed: " + e.message) } finally { isLoading.value = false }
}

const acceptLegal = () => {
    showLegalModal.value = false
    acceptedLegal.value = true
}
</script>

<template>
  <div class="h-screen w-full flex items-center justify-center bg-muted/20 relative transition-colors duration-300">
    
    <div v-if="viewMode === 'checking'" class="flex flex-col items-center">
        <BaseSpinner :size="40" class="text-primary mb-4" />
        <span class="text-xs font-bold uppercase tracking-wider text-muted-foreground">Checking System Status...</span>
    </div>

    <BaseCard v-else class="w-full max-w-md animate-in fade-in slide-in-from-bottom-4 relative z-10 border-border shadow-xl">
      
      <template #header>
          <div class="text-center w-full pt-4">
              <h1 class="text-2xl font-bold text-foreground flex justify-center items-center gap-2">
                  MatrosDMS
                  <Sparkles v-if="viewMode === 'setup'" class="text-yellow-500" :size="24" />
              </h1>
              <p v-if="viewMode === 'login'" class="text-sm text-muted-foreground mt-1">Sign in to your account</p>
              <p v-else class="text-sm text-primary mt-1 font-medium bg-primary/10 py-1 px-2 rounded inline-block">Welcome! Let's set up your Admin account.</p>
          </div>
      </template>

      <div class="mt-2">
        <form v-if="viewMode === 'login'" @submit.prevent="handleLogin" class="flex flex-col gap-4">
            <BaseInput v-model="loginForm.username" label="Username" placeholder="e.g. admin" autofocus />
            <BaseInput v-model="loginForm.password" label="Password" type="password" placeholder="••••••" />
            
            <BaseButton type="submit" variant="default" class="w-full mt-2" :loading="isLoading">
                <LogIn class="mr-2" :size="18" /> Sign In
            </BaseButton>
        </form>

        <form v-if="viewMode === 'setup'" @submit.prevent="handleSetup" class="flex flex-col gap-4">
            <BaseInput v-model="setupForm.name" label="Create Username" placeholder="admin" autofocus />
            <BaseInput v-model="setupForm.firstname" label="Full Name" placeholder="Your Name" />
            <BaseInput v-model="setupForm.email" label="Email (Required)" type="email" placeholder="admin@local.host" />
            <BaseInput v-model="setupForm.password" label="Set Password" type="password" placeholder="Strong Password" />
            
            <div class="space-y-3 mt-2">
                <div class="flex items-center gap-2 bg-muted/50 p-3 rounded border border-border">
                    <input id="legalCheck" type="checkbox" :checked="acceptedLegal" disabled class="h-4 w-4 rounded border-input cursor-not-allowed opacity-60" />
                    <div class="text-xs text-muted-foreground leading-snug">
                        I agree to the 
                        <button type="button" @click="showLegalModal = true" class="underline text-primary hover:text-primary-hover font-bold focus:outline-none ml-0.5">Terms of Use & Disclaimer</button>.
                        <span v-if="!acceptedLegal" class="text-[10px] text-destructive block mt-0.5">(Click link to read & accept)</span>
                    </div>
                </div>

                <div class="flex items-start gap-2 bg-warning/10 p-3 rounded border border-warning/20">
                    <input id="sensitiveCheck" type="checkbox" v-model="acceptedSensitivePolicy" class="mt-1 h-4 w-4 text-warning focus:ring-warning border-warning rounded cursor-pointer shrink-0" />
                    <label for="sensitiveCheck" class="text-xs text-warning-foreground leading-snug cursor-pointer select-none font-medium">
                        I confirm that I will <strong>NOT</strong> upload unencrypted credentials, crypto wallet seeds/PINs, or illegal content.
                    </label>
                </div>
            </div>

            <BaseButton type="submit" variant="success" class="w-full mt-2" :loading="isLoading" :disabled="!acceptedLegal || !acceptedSensitivePolicy">
                <UserPlus class="mr-2" :size="18" /> Initialize System
            </BaseButton>
        </form>
      </div>

      <template #footer>
          <div class="w-full text-center text-xs text-muted-foreground pt-4">&copy; 2025 Schwehla IT</div>
      </template>
    </BaseCard>

    <!-- LEGAL MODAL -->
    <div v-if="showLegalModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
        <BaseCard class="max-w-2xl w-full max-h-[85vh] flex flex-col" no-padding>
            <template #header>
                <div class="flex justify-between items-center w-full">
                    <h3 class="text-sm font-bold flex items-center gap-2 uppercase tracking-wide">
                        <ShieldAlert :size="18" class="text-primary" /> Terms of Use
                    </h3>
                    <BaseButton variant="ghost" size="icon" @click="showLegalModal = false"><X :size="20" /></BaseButton>
                </div>
            </template>
            
            <div class="p-6 overflow-y-auto bg-muted/10">
                <LegalText />
            </div>

            <template #footer>
                <div class="w-full flex justify-between items-center py-2">
                    <div class="text-[10px] text-muted-foreground flex items-center gap-1">
                        <FileText :size="12"/> Please scroll and read carefully.
                    </div>
                    <BaseButton variant="default" @click="acceptLegal">
                        I Understand & Accept
                    </BaseButton>
                </div>
            </template>
        </BaseCard>
    </div>

  </div>
</template>