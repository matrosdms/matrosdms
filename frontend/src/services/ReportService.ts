import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'

async function triggerDownload(url: string, filename: string) {
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
}

function extractFilename(response: Response, fallback: string): string {
    const disposition = response.headers?.get('content-disposition')
    if (disposition) {
        const match = /filename="([^"]+)"/.exec(disposition)
        if (match?.[1]) return match[1]
    }
    return fallback
}

export const ReportService = {
    async downloadInventoryCsv() {
        const { data, error, response } = await client.GET("/api/report", {
            params: { query: { format: 'csv' } },
            parseAs: 'blob',
        })
        if (error) throw new Error(getErrorMessage(error))
        if (data) {
            const url = window.URL.createObjectURL(data as Blob)
            const filename = extractFilename(response, `matros_inventory_${today()}.csv`)
            await triggerDownload(url, filename)
        }
    },

    async downloadInventoryHtml() {
        const { data, error, response } = await client.GET("/api/report", {
            params: { query: { format: 'html' } },
            parseAs: 'blob',
        })
        if (error) throw new Error(getErrorMessage(error))
        if (data) {
            const url = window.URL.createObjectURL(data as Blob)
            const filename = extractFilename(response, `matros_inventory_${today()}.html`)
            await triggerDownload(url, filename)
        }
    },
}

function today(): string {
    return new Date().toISOString().split('T')[0]
}
