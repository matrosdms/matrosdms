import type { components } from '@/types/schema'

type MCategory = components['schemas']['MCategory'];

/**
 * Builds a map where key = CategoryUUID and value = Set of all descendant UUIDs (inclusive).
 * Used for "Transitive" filtering (e.g. selecting "Finance" also includes "Invoices").
 */
export const buildTransitiveMap = (node: MCategory | null | undefined): Map<string, Set<string>> => {
    const map = new Map<string, Set<string>>()
    if (!node) return map
    
    const buildSet = (n: MCategory) => {
        const set = new Set<string>()
        const id = n.uuid
        if (id) set.add(id)

        if (n.children && n.children.length > 0) {
            n.children.forEach(child => {
                const childSet = buildSet(child)
                childSet.forEach(cid => set.add(cid))
            })
        }
        
        if (id) map.set(id, set)
        return set
    }

    buildSet(node)
    return map
}