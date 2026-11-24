<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUIStore } from '@/stores/ui'
import { usePreferencesStore } from '@/stores/preferences'
import { client } from '@/api/client'
import { sha256, formatDateTime } from '@/lib/utils'
import { push } from 'notivue'
import { ArrowLeft, User, Moon, Sun, Bell, Shield, LogOut, Key, Eye, EyeOff, ListChecks, CalendarDays, RefreshCw, Unlink, Link2, AlertTriangle } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { GoogleIntegrationService, type GoogleStatus } from '@/services/GoogleIntegrationService'

const auth = useAuthStore()
const ui = useUIStore()
const prefs = usePreferencesStore()

// --- Google Tasks & Calendar ---
const google = ref<GoogleStatus | null>(null)
const googleBusy = ref(false)
let googleRefreshTimer: ReturnType<typeof setTimeout> | undefined

const loadGoogle = async () => {
  try {
    google.value = await GoogleIntegrationService.getStatus()
  } catch (e: any) {
    console.warn('[Google] status unavailable', e)
  }
}

// The first push runs in the background (scheduler polls every few seconds), so
// look again shortly to show the counts going up
const refreshGoogleSoon = () => {
  clearTimeout(googleRefreshTimer)
  googleRefreshTimer = setTimeout(loadGoogle, 15000)
}

const connectGoogle = async (options: { calendar?: boolean } = {}) => {
  googleBusy.value = true
  try {
    window.location.href = await GoogleIntegrationService.connect(options)
  } catch (e: any) {
    push.error(e.message || 'Could not start Google sign-in')
    googleBusy.value = false
  }
}

const toggleGoogle = async (target: 'tasks' | 'calendar') => {
  const s = google.value
  if (!s || googleBusy.value) return
  // Switching on something Google has not allowed yet: ask Google for just that permission.
  // Connecting asks for Tasks only, so this is how the calendar gets its permission.
  const turningOn = target === 'tasks' ? !s.tasksEnabled : !s.calendarEnabled
  const granted = target === 'tasks' ? s.tasksGranted : s.calendarGranted
  if (turningOn && !granted) {
    await connectGoogle({ calendar: target === 'calendar' })
    return
  }
  googleBusy.value = true
  try {
    google.value = await GoogleIntegrationService.updateSettings({
      tasksEnabled: target === 'tasks' ? !s.tasksEnabled : !!s.tasksEnabled,
      calendarEnabled: target === 'calendar' ? !s.calendarEnabled : !!s.calendarEnabled
    })
    refreshGoogleSoon()
  } catch (e: any) {
    push.error(e.message || 'Could not change Google sync')
  } finally {
    googleBusy.value = false
  }
}

const resyncGoogle = async () => {
  googleBusy.value = true
  try {
    const n = await GoogleIntegrationService.resync()
    push.success(`${n} task${n === 1 ? '' : 's'} queued for Google sync`)
    refreshGoogleSoon()
  } catch (e: any) {
    push.error(e.message || 'Could not start sync')
  } finally {
    googleBusy.value = false
  }
}

const disconnectGoogle = async () => {
  googleBusy.value = true
  try {
    google.value = await GoogleIntegrationService.disconnect()
    push.info('Google disconnected. Tasks already pushed stay in Google.')
  } catch (e: any) {
    push.error(e.message || 'Could not disconnect')
  } finally {
    googleBusy.value = false
  }
}

const formatTime = (val: any) => formatDateTime(val, '')

