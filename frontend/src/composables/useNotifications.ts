import { push } from 'notivue'

/**
 * Centralized notification handler.
 * Decouples the app from the specific toast library (Notivue).
 */
export function useNotifications() {
  const notify = {
    success: (title: string, message?: string) => {
      push.success({ title, message, duration: 4000 })
    },
    error: (title: string, error?: any) => {
      let message = typeof error === 'string' ? error : error?.message || 'Unknown error'
      // Clean up common prefixes if present
      message = message.replace(/^Error: /, '')
      push.error({ title, message, duration: 6000 })
    },
    info: (title: string, message?: string) => {
      push.info({ title, message })
    },
    warning: (title: string, message?: string) => {
      push.warning({ title, message, duration: 5000 })
    },
    promise: (message: string) => {
      return push.promise(message)
    }
  }

  return { notify }
}