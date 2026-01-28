import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'
import { EArchivedState } from '@/enums'

type UpdateContextPayload = components['schemas']['UpdateContextMessage'];
type MContext = components['schemas']['MContext'];

export const ContextService = {
  async getAll() {
    const { data, error } = await client.GET("/api/contexts", { 
        params: { 
            query: { 
                archiveState: EArchivedState.ONLYACTIVE, 
                sort: 'name'
            } 
        } 
    })
    if (error) throw new Error(getErrorMessage(error))
    return (data || []) as MContext[]
  },

  async create(payload: any) {
    const { error } = await client.POST('/api/contexts', { body: payload })
    if (error) throw new Error(getErrorMessage(error))
  },

  async update(uuid: string, payload: UpdateContextPayload) {
    const { error } = await client.PUT('/api/contexts/{id}', { 
      params: { path: { id: uuid } }, body: payload
    })
    if (error) throw new Error(getErrorMessage(error))
  },

  async archive(uuid: string) {
    const { error } = await client.DELETE('/api/contexts/{id}', { 
      params: { path: { id: uuid } } 
    })
    if (error) throw new Error(getErrorMessage(error))
  }
}