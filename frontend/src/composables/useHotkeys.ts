import { onKeyStroke } from '@vueuse/core'

interface HotkeyOptions {
  ctrl?: boolean;   // Require Ctrl (or Cmd on Mac)
  shift?: boolean;  // Require Shift
  prevent?: boolean; // Default true if not specified
  condition?: () => boolean; // Only execute if true
}

/**
 * Standardized hotkey handler using VueUse.
 * Handles cross-platform Ctrl/Cmd mapping and preventing defaults.
 */
export function useHotkeys(key: string | string[], handler: (e: KeyboardEvent) => void, options: HotkeyOptions = {}) {
  const { 
    ctrl = false, 
    shift = false, 
    prevent = true,
    condition = () => true 
  } = options

  return onKeyStroke(key, (e: KeyboardEvent) => {
    // 1. Check Modifiers
    if (ctrl && !(e.ctrlKey || e.metaKey)) return
    if (shift && !e.shiftKey) return
    
    // 2. Check Logic Condition
    if (!condition()) return

    // 3. Execute
    if (prevent) e.preventDefault()
    handler(e)
  })
}