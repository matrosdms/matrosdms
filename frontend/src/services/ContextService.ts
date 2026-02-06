import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'
import { EArchiveFilter } from '@/enums'

type UpdateContextPayload = components['schemas']['UpdateContextMessage'];
type MContext = components['schemas']['MContext'];

export const ContextService = {
  async getAll() {
    const { data, error } = await client.GET("/api/contexts", { 
        params: { 
            query: { 
                // Only fetch non-archived contexts
                archiveState: EArchiveFilter.ACTIVE_ONLY, 
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

  /**
   * Archive/Soft Delete a context.
   * Maps to DELETE /api/contexts/{id}
   * Note: Returns 409 Conflict if context contains items (preventing accidental loss).
   */
  async close(uuid: string) {
    const { error, response } = await client.DELETE('/api/contexts/{id}', { 
      params: { path: { id: uuid } } 
    })
    if (response?.status === 409) {
      throw new Error('Cannot delete folder. Please move or delete items inside it first.')
    }
    if (error) throw new Error(getErrorMessage(error))
  },
  
  async archive(uuid: string) {
    return this.close(uuid)
  }
}