import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'

// 1. Strict Type Imports
type MAction = components['schemas']['MAction'];
type PageMAction = components['schemas']['PageMAction']; 
type CreateActionPayload = components['schemas']['CreateActionMessage'];
type UpdateActionPayload = components['schemas']['UpdateActionMessage'];

const throwWithCode = (error: any) => {
    const msg = getErrorMessage(error)
    const err: any = new Error(msg)
    if (typeof error === 'object') {
        err.errorCode = error.errorCode
        err.validationErrors = error.validationErrors
        err.status = error.status
    }
    throw err
}

export const ActionService = {
  async getAll(
      pageable: { page?: number, size?: number, sort?: string[] } = { size: 100, sort: ['dueDate,asc'] },
      filters: { status?: string[], assignee?: string, minDate?: string } = {}
  ): Promise<PageMAction> { 
    
    const query: any = { 
        page: pageable.page || 0,
        size: pageable.size || 100,
        sort: pageable.sort || ['dueDate,asc']
    };
    
    if (filters.status && filters.status.length > 0) query.status = filters.status;
    if (filters.assignee) query.assignee = filters.assignee;
    if (filters.minDate) query.minDate = filters.minDate;

    const { data, error } = await client.GET("/api/actions", { 
        params: { query } 
    })
    
    // FIX: Throw error instead of returning empty object so UI knows fetch failed
    if (error) throwWithCode(error)

    return data as PageMAction
  },

  async getByItem(itemUuid: string): Promise<MAction[]> {
    const { data, error } = await client.GET("/api/actions/item/{itemUuid}", {
        params: { path: { itemUuid } }
    })
    
    // FIX: Throw error instead of returning [] to allow "Retry" UI
    if (error) throwWithCode(error)
    
    return (data as unknown as MAction[]) || []
  },

  async create(payload: CreateActionPayload) {
    const { error } = await client.POST('/api/actions', { body: payload })
    if (error) throwWithCode(error)
  },

  async update(uuid: string, payload: UpdateActionPayload) {
    const { error } = await client.PUT('/api/actions/{uuid}', { 
        params: { path: { uuid } }, body: payload 
    })
    if (error) throwWithCode(error)
  },

  async delete(uuid: string) {
    const { error } = await client.DELETE('/api/actions/{uuid}', { 
        params: { path: { uuid } } 
    })
    if (error) throwWithCode(error)
  }
}