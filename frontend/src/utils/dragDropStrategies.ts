import type { components } from '@/types/schema'
import { push } from 'notivue'
import { client } from '@/api/client'
import { ItemService } from '@/services/ItemService'

type MContext = components['schemas']['MContext'];

// Strategy Interface
type DropHandler = (data: any, target: MContext, context: any) => Promise<boolean>

/**
 * Strategy: Move a DMS Item (Document) to a Context
 */
const moveItemStrategy: DropHandler = async (data, target, { queryClient, dms }) => {
    // Prevent drop on self
    if (data.context?.uuid === target.uuid) return false
    
    // Direct move, no confirmation needed for single items
    try {
        if (!data.uuid || !target.uuid) return false;
        
        // Use batchMove instead of directly PUTting the item.
        // This avoids mismatches between the MItem representation and UpdateItemMessage payload (e.g. kindList).
        await ItemService.batchMove([data.uuid], target.uuid)
        
        push.success(`Moved to ${target.name}`)
        
        // Remove from stack if it was in there
        dms.removeFromStack(data.uuid)
        
        queryClient.invalidateQueries({ queryKey: ['items'] })
        queryClient.invalidateQueries({ queryKey: ['contexts'] })
        
        return true
    } catch(e: any) {
        push.error(`Failed to move: ${e.message}`)
        return false
    }
}

/**
 * Strategy: Batch Move (From Item Stack)
 */
const moveBatchStrategy: DropHandler = async (data, target, { queryClient, dms }) => {
    const ids = data.itemUuids as string[]
    if (!ids || ids.length === 0) return false

    // Removed Confirmation as requested ("User knows what he is doing")
    
    try {
        await ItemService.batchMove(ids, target.uuid!)
        
        push.success(`Moved ${ids.length} items to ${target.name}`)
        
        dms.clearStack() // This clears the data store
        queryClient.invalidateQueries({ queryKey: ['items'] })
        queryClient.invalidateQueries({ queryKey: ['contexts'] })
        
        return true
    } catch (e: any) {
        push.error(`Batch move failed: ${e.message}`)
        return false
    }
}

/**
 * Strategy: Assign an Inbox File to a Context (Create New Item)
 */
const inboxFileStrategy: DropHandler = async (data, target, { dms }) => {
    dms.startItemCreation(target, data)
    return true
}

// Registry
export const DROP_STRATEGIES: Record<string, DropHandler> = {
    'dms-item': moveItemStrategy,
    'dms-batch': moveBatchStrategy,
    'inbox-file': inboxFileStrategy
}