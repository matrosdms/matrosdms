import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'
import { ESearchDimension, EOperator } from '@/enums'

type SearchCriteria = components['schemas']['SearchCriteria'];
type PageMSearchResult = components['schemas']['PageMSearchResult'];

export const SearchService = {
  /**
   * Search using structured Payload or simple String.
   * Uses Generated Enums for type safety.
   */
  async search(queryOrPayload: string | SearchCriteria) {
    let payload: SearchCriteria;
    
    if (typeof queryOrPayload === 'string') {
        if (!queryOrPayload || queryOrPayload.length < 2) return [];
        payload = {
            type: 'GROUP',
            logic: 'AND',
            children: [
                { 
                    type: 'FILTER', 
                    field: ESearchDimension.FULLTEXT, // Enum
                    operator: EOperator.CONTAINS,     // Enum
                    value: queryOrPayload 
                }
            ]
        };
    } else {
        payload = queryOrPayload;
    }

    const { data, error } = await client.POST("/api/search", {
      body: payload,
      params: { query: { offset: 0, limit: 50 } }
    });
    
    if (error) throw new Error(getErrorMessage(error))
    
    return ((data as PageMSearchResult)?.content || []) as components['schemas']['MSearchResult'][]
  },

  async getSuggestions(field: string, q: string) {
      if (!field) return []
      
      const { data, error } = await client.GET("/api/search/suggest", {
          params: { query: { field, q: q || '' } }
      })
      if (error) return []
      return (data as unknown as string[]) || []
  }
}