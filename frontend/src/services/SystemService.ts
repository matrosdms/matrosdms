import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'

export const SystemService = {
  async getVersion() {
    const { data, error } = await client.GET("/api/system/version", {})
    if (error) throw new Error(getErrorMessage(error))
    return data as any
  }
}