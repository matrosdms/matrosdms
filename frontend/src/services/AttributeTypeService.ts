import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'

export const AttributeTypeService = {
  async getAll() {
    const { data, error } = await client.GET("/api/attribute-types", {})
    if (error) throw new Error(getErrorMessage(error))
    return ((data as unknown) || []) as components['schemas']['MAttributeType'][]
  },

  async create(payload: any) {
    const { data, error } = await client.POST("/api/attribute-type", { body: payload })
    if (error) throw new Error(getErrorMessage(error))
    return data as components['schemas']['MAttributeType']
  },

  async update(id: string, payload: any) {
    const { data, error } = await client.PUT("/api/attribute-type/{uuid}", { 
        params: { path: { uuid: id } },
        body: payload
    })
    if (error) throw new Error(getErrorMessage(error))
    return data as components['schemas']['MAttributeType']
  },

  async delete(id: string) {
    const { error } = await client.DELETE("/api/attribute-type/{uuid}", {
        params: { path: { uuid: id } }
    })
    if (error) throw new Error(getErrorMessage(error))
  }
}