import { client } from '@/api/client'
import { ItemSchema, ItemListSchema } from '@/schemas'
import { getErrorMessage } from '@/lib/utils'
import { EArchiveFilter, type EArchiveFilterType } from '@/enums'
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
  
  async getByContext(contextId: string, archiveState: EArchiveFilterType = EArchiveFilter.ACTIVE_ONLY): Promise<Item[]> {
      const allItems: any[] = []
      let page = 0
      let hasNext = true

      // Fetch all pages
      while (hasNext) {
          const { data, error } = await client.GET("/api/items", { 
              params: {
                  query: { 
                      context: contextId, 
                      // Explicitly pass the filter (Active vs Archived)
                      archiveState: archiveState,
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

  /**
   * Soft-delete (Archive) an item.
   * Maps to DELETE /api/items/{uuid}
   */
  async delete(uuid: string) {
    const { error } = await client.DELETE('/api/items/{uuid}', { params: { path: { uuid } } })
    if (error) throwWithCode(error)
  },
  
  // Alias
  async archive(uuid: string) {
    return this.delete(uuid)
  },

  /**
   * Restore an archived item to active state.
   */
  async restore(uuid: string) {
      const { error } = await client.POST('/api/items/{uuid}/restore', { params: { path: { uuid } } })
      if (error) throwWithCode(error)
  },

  /**
   * Permanently remove item from Database and Storage.
   * Maps to DELETE /api/items/{uuid}/permanent
   */
  async destroy(uuid: string) {
      const { error } = await client.DELETE('/api/items/{uuid}/permanent', { params: { path: { uuid } } })
      if (error) throwWithCode(error)
  },
  
  /**
   * Get the extracted text layer (OCR) for an item.
   */
  async getRawText(uuid: string): Promise<string> {
      const { data, error } = await client.GET("/api/items/{uuid}/text", {
          params: { path: { uuid } },
          parseAs: "text"
      });
      if (error) throwWithCode(error);
      return data || "";
  },

  /**
   * Run an AI transformation on the item content.
   */
  async aiTransform(uuid: string, instruction: 'SUMMARY' | 'KEY_FACTS' | 'action_items' | 'proofread', format: 'TEXT' | 'MARKDOWN' | 'JSON' = 'MARKDOWN'): Promise<string> {
      // Map lowercase inputs to UpperCase Enums if needed
      const safeInstruction = instruction.toUpperCase() as any;
      
      const { data, error } = await client.POST("/api/items/{uuid}/ai/transform", {
          params: { 
              path: { uuid },
              query: { instruction: safeInstruction, format }
          },
          parseAs: "text"
      });
      if (error) throwWithCode(error);
      return data || "";
  },

  async getDocumentBlob(uuid: string): Promise<{ blob: Blob, type: string }> {
      const { data, error, response } = await client.GET("/api/items/{uuid}/content", {
          params: { path: { uuid } },
          parseAs: "blob"
      });

      if (error) throwWithCode(error);
      
      const blob = data as Blob;
      return { 
          blob, 
          type: response.headers.get('content-type') || blob.type || 'application/octet-stream' 
      };
  },

  getThumbnailUrl(uuid: string): string {
      return `/api/items/${uuid}/thumbnail`
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