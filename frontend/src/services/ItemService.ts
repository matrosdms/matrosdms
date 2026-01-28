import { client } from '@/api/client'
import { ItemSchema, ItemListSchema } from '@/schemas'
import { getErrorMessage } from '@/lib/utils'
import { EArchivedState } from '@/enums'
import type { Item } from '@/types/models'
import type { components } from '@/types/schema'

type UpdateItemPayload = components['schemas']['UpdateItemMessage'];
type BatchRequest = components['schemas']['BatchRequest'];

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

export const ItemService = {
  
  async getByContext(contextId: string): Promise<Item[]> {
      const allItems: any[] = []
      let page = 0
      let hasNext = true

      // Fetch all pages
      while (hasNext) {
          const { data, error } = await client.GET("/api/items", { 
              params: {
                  query: { 
                      context: contextId, 
                      archiveState: EArchivedState.ONLYACTIVE,
                      page: page,
                      size: 50,
                      sort: ['issueDate,desc'] 
                  } as any
              }
          })
          
          if(error) throwWithCode(error)

          const content = data?.content || []
          if (content.length > 0) {
              allItems.push(...content)
              page++
              if (data?.last === true || content.length < 50) hasNext = false
          } else {
              hasNext = false
          }
      }

      // RUNTIME VALIDATION (Zod)
      const result = ItemListSchema.safeParse(allItems)
      
      if (!result.success) {
          console.error("[ItemService] Validation Failed:", result.error)
          return [] 
      }
      
      return result.data as Item[]
  },

  async getById(uuid: string): Promise<Item> {
    const { data, error } = await client.GET("/api/items/{uuid}", { params: { path: { uuid } } })
    if (error) throwWithCode(error)
    
    // Validate single item
    return ItemSchema.parse(data) as Item
  },

  async create(payload: any): Promise<Item> {
    const { data, error } = await client.POST('/api/items', { body: payload })
    if (error) throwWithCode(error)
    return ItemSchema.parse(data) as Item
  },

  async update(uuid: string, payload: UpdateItemPayload): Promise<Item> {
    const { data, error } = await client.PUT('/api/items/{uuid}', { 
      params: { path: { uuid } }, body: payload
    })
    if (error) throwWithCode(error)
    return ItemSchema.parse(data) as Item
  },

  async archive(uuid: string) {
    const { error } = await client.DELETE('/api/items/{uuid}', { params: { path: { uuid } } })
    if (error) throwWithCode(error)
  },
  
  async _fetchBlob(url: string): Promise<{ blob: Blob, type: string }> {
      const { useAuthStore } = await import('@/stores/auth')
      const auth = useAuthStore()
      if (!auth.token) throw new Error("Authentication required")

      const response = await fetch(url, {
          headers: { 'Authorization': `Bearer ${auth.token}` }
      })

      if (!response.ok) throw new Error("Failed to load content")
      const blob = await response.blob()
      return { blob, type: blob.type }
  },

  async getDocumentBlob(uuid: string) {
      return this._fetchBlob(`/api/items/${uuid}/content`)
  },

  async openDocument(uuid: string) {
      try {
          const { blob } = await this.getDocumentBlob(uuid)
          const url = window.URL.createObjectURL(blob)
          const win = window.open(url, '_blank')
          if (!win) throw new Error("Popup blocked")
          setTimeout(() => window.URL.revokeObjectURL(url), 60000) 
      } catch (e: any) {
          throw new Error(e.message)
      }
  },

  async batchMove(itemUuids: string[], targetContextUuid: string) {
      const body: BatchRequest = { itemUuids, targetContextUuid }
      const { error } = await client.POST("/api/items/batch/move", { body })
      if (error) throwWithCode(error)
  },

  async batchTag(itemUuids: string[], addTags: string[], removeTags: string[]) {
      const body: BatchRequest = { itemUuids, addTags, removeTags }
      const { error } = await client.POST("/api/items/batch/tag", { body })
      if (error) throwWithCode(error)
  },

  async batchDelete(itemUuids: string[]) {
      const body: BatchRequest = { itemUuids }
      const { error } = await client.DELETE("/api/items/batch", { body: body } as any)
      if (error) throwWithCode(error)
  }
}