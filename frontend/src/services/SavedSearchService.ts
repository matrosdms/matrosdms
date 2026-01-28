import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

type SavedSearchMessage = components['schemas']['SavedSearchMessage'];

export const SavedSearchService = {
  async getAll() {
    const { data, error } = await client.GET("/api/users/me/searches", {})
    if (error) throw new Error(getErrorMessage(error))
    return (data as SavedSearchMessage[]) || []
  },

  async create(name: string, query: string) {
    const { error } = await client.POST("/api/users/me/searches", {
        body: { name, query }
    })
    if (error) throw new Error(getErrorMessage(error))
  },

  async delete(name: string) {
    const { error } = await client.DELETE("/api/users/me/searches/{name}", {
        params: { path: { name } }
    })
    if (error) throw new Error(getErrorMessage(error))
  }
}