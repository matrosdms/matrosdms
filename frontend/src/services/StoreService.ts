import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { throwAPIError } from '@/lib/utils'

type MStore = components['schemas']['MStore'];

function validateUUID(uuid: string, fieldName = 'UUID'): void {
    if (!uuid || typeof uuid !== 'string' || uuid.trim() === '') {
        throw new Error(`${fieldName} is required and must be a non-empty string`)
    }
}

export const StoreService = {
  async getAll(): Promise<MStore[]> {
    const { data, error } = await client.GET("/api/stores", {})
    if (error) throwAPIError(error)
    return data ?? []
  },

  async create(payload: MStore): Promise<MStore> {
    const { data, error } = await client.POST('/api/stores', { body: payload })
    if (error) throwAPIError(error)
    if (!data) throw new Error('No data returned from create')
    return data
  },

  async update(uuid: string, payload: MStore): Promise<MStore> {
    validateUUID(uuid, 'Store UUID')
    
    const { data, error } = await client.PUT('/api/stores/{id}', { 
        params: { path: { id: uuid } },
        body: payload 
    })
    if (error) throwAPIError(error)
    if (!data) throw new Error('No data returned from update')
    return data
  },

  async delete(uuid: string): Promise<void> {
    validateUUID(uuid, 'Store UUID')

    const { error } = await client.DELETE('/api/stores/{id}', { params: { path: { id: uuid } } })
    if (error) throwAPIError(error)
  },

  async getNextNumber(id: string): Promise<number> {
    validateUUID(id, 'Store UUID')

    const { data, error } = await client.GET('/api/stores/{id}/next-number', {
        params: { path: { id } }
    })
    if (error) throwAPIError(error)
    return data ?? 0
  }
}