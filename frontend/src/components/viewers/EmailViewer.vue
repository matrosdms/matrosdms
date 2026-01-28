<script setup lang="ts">
import { ref, watch } from 'vue'
import PostalMime from 'postal-mime'
import { Paperclip, User, Calendar, Download, Mail } from 'lucide-vue-next'
import { push } from 'notivue'

const props = defineProps<{
  blob: Blob
}>()

const emailData = ref<any>(null)
const isParsing = ref(false)
const iframeContent = ref('')

const parseEmail = async () => {
    if (!props.blob) return
    isParsing.value = true
    try {
        const parser = new PostalMime()
        const email = await parser.parse(props.blob)
        emailData.value = email
        
        // Prefer HTML, fallback to Text
        const html = email.html || (email.text ? `<pre style="font-family: sans-serif; white-space: pre-wrap;">${email.text}</pre>` : '<i>No Content</i>')
        iframeContent.value = html
    } catch (e: any) {
        push.error(`Failed to parse email: ${e.message}`)
    } finally {
        isParsing.value = false
    }
}

const downloadAttachment = (att: any) => {
    if (!att.content) return
    const url = URL.createObjectURL(new Blob([att.content], { type: att.mimeType }))
    const a = document.createElement('a')
    a.href = url
    a.download = att.filename || 'attachment'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
}

// Utility to clean up malformed headers (e.g. double quoted RFC2047)
const formatSender = (fromObj: any) => {
    if (!fromObj) return 'Unknown'
    let name = fromObj.name || ''
    const address = fromObj.address || ''

    // Fix: ""=?UTF-8..."" issue
    if (name.startsWith('""=?') && name.endsWith('?=""')) {
        name = name.substring(2, name.length - 2)
    }
    
    // Manual Decode if parser failed (Simple Base64 UTF-8)
    if (name.startsWith('=?UTF-8?B?') && name.endsWith('?=')) {
        try {
            const b64 = name.replace('=?UTF-8?B?', '').replace('?=', '')
            name = decodeURIComponent(escape(atob(b64)))
        } catch (e) {
            // keep raw if decode fails
        }
    }

    // Clean up extra quotes
    name = name.replace(/^"+|"+$/g, '')

    return name ? `${name} <${address}>` : address
}

watch(() => props.blob, parseEmail, { immediate: true })
</script>

<template>
  <div class="h-full flex flex-col bg-white dark:bg-gray-900 overflow-hidden">
      <!-- Loading State -->
      <div v-if="isParsing" class="flex-1 flex items-center justify-center text-muted-foreground">
          <span class="animate-pulse">Parsing Email...</span>
      </div>

      <template v-else-if="emailData">
          <!-- Header Area -->
          <div class="p-6 border-b border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 shrink-0">
              <h2 class="text-xl font-bold text-gray-900 dark:text-gray-100 mb-4 leading-snug">
                  {{ emailData.subject || '(No Subject)' }}
              </h2>

              <div class="flex flex-col gap-2 text-sm">
                  <div class="flex items-center gap-2">
                      <div class="w-6 text-gray-400 flex justify-center"><User :size="16"/></div>
                      <span class="font-bold text-gray-700 dark:text-gray-300">From:</span>
                      <span class="text-gray-800 dark:text-gray-200 select-all">
                          {{ formatSender(emailData.from) }}
                      </span>
                  </div>
                  
                  <div class="flex items-center gap-2" v-if="emailData.to && emailData.to.length">
                      <div class="w-6 text-gray-400 flex justify-center"><Mail :size="16"/></div>
                      <span class="font-bold text-gray-700 dark:text-gray-300">To:</span>
                      <span class="text-gray-600 dark:text-gray-400 truncate">
                          {{ emailData.to.map((t: any) => t.name || t.address).join(', ') }}
                      </span>
                  </div>

                  <div class="flex items-center gap-2">
                      <div class="w-6 text-gray-400 flex justify-center"><Calendar :size="16"/></div>
                      <span class="font-bold text-gray-700 dark:text-gray-300">Date:</span>
                      <span class="text-gray-600 dark:text-gray-400">
                          {{ emailData.date ? new Date(emailData.date).toLocaleString() : 'Unknown' }}
                      </span>
                  </div>
              </div>

              <!-- Attachments Chip List -->
              <div v-if="emailData.attachments && emailData.attachments.length > 0" class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                  <div class="text-xs font-bold uppercase text-gray-500 mb-2 flex items-center gap-1">
                      <Paperclip :size="12" /> {{ emailData.attachments.length }} Attachments
                  </div>
                  <div class="flex flex-wrap gap-2">
                      <button 
                        v-for="(att, i) in emailData.attachments" 
                        :key="i"
                        @click="downloadAttachment(att)"
                        class="flex items-center gap-2 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 hover:border-blue-500 dark:hover:border-blue-400 px-3 py-1.5 rounded-md text-xs transition-colors group shadow-sm"
                        title="Click to Download"
                      >
                          <div class="font-medium text-gray-700 dark:text-gray-300 max-w-[150px] truncate">{{ att.filename }}</div>
                          <div class="text-[9px] text-gray-400 font-mono">({{ Math.round(att.content.byteLength / 1024) }} KB)</div>
                          <Download :size="12" class="opacity-0 group-hover:opacity-100 text-blue-600 dark:text-blue-400 transition-opacity" />
                      </button>
                  </div>
              </div>
          </div>

          <!-- Body (Sandboxed Iframe) -->
          <div class="flex-1 bg-white relative">
              <iframe 
                :srcdoc="iframeContent" 
                class="w-full h-full border-0 absolute inset-0" 
                sandbox="allow-popups allow-popups-to-escape-sandbox"
                referrerpolicy="no-referrer"
              ></iframe>
          </div>
      </template>
  </div>
</template>