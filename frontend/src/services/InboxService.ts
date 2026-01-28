import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

type InboxFile = components['schemas']['InboxFile'];

export const InboxService = {
  
  async getAll() {
      const { data, error } = await client.GET("/api/inbox", {})
      if (error) throw new Error(getErrorMessage(error))
      return (data || []) as InboxFile[]
  },

  async upload(file: File) {
      const formData = new FormData()
      formData.append('file', file)
      
      const { data, error } = await client.POST("/api/upload", {
          body: formData as any,
          bodySerializer: (body) => body, 
      })
      
      if (error) throw new Error(getErrorMessage(error))
      return data
  },

  async _fetchBlob(url: string): Promise<{ blob: Blob, type: string }> {
      const { useAuthStore } = await import('@/stores/auth')
      const auth = useAuthStore()
      if (!auth.token) throw new Error("Authentication required")

      const response = await fetch(url, {
          headers: { 'Authorization': `Bearer ${auth.token}` }
      })

      if (!response.ok) throw new Error("Failed to load file content")

      const blob = await response.blob()
      return { blob, type: blob.type }
  },

  async getFileBlob(hash: string) {
      return this._fetchBlob(`/api/inbox/${hash}/content`)
  },

  async openFileInNewTab(hash: string) {
    try {
        const { blob } = await this.getFileBlob(hash)
        const url = window.URL.createObjectURL(blob)
        const win = window.open(url, '_blank')
        if (!win) throw new Error("Popup blocked")
        setTimeout(() => window.URL.revokeObjectURL(url), 60000)
    } catch(e: any) {
        throw new Error(e.message)
    }
  }
}