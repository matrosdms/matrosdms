import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

export const JobService = {
  async getUnifiedJobs(filter: any) {
    // 1. Parse Filter (Object or String)
    const statusVal = typeof filter === 'object' ? filter.status : filter
    const rangeVal = typeof filter === 'object' ? filter.range : '7d'

    // 2. Pagination
    const pageParams = { 
        page: 0, 
        size: 100, // Fetch more for timeline view
        sort: ['executionTime,desc'] 
    }
    
    // 3. Date Calculation
    let from: string | undefined = undefined
    const now = new Date()
    
    if (rangeVal === '7d') {
        now.setDate(now.getDate() - 7)
        from = now.toISOString()
    } else if (rangeVal === '30d') {
        now.setDate(now.getDate() - 30)
        from = now.toISOString()
    }
    // 'all' leaves from as undefined

    const queryParams: any = { 
        ...pageParams,
        from 
    }

    const { data, error } = await client.GET("/api/jobs", {
        params: { query: queryParams }
    })
    
    if (error) throw new Error(getErrorMessage(error))
    
    let jobs = ((data as any)?.content || []) as components['schemas']['JobMessage'][]
    
    // 4. Client-side Status Filtering (if backend param not used)
    if (statusVal) {
      jobs = jobs.filter(j => j.status === statusVal)
    }
    
    // 5. Robust ID Mapping for UI Selection
    return jobs.map(j => {
        // Prefer uuid (new), fallback to instanceId (legacy), fallback to generated
        const id = (j as any).uuid || (j as any).instanceId || `job-${Math.random()}`;
        return { 
            ...j, 
            uuid: id,
            // Ensure instanceId is also present for detail view compatibility
            instanceId: (j as any).instanceId || id 
        };
    });
  }
}