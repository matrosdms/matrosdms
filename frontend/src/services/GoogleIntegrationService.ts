import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'

export type GoogleStatus = components['schemas']['GoogleIntegrationStatus']
type GoogleSettings = components['schemas']['GoogleSyncSettingsMessage']

const fail = (error: unknown): never => {
    throw new Error(getErrorMessage(error))
}

/** The current user's Google Tasks / Calendar connection (see doc/google-sync.md). */
export const GoogleIntegrationService = {
    async getStatus(): Promise<GoogleStatus> {
        const { data, error } = await client.GET('/api/integrations/google/status')
        if (error) fail(error)
        return data as GoogleStatus
    },

    /**
     * Returns Google's consent URL; the caller sends the browser there. Connecting asks for Google
     * Tasks only; `calendar: true` also asks for the calendar and switches it on when granted.
     */
    async connect(options: { calendar?: boolean } = {}): Promise<string> {
        const { data, error } = await client.POST('/api/integrations/google/connect', {
            params: { query: { calendar: !!options.calendar } }
        })
        if (error) fail(error)
        return (data as components['schemas']['GoogleConnectResponse']).authorizationUrl!
    },

    async updateSettings(settings: GoogleSettings): Promise<GoogleStatus> {
        const { data, error } = await client.PUT('/api/integrations/google/settings', { body: settings })
        if (error) fail(error)
        return data as GoogleStatus
    },

    /** Queues a background sync of all the user's tasks; returns how many. */
    async resync(): Promise<number> {
        const { data, error } = await client.POST('/api/integrations/google/resync')
        if (error) fail(error)
        return (data as Record<string, number>)?.scheduled ?? 0
    },

    async disconnect(): Promise<GoogleStatus> {
        const { data, error } = await client.DELETE('/api/integrations/google')
        if (error) fail(error)
        return data as GoogleStatus
    }
}
