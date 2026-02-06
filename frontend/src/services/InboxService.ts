import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

type InboxFile = components['schemas']['InboxFile'];

const throwWithCode = (error: any) => {
    const msg = getErrorMessage(error)
    throw new Error(msg)
}

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

  // FIX: Using client.GET with parseAs: 'blob' ensures Token Refresh logic works
  async getFileBlob(hash: string): Promise<{ blob: Blob, type: string }> {
      const { data, error, response } = await client.GET("/api/inbox/{hash}/content", {
          params: { path: { hash } },
          parseAs: "blob"
      });

      if (error) throwWithCode(error);
      
      const blob = data as Blob;
      return { 
          blob, 
          type: response.headers.get('content-type') || blob.type || 'application/octet-stream' 
      };
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