onMounted(() => {
  // Back from Google's consent screen: ?view=profile&google=connected|error&reason=...
  const params = new URLSearchParams(window.location.search)
  const outcome = params.get('google')
  if (outcome) {
    if (outcome === 'connected') {
      push.success('Google account connected. Your tasks are being pushed in the background.')
      refreshGoogleSoon()
    } else {
      const reason = params.get('reason')
      push.error(reason === 'access_denied' ? 'Google sign-in was cancelled.' : `Google sign-in failed: ${reason || 'unknown error'}`)
    }
    params.delete('google')
    params.delete('reason')
    window.history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`)
  }
  loadGoogle()
})

onUnmounted(() => clearTimeout(googleRefreshTimer))

// Change Password Form
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const showPasswords = ref(false)
const isChangingPassword = ref(false)

const goBack = () => {
    ui.setView('dms')
}

const handleChangePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    return push.warning('Please fill in all password fields')
  }
  
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    return push.warning('New passwords do not match')
  }
  
  if (passwordForm.value.newPassword.length < 6) {
    return push.warning('New password must be at least 6 characters')
  }
  
  isChangingPassword.value = true
  const promise = push.promise('Changing password...')
  
  try {
    const oldPasswordHash = await sha256(passwordForm.value.oldPassword)
    const newPasswordHash = await sha256(passwordForm.value.newPassword)
    
    const { error, response } = await client.POST('/api/auth/change-password', {
      body: {
        oldPassword: oldPasswordHash,
        newPassword: newPasswordHash
      }
    })
    
    if (response?.status === 401 || response?.status === 403) {
      throw new Error('Current password is incorrect')
    }
    
    if (error) {
      throw new Error((error as any).message || 'Failed to change password')
    }
    
    // Clear form
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    promise.resolve('Password changed successfully!')
  } catch (e: any) {
    promise.reject(e.message || 'Failed to change password')
  } finally {
    isChangingPassword.value = false
  }
}
</script>

<template>
  <div class="h-full w-full bg-gray-50 dark:bg-gray-900 flex flex-col overflow-hidden font-sans transition-colors duration-300">
    
    <!-- Header -->
    <div class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shrink-0 transition-colors duration-300">
        <div class="max-w-3xl mx-auto px-6 py-6">
            <div class="flex items-center gap-4">
                <button @click="goBack" class="p-2 rounded-full bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors text-gray-600 dark:text-gray-300">
                    <ArrowLeft :size="20" />
                </button>
                <div>
                    <h1 class="text-2xl font-extrabold text-gray-900 dark:text-white tracking-tight">My Profile</h1>
                    <p class="text-sm text-gray-500 dark:text-gray-400">Personal settings and preferences</p>
                </div>
            </div>
        </div>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto">
        <div class="max-w-3xl mx-auto w-full p-6 space-y-6">
            
            <!-- User Info Card -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm p-6 flex items-start gap-6 transition-colors duration-300">
                <div class="w-16 h-16 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0 text-xl font-bold border border-blue-200 dark:border-blue-800">
                    <User :size="32" />
                </div>
                <div class="flex-1">
                    <h2 class="text-lg font-bold text-gray-800 dark:text-white">{{ auth.currentUser?.firstname || 'User' }}</h2>
                    <div class="text-sm text-gray-500 dark:text-gray-400 font-mono mb-2">@{{ auth.currentUser?.name }}</div>
                    
                    <div class="flex gap-2 mb-4">
                        <span class="px-2 py-0.5 rounded bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 text-xs font-bold border border-gray-200 dark:border-gray-600">
                            {{ auth.currentUser?.email || 'No Email' }}
                        </span>
                        <span class="px-2 py-0.5 rounded bg-purple-50 dark:bg-purple-900/20 text-purple-700 dark:text-purple-300 text-xs font-bold border border-purple-100 dark:border-purple-800 flex items-center gap-1">
                            <Shield :size="10" /> {{ auth.currentUser?.role }}
                        </span>
                    </div>
                </div>
                <!-- Updated Button -->
                <BaseButton variant="destructive" @click="auth.logout">
                    <LogOut :size="14" class="mr-2" /> Sign Out
                </BaseButton>
            </div>

            <!-- Appearance Settings -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden transition-colors duration-300">
                <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50">
                    <h3 class="font-bold text-gray-700 dark:text-gray-200 text-sm uppercase tracking-wide">Appearance & UI</h3>
                </div>
                
                <div class="p-6 space-y-6">
                    <!-- Dark Mode Toggle -->
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                            <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 transition-colors">
                                <Moon v-if="prefs.isDarkMode" :size="20" />
                                <Sun v-else :size="20" />
                            </div>
                            <div>
                                <div class="font-bold text-gray-800 dark:text-gray-100 text-sm">Dark Mode</div>
                                <div class="text-xs text-gray-500 dark:text-gray-400">Switch between light and dark themes.</div>
                            </div>
                        </div>
                        <button 
                            @click="prefs.toggleDarkMode"
                            class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
                            :class="prefs.isDarkMode ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'"
                        >
                            <span 
                                class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
                                :class="prefs.isDarkMode ? 'translate-x-6' : 'translate-x-1'"
                            />
                        </button>
                    </div>

                    <!-- Compact Sidebar -->
                    <div class="flex items-center justify-between">
                        <div class="flex items-center gap-3">
                            <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300">
                                <Bell :size="20" />
                            </div>
                            <div>
                                <div class="font-bold text-gray-800 dark:text-gray-100 text-sm">Show Notifications</div>
                                <div class="text-xs text-gray-500 dark:text-gray-400">Enable UI toast popups.</div>
                            </div>
                        </div>
                        <button 
                            @click="prefs.showNotifications = !prefs.showNotifications"
                            class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
                            :class="prefs.showNotifications ? 'bg-green-500' : 'bg-gray-200 dark:bg-gray-700'"
                        >
                            <span 
                                class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
                                :class="prefs.showNotifications ? 'translate-x-6' : 'translate-x-1'"
                            />
                        </button>
                    </div>
                </div>
            </div>

            <!-- Google Tasks & Calendar -->
            <div v-if="google" class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden transition-colors duration-300">
                <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50 flex items-center justify-between">
                    <h3 class="font-bold text-gray-700 dark:text-gray-200 text-sm uppercase tracking-wide flex items-center gap-2">
                        <CalendarDays :size="16" /> Google Tasks & Calendar
                    </h3>
                    <span v-if="google.connected" class="text-xs text-gray-500 dark:text-gray-400 font-mono">{{ google.email }}</span>
                </div>

                <div class="p-6 space-y-5">
                    <!-- Server has no OAuth client configured -->
                    <div v-if="!google.configured" class="text-sm text-gray-600 dark:text-gray-300">
                        Pushing your tasks to Google is off on this server. An administrator enables it by
                        setting <code class="text-xs bg-gray-100 dark:bg-gray-700 px-1 rounded">MATROS_GOOGLE_CLIENT_ID</code> and
                        <code class="text-xs bg-gray-100 dark:bg-gray-700 px-1 rounded">MATROS_GOOGLE_CLIENT_SECRET</code>
                        (see <span class="font-mono text-xs">doc/google-sync.md</span>).
                    </div>

                    <!-- Configured, not connected -->
                    <div v-else-if="!google.connected" class="flex items-center justify-between gap-4">
                        <div class="text-sm text-gray-600 dark:text-gray-300">
                            Push tasks assigned to you into Google Tasks. They show up in the Tasks app, in Gmail and on their due date in Google Calendar.
                            Ticking a task off or moving its date in Google comes back to MatrosDMS. Everything runs in the background.
                            <span class="block text-xs text-gray-500 dark:text-gray-400 mt-1">
                                Sent to Google: task title, description, due date, and the linked document and context names.
                            </span>
                        </div>
                        <BaseButton variant="default" :disabled="googleBusy" :loading="googleBusy" @click="connectGoogle()" class="shrink-0">
                            <Link2 :size="14" class="mr-2" /> Connect Google
                        </BaseButton>
                    </div>

                    <!-- Connected -->
                    <template v-else>
                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-3">
                                <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300"><ListChecks :size="20" /></div>
                                <div>
                                    <div class="font-bold text-gray-800 dark:text-gray-100 text-sm">Google Tasks</div>
                                    <div class="text-xs text-gray-500 dark:text-gray-400">
                                        <template v-if="google.tasksGranted">List "MatrosDMS" · {{ google.syncedTasks }} synced</template>
                                        <template v-else>Not allowed in Google yet. Switch it on to ask Google for permission.</template>
                                    </div>
                                </div>
                            </div>
                            <button
                                @click="toggleGoogle('tasks')"
                                :disabled="googleBusy"
                                class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900 disabled:opacity-50"
                                :class="google.tasksEnabled ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'"
                                :aria-pressed="!!google.tasksEnabled"
                                aria-label="Sync to Google Tasks"
                            >
                                <span class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform" :class="google.tasksEnabled ? 'translate-x-6' : 'translate-x-1'" />
                            </button>
                        </div>

                        <div class="flex items-center justify-between">
                            <div class="flex items-center gap-3">
                                <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300"><CalendarDays :size="20" /></div>
                                <div>
                                    <div class="font-bold text-gray-800 dark:text-gray-100 text-sm">Extra "MatrosDMS" calendar <span class="font-normal text-gray-500 dark:text-gray-400">(optional)</span></div>
                                    <div class="text-xs text-gray-500 dark:text-gray-400">
                                        <template v-if="google.calendarEnabled">Open tasks with a due date as events · {{ google.syncedEvents }} synced</template>
                                        <template v-else>Google Calendar already shows your tasks. Turn this on for other calendar apps, sharing or event reminders.</template>
                                    </div>
                                </div>
                            </div>
                            <button
                                @click="toggleGoogle('calendar')"
                                :disabled="googleBusy"
                                class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900 disabled:opacity-50"
                                :class="google.calendarEnabled ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'"
                                :aria-pressed="!!google.calendarEnabled"
                                aria-label="Sync to Google Calendar"
                            >
                                <span class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform" :class="google.calendarEnabled ? 'translate-x-6' : 'translate-x-1'" />
                            </button>
                        </div>

                        <div v-if="google.lastError" class="flex gap-2 rounded-lg border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-900/20 p-3 text-xs text-amber-800 dark:text-amber-300">
                            <AlertTriangle :size="14" class="shrink-0 mt-0.5" />
                            <div>
                                <div class="font-bold">Last sync problem<span v-if="google.lastErrorAt" class="font-normal"> · {{ formatTime(google.lastErrorAt) }}</span></div>
                                <div class="break-words">{{ google.lastError }}</div>
                            </div>
                        </div>

                        <div class="flex items-center justify-between gap-2 pt-1">
                            <span class="text-xs text-gray-500 dark:text-gray-400">
                                <template v-if="google.lastPulledAt">Checked Google for changes: {{ formatTime(google.lastPulledAt) }}</template>
                            </span>
                            <div class="flex items-center gap-2">
                            <BaseButton variant="outline" :disabled="googleBusy" @click="resyncGoogle">
                                <RefreshCw :size="14" class="mr-2" /> Sync now
                            </BaseButton>
                            <BaseButton variant="outline" :disabled="googleBusy" @click="disconnectGoogle">
                                <Unlink :size="14" class="mr-2" /> Disconnect
                            </BaseButton>
                            </div>
                        </div>
                    </template>

                    <!-- Disconnected after a revoke / expiry: offer to reconnect, keep the error visible -->
                    <div v-if="google.configured && !google.connected && google.lastError" class="flex gap-2 rounded-lg border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-900/20 p-3 text-xs text-amber-800 dark:text-amber-300">
                        <AlertTriangle :size="14" class="shrink-0 mt-0.5" />
                        <div class="break-words">{{ google.lastError }}</div>
                    </div>
                </div>
            </div>

            <!-- Change Password Section -->
            <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 shadow-sm overflow-hidden transition-colors duration-300">
                <div class="px-6 py-4 border-b border-gray-100 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-800/50">
                    <h3 class="font-bold text-gray-700 dark:text-gray-200 text-sm uppercase tracking-wide flex items-center gap-2">
                        <Key :size="16" /> Change Password
                    </h3>
                </div>
                
                <form @submit.prevent="handleChangePassword" class="p-6 space-y-4">
                    <div class="relative">
                        <BaseInput 
                            v-model="passwordForm.oldPassword" 
                            :type="showPasswords ? 'text' : 'password'" 
                            label="Current Password" 
                            placeholder="Enter your current password"
                            autocomplete="current-password"
                        />
                    </div>
                    
                    <div class="relative">
                        <BaseInput 
                            v-model="passwordForm.newPassword" 
                            :type="showPasswords ? 'text' : 'password'" 
                            label="New Password" 
                            placeholder="Enter new password (min. 6 characters)"
                            autocomplete="new-password"
                        />
                    </div>
                    
                    <div class="relative">
                        <BaseInput 
                            v-model="passwordForm.confirmPassword" 
                            :type="showPasswords ? 'text' : 'password'" 
                            label="Confirm New Password" 
                            placeholder="Confirm your new password"
                            autocomplete="new-password"
                        />
                    </div>
                    
                    <div class="flex items-center justify-between pt-2">
                        <button 
                            type="button"
                            @click="showPasswords = !showPasswords"
                            class="text-xs text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 flex items-center gap-1 transition-colors"
                        >
                            <Eye v-if="!showPasswords" :size="14" />
                            <EyeOff v-else :size="14" />
                            {{ showPasswords ? 'Hide' : 'Show' }} passwords
                        </button>
                        
                        <BaseButton 
                            type="submit" 
                            variant="default" 
                            :disabled="isChangingPassword || !passwordForm.oldPassword || !passwordForm.newPassword"
                            :loading="isChangingPassword"
                        >
                            <Key :size="14" class="mr-2" /> Update Password
                        </BaseButton>
                    </div>
                </form>
            </div>

            <div class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 text-xs text-blue-800 dark:text-blue-300">
                <strong>Note:</strong> These preferences are currently stored in your browser (LocalStorage).
            </div>

        </div>
    </div>
  </div>
</template>