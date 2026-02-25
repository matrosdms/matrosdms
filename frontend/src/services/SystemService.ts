import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

type SystemInfoResponse = components['schemas']['SystemInfoResponse']

export const SystemService = {
  async getVersion(): Promise<SystemInfoResponse> {
    const { data, error } = await client.GET("/api/system/version", {})
    if (error) throw new Error(getErrorMessage(error))
    return data || {}
  }
}