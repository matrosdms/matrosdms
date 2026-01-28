import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

export const JobService = {
  async getUnifiedJobs() {
    // Fix: API requires pageable
    const pageable = { page: 0, size: 50, sort: ['executionTime,desc'] }
    const { data, error } = await client.GET("/api/jobs", {
        params: { query: { pageable } as any }
    })
    if (error) throw new Error(getErrorMessage(error))
    return ((data as any) || []) as components['schemas']['JobMessage'][]
  }
}