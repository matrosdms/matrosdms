import { type Ref } from 'vue'

interface UseListNavigationOptions {
    /** The total number of items in the list */
    listLength: Ref<number>;
    /** The currently active index (v-model) */
    activeIndex: Ref<number>;
    /** Callback when Enter is pressed on a valid index */
    onSelect?: (index: number) => void;
    /** Optional: Loop navigation (Down at bottom goes to top) */
    loop?: boolean;
}

/**
 * Standardizes Arrow Key navigation for lists (Search results, Tables, Trees).
 * Removes repetitive Math.min/max logic from components.
 */
export function useListNavigation(options: UseListNavigationOptions) {
    const { listLength, activeIndex, onSelect, loop = false } = options

    const handleKey = (e: KeyboardEvent) => {
        if (listLength.value === 0) return

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault()
                if (activeIndex.value < listLength.value - 1) {
                    activeIndex.value++
                } else if (loop) {
                    activeIndex.value = 0
                }
                break
            case 'ArrowUp':
                e.preventDefault()
                if (activeIndex.value > 0) {
                    activeIndex.value--
                } else if (activeIndex.value === -1) {
                    // If -1 (input focused), go to bottom? Or stay? 
                    // Usually stay, or wrap to bottom if loop is on.
                    if (loop) activeIndex.value = listLength.value - 1
                } else if (loop) {
                    activeIndex.value = listLength.value - 1
                }
                break
            case 'Home':
                e.preventDefault()
                activeIndex.value = 0
                break
            case 'End':
                e.preventDefault()
                activeIndex.value = listLength.value - 1
                break
            case 'Enter':
                // Only trigger if an item is actually selected
                if (activeIndex.value >= 0 && onSelect) {
                    e.preventDefault()
                    onSelect(activeIndex.value)
                }
                break
        }
    }

    return { handleKey }
}