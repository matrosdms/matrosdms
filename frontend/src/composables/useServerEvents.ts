import { onMounted, onUnmounted, watch } from 'vue'
import { useWorkflowStore } from '@/stores/workflow'
import { useUIStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { useQueryClient } from '@tanstack/vue-query'
import type { BroadcastMessage, InboxFile, ProgressMessage, PipelineStatusMessage } from '@/types/events'

export function useServerEvents() {
    const workflow = useWorkflowStore()
    const ui = useUIStore()
    const auth = useAuthStore()
    const queryClient = useQueryClient()
    
    let abortController: AbortController | null = null
    let reconnectTimer: any = null
    let retryCount = 0
    let isConnecting = false

    const connect = async () => {
        if (!auth.token || isConnecting) return
        
        if (abortController) abortController.abort()
        if (reconnectTimer) clearTimeout(reconnectTimer)
        
        abortController = new AbortController()
        isConnecting = true
        
        try {
            const response = await fetch('/api/api/stream/updates', {
                headers: {
                    'Authorization': `Bearer ${auth.token}`,
                    'Accept': 'text/event-stream'
                },
                signal: abortController.signal
            })

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    isConnecting = false
                    return
                }
                throw new Error(`SSE Status ${response.status}`)
            }

            if (!response.body) throw new Error('No body in response')

            const reader = response.body.getReader()
            const decoder = new TextDecoder()
            let buffer = ''
            
            ui.addLog('[SSE] Connected', 'debug')
            retryCount = 0

            while (true) {
                const { value, done } = await reader.read()
                if (done) break
                
                buffer += decoder.decode(value, { stream: true })
                const parts = buffer.split(/\r\n\r\n|\n\n/)
                buffer = parts.pop() || '' 

                for (const part of parts) {
                    const lines = part.split(/\r\n|\n/)
                    let dataBuffer = ''
                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            dataBuffer += line.replace(/^data:\s?/, '') + '\n'
                        }
                    }
                    if (dataBuffer.trim()) {
                        try {
                            const event = JSON.parse(dataBuffer)
                            handleEvent(event)
                        } catch (e) {
                            console.error('[SSE] JSON Parse error', e)
                        }
                    }
                }
            }
        } catch (err: any) {
            if (err.name !== 'AbortError') {
                scheduleReconnect()
            }
        } finally {
            isConnecting = false
        }
    }

    const scheduleReconnect = () => {
        if (reconnectTimer) clearTimeout(reconnectTimer)
        const delay = Math.min(1000 * (2 ** retryCount), 30000)
        reconnectTimer = setTimeout(() => {
            retryCount++
            connect()
        }, delay)
    }

    const handleEvent = (wrapper: BroadcastMessage) => {
        const { process, type, message } = wrapper;

        // 1. INBOX: File Detected
        if (process === 'INBOX' && type === 'FILE_ADDED') {
            const file = message as InboxFile;
            if (file.sha256) {
                workflow.upsertLiveFile(file);
                // Invalidate query to eventually sync full list
                queryClient.invalidateQueries({ queryKey: ['inbox'] })
            }
        }

        // 2. PIPELINE: Updates
        if (process === 'PIPELINE') {
            
            // 2A. PROGRESS
            if (type === 'PROGRESS') {
                const prog = message as ProgressMessage;
                if (prog.sha256) {
                    workflow.upsertLiveFile({
                        sha256: prog.sha256,
                        status: 'PROCESSING',
                        progressMessage: prog.info
                    });
                }
            }

            // 2B. STATUS / COMPLETE
            if (type === 'STATUS' || type === 'COMPLETE' || type === 'ERROR') {
                const statusMsg = message as PipelineStatusMessage;
                
                if (statusMsg.fileState) {
                    // Update the full file state (Metadata snap or Final Result)
                    workflow.upsertLiveFile(statusMsg.fileState);
                    
                    // Show Toasts based on result
                    const fname = statusMsg.fileState.fileInfo?.originalFilename || statusMsg.originalFilename || 'File';
                    
                    if (statusMsg.status === 'READY') {
                        ui.addLog(`Ready: ${statusMsg.fileState.emailInfo?.subject || fname}`, 'success');
                    } else if (statusMsg.status === 'DUPLICATE') {
                        ui.addLog(`Duplicate detected: ${fname}`, 'warning');
                    } else if (statusMsg.status === 'ERROR') {
                        ui.addLog(`Processing failed: ${fname}`, 'error');
                    }
                } else if (statusMsg.sha256) {
                    // Fallback if full fileState not present, update status minimally
                    workflow.upsertLiveFile({ 
                        sha256: statusMsg.sha256, 
                        status: statusMsg.status as any 
                    });
                }
                
                // If it's a metadata snap (STATUS but still processing), don't invalidate yet
                if (type === 'COMPLETE' || statusMsg.status === 'READY') {
                    queryClient.invalidateQueries({ queryKey: ['inbox'] });
                }
            }
        }
    }

    onMounted(() => {
        if (auth.isAuthenticated) connect()
    })

    watch(() => auth.isAuthenticated, (isAuth) => {
        if (isAuth) connect()
        else if (abortController) abortController.abort()
    })

    onUnmounted(() => {
        if (reconnectTimer) clearTimeout(reconnectTimer)
        if (abortController) abortController.abort()
    })
}