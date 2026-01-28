import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import { ERootCategory, ERootCategoryList, type ERootCategoryType } from '@/enums'
import type { components } from '@/types/schema'

type MCategory = components['schemas']['MCategory'];

const throwWithCode = (error: any) => {
    const msg = getErrorMessage(error)
    const err: any = new Error(msg)
    if (typeof error === 'object') {
        err.errorCode = error.errorCode
        err.status = error.status
    }
    throw err
}

export const CategoryService = {
  
  // Unified Tree Fetcher
  async getTree(id: string | ERootCategoryType) {
      // If it's a Root Enum (WHO, WHAT...), use the root endpoint
      if (ERootCategoryList.includes(id as any)) {
          const { data, error } = await client.GET("/api/category/root/{type}", {
              params: { path: { type: id as ERootCategoryType }, query: { transitive: true } }
          })
          if (error) throwWithCode(error)
          return data as MCategory
      } 
      // Otherwise generic UUID fetch
      return this.getById(id as string, true);
  },

  async getById(id: string, transitive = false) {
    const { data, error } = await client.GET('/api/category/{id}', { 
        params: { path: { id }, query: { transitive } } 
    })
    if (error) throwWithCode(error)
    return data as MCategory
  },

  async create(parentId: string, payload: any) {
    const { data, error } = await client.POST("/api/category/{id}", {
      params: { path: { id: parentId } },
      body: payload
    })
    if (error) throwWithCode(error)
    return data as MCategory
  },

  async update(uuid: string, payload: any) {
    const { data, error } = await client.PUT("/api/category/{id}", {
      params: { path: { id: uuid } },
      body: payload
    })
    if (error) throwWithCode(error)
    return data as MCategory
  },

  async delete(id: string) {
    const { error } = await client.DELETE("/api/category/{id}", { params: { path: { id: id } } })
    if (error) throwWithCode(error)
  },

  async importToRoot(type: ERootCategory, yamlContent: string, simulate: boolean = false, replace: boolean = false) {
      const safeContent = yamlContent || "";
      const { data, error, response } = await client.POST("/api/category/root/{type}/import", {
          params: { path: { type } },
          body: { yaml: safeContent, simulate, replace },
          headers: { "Accept": "text/plain" },
          parseAs: 'text' 
      })
      if (response.ok) return data as string;
      if (error) throwWithCode(error)
      throw new Error(`Import failed with status: ${response.status}`);
  }
}