import { defineStore } from 'pinia'
import { useStorage } from '@vueuse/core'
import { useUIStore } from '@/stores/ui'
import type { components } from '@/types/schema'

type MItem = components['schemas']['MItem'];

/**
 * Dedicated Store for "Item Stack" / Clipboard operations.
 * Uses localStorage persistence to survive 409 refreshes.
 */
export const useClipboardStore = defineStore('clipboard', () => {
    const ui = useUIStore()

    // Persist stack in localStorage
    const stack = useStorage<MItem[]>('matros-clipboard-v1', [])

    function addToStack(item: MItem) {
        if (!item || !item.uuid) return
        
        // Prevent duplicates
        if (!stack.value.find(i => i.uuid === item.uuid)) {
            stack.value.push(item)
            ui.addLog(`Added '${item.name}' to stack`, 'info')
        }
    }

    function removeFromStack(uuid: string) {
        const initialLen = stack.value.length
        stack.value = stack.value.filter(i => i.uuid !== uuid)
        if (stack.value.length < initialLen) {
            // Optional: Log removal if needed
        }
    }

    function clearStack() {
        stack.value = []
        ui.addLog('Item stack cleared', 'debug')
    }

    function isInStack(uuid: string): boolean {
        return stack.value.some(i => i.uuid === uuid)
    }

    return {
        stack,
        addToStack,
        removeFromStack,
        clearStack,
        isInStack
    }
})