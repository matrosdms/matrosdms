import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

export const JobService = {
  async getUnifiedJobs(statusFilter?: string) {
    const pageable = { page: 0, size: 50, sort: ['executionTime,desc'] }
    
    // Calculate 'from' date: default to 7 days ago to show recent history
    // For RUNNING/QUEUED, don't limit by date
    let from: string | undefined = undefined
    if (!statusFilter || statusFilter === 'COMPLETED' || statusFilter === 'FAILED') {
      const weekAgo = new Date()
      weekAgo.setDate(weekAgo.getDate() - 7)
      from = weekAgo.toISOString()
    }
    
    const { data, error } = await client.GET("/api/jobs", {
        params: { query: { pageable, from } as any }
    })
    if (error) throw new Error(getErrorMessage(error))
    
    // API returns Page object with content array
    let jobs = ((data as any)?.content || []) as components['schemas']['JobMessage'][]
    
    // Client-side status filtering (backend doesn't have status filter param)
    if (statusFilter) {
      jobs = jobs.filter(j => j.status === statusFilter)
    }
    
    return jobs
  }
}