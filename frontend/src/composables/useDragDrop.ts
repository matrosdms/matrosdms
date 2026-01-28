import { useDmsStore } from '@/stores/dms'
import { useQueryClient } from '@tanstack/vue-query'
import { DROP_STRATEGIES } from '@/utils/dragDropStrategies'
import { InboxService } from '@/services/InboxService'
import { push } from 'notivue'

export function useDragDrop() {
  const dms = useDmsStore()
  const queryClient = useQueryClient()

  const startDrag = (event: DragEvent, type: string, payload: any) => {
    dms.setDragging(true, type)
    // Simplify payload to avoid circular JSON issues
    const data = JSON.stringify({ type, ...payload })
    if (event.dataTransfer) {
      event.dataTransfer.setData('application/json', data)
      event.dataTransfer.effectAllowed = 'copy'
    }
  }

  const endDrag = () => {
    dms.setDragging(false)
  }

  const handleDropOnContext = async (event: DragEvent, targetContext: any) => {
    dms.setDragging(false)
    if (!targetContext) return false

    // 1. External File Drop (Desktop -> App)
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
        const file = event.dataTransfer.files[0]
        try {
            push.info(`Uploading ${file.name}...`)
            const inboxFile = await InboxService.upload(file)
            
            // "Act like it was dropped in the inbox but mark the context"
            // We start the item creation wizard with the context pre-selected.
            dms.startItemCreation(targetContext, inboxFile)
            return true
        } catch (e: any) {
            push.error(`Upload failed: ${e.message}`)
            return false
        }
    }

    // 2. Internal Item Drop (Drag between panes)
    const raw = event.dataTransfer?.getData('application/json')
    if (!raw) return false

    try {
      const data = JSON.parse(raw)
      const strategy = DROP_STRATEGIES[data.type]
      
      if (strategy) {
          // Pass context dependencies to strategy
          return await strategy(data, targetContext, { dms, queryClient })
      }
      return false
    } catch (e) {
      console.error("Drop parse error", e)
      return false
    }
  }

  return { startDrag, endDrag, handleDropOnContext }
}