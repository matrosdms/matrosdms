import { client } from '@/api/client'
import { getErrorMessage } from '@/lib/utils'

export const ReportService = {
  async downloadInventoryCsv() {
    const { data, error, response } = await client.GET("/api/report/items.csv", {
        parseAs: "blob"
    });
    
    if (error) throw new Error(getErrorMessage(error));

    if (data) {
        const url = window.URL.createObjectURL(data as Blob);
        const a = document.createElement('a');
        a.href = url;
        
        let filename = `matros_inventory_${new Date().toISOString().split('T')[0]}.csv`;
        const disposition = response.headers?.get('content-disposition');
        
        // Extract filename from Content-Disposition header if provided
        if (disposition && disposition.indexOf('filename=') !== -1) {
            const matches = /filename="([^"]+)"/.exec(disposition);
            if (matches != null && matches[1]) {
                filename = matches[1];
            }
        }
        
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    }
  }
}