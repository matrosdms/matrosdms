import type { components } from '@/types/schema'
import { EStage, type EStageType } from '@/enums'

type CreateItemMsg = components['schemas']['CreateItemMessage'];
type UpdateItemMsg = components['schemas']['UpdateItemMessage'];

// Defined strict interface matching useItemForm data
interface ItemFormInput {
    name: string;
    description?: string;
    icon?: string;
    issueDate?: string;
    dateExpire?: string;
    storeId?: string;
    storeItemNumber?: string;
    stage?: EStageType;
    kindId?: string;
    attributes?: UIFormAttribute[];
}

type UIFormAttribute = {
    definitionId?: string;
    typeKey?: string;
    value: any;
};

export const ItemMapper = {
  toCreatePayload(form: ItemFormInput, contextId: string, userId: string, fileHash: string): CreateItemMsg {
    return {
        name: form.name,
        description: form.description,
        icon: form.icon || undefined,
        contextIdentifier: contextId,
        userIdentifier: userId,
        sha256: fileHash, 
        
        issueDate: form.issueDate ? new Date(form.issueDate).toISOString() : undefined,
        dateExpire: form.dateExpire ? new Date(form.dateExpire).toISOString() : undefined,
        
        storeIdentifier: form.storeId || undefined,
        storeItemNumber: form.storeItemNumber || undefined,
        
        stage: form.stage || EStage.ACTIVE, 
        kindList: form.kindId ? [form.kindId] : [],
        attributes: this.mapAttributes(form.attributes)
    }
  },
  
  toUpdatePayload(form: ItemFormInput, contextId: string | undefined, userId: string | undefined): UpdateItemMsg {
    return {
        name: form.name,
        description: form.description,
        icon: form.icon || undefined,
        
        issueDate: form.issueDate ? new Date(form.issueDate).toISOString() : undefined,
        dateExpire: form.dateExpire ? new Date(form.dateExpire).toISOString() : undefined,
        
        storeIdentifier: form.storeId || undefined,
        storeItemNumber: form.storeItemNumber || undefined,
        
        stage: form.stage, 
        kindList: form.kindId ? [form.kindId] : [],
        attributes: this.mapAttributes(form.attributes),
        
        contextIdentifier: contextId,
        userIdentifier: userId
    }
  },
  
  mapAttributes(attrs: UIFormAttribute[] | undefined) {
    const map: Record<string, any> = {}
    if (!attrs) return map
    
    attrs.forEach(a => {
        // Strict null check
        if (a.value === null || a.value === '' || a.value === undefined) return;

        if (a.definitionId) {
            map[a.definitionId] = { value: a.value }
        } else {
            console.error(`[ItemMapper] Critical: Attribute '${a.typeKey}' has value but missing Definition ID.`, a)
            throw new Error(`Cannot save attribute '${a.typeKey || 'Unknown'}': System ID missing. Please refresh the page.`)
        }
    })
    return map
  }
}