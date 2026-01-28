import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'
import { createNotivue, push } from 'notivue'

import './style.css'
import 'notivue/notification.css'
import 'notivue/animations.css'

import App from './App.vue'

const app = createApp(App)
export const pinia = createPinia() // Export pinia for use in API interceptors

const notivue = createNotivue({
  position: 'bottom-right',
  limit: 5,
  enqueue: true,
  notifications: {
    global: {
      duration: 4000
    }
  }
})

// --- GLOBAL ERROR HANDLING ---
app.config.errorHandler = (err, instance, info) => {
  console.error('[Global Vue Error]:', err, info)
  
  // Prevent notification spam for minor/known interaction cancellations
  if (String(err).includes('cancelled') || String(err).includes('Aborted')) return

  push.error({
    title: 'System Error',
    message: 'An unexpected error occurred. Check console for details.',
    duration: 6000
  })
}

// Catch Unhandled Promise Rejections (e.g. async setup() failures)
window.addEventListener('unhandledrejection', (event) => {
  console.error('[Unhandled Promise]:', event.reason)
  push.error({
    title: 'Async Error',
    message: event.reason?.message || 'Operation failed unexpectedly',
    duration: 5000
  })
})

app.use(pinia)
app.use(VueQueryPlugin)
app.use(notivue)

app.mount('#app')