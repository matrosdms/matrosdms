import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'

export const AdminService = {
  async getHistory() {
    const pageable = { page: 0, size: 50, sort: ['executionTime,desc'] }
    const { data, error } = await client.GET("/api/jobs", { 
        params: { query: { pageable } as any } 
    })
    if (error) throw new Error(getErrorMessage(error))
    // API returns Page object with content array
    return (data as any)?.content || []
  },

  async startJob(type: 'INTEGRITY_CHECK' | 'EXPORT_ARCHIVE' | 'REINDEX_SEARCH', config?: string) {
    const { data, error } = await client.POST("/api/admin/jobs/{type}", {
        params: { path: { type }, query: { config } },
        parseAs: 'text' 
    })
    
    if (error) throw new Error(getErrorMessage(error))
    return data
  }
}