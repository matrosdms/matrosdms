import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'
import type { components } from '@/types/schema'

export interface TemplateRepo { id: string; name: string; }
export type TemplateProposal = components['schemas']['TemplateProposal'];

export const TemplateService = {
  async getRepositories() {
    const { data, error } = await client.GET("/api/templates" as any, {})
    if (error) throw new Error(getErrorMessage(error))
    return ((data as any) || []) as TemplateRepo[]
  },

  async getProposals(repoId: string) {
    const { data, error } = await client.GET("/api/templates" as any, {})
    if (error) throw new Error(getErrorMessage(error))
    return ((data as any) ||[]) as TemplateProposal[]
  },

  /**
   * Fetches the raw YAML content for a template.
   * Backend endpoint: @GetMapping(value = "/{templateId}", produces = "application/yaml")
   */
  async getPreview(repoId: string, templateId: string, language?: string) {
    const query: any = {}
    if (language) query['lang'] = language

    const { data, error } = await client.GET("/api/templates/{templateId}", {
        params: { 
            path: { templateId },
            query: query
        },
        headers: {
            // Backend explicitly requires this header to return the body
            "Accept": "application/yaml, text/yaml, text/plain"
        },
        parseAs: "text" // Treat response as raw string, do not try to JSON.parse
    })
    
    if (error) throw new Error(getErrorMessage(error))
    return data as string
  }
}