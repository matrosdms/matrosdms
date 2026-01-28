import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'

export const SystemInfoService = {
  async getVersion() {
    const { data, error } = await client.GET("/api/system/version", {})
    if (error) throw new Error(getErrorMessage(error))
    
    const raw = data as any
    return {
        backend: {
            version: raw?.version || 'unknown',
            buildDate: raw?.buildDate || '',
            branch: raw?.branch || '',
            buildNumber: ''
        },
        database: {
            currentVersion: 'Managed',
            productInfo: 'PostgreSQL',
            history: []
        }
    }
  }
};