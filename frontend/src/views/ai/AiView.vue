<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import { useStorage, useDateFormat, useNow } from '@vueuse/core'
import { Send, Bot, User, Trash2, Plus, MessageSquare, AlertCircle, RefreshCw, Eraser } from 'lucide-vue-next'
import { AiService } from '@/services/AiService'
import MasterDetailLayout from '@/components/layout/MasterDetailLayout.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSpinner from '@/components/ui/BaseSpinner.vue'
import AppPane from '@/components/ui/BasePane.vue'

// --- Layout State ---
const STORAGE_KEY = 'matros-ai-layout-v1'
const defaultLayout = { sidebar: [20, 80], workspace: [100, 0] }
const layoutStorage = useStorage(STORAGE_KEY, defaultLayout)

const flatLayout = computed(() => [
    layoutStorage.value.sidebar[0], 
    layoutStorage.value.sidebar[1], 
    100, 0 
])

const handleLayoutUpdate = ({ key, sizes }: any) => {
    if (key === 'sidebar') layoutStorage.value.sidebar = sizes
}

// Implement Toggle Logic
const toggleSidebar = () => {
  if (layoutStorage.value.sidebar[0] > 2) layoutStorage.value.sidebar = [0, 100]
  else layoutStorage.value.sidebar = [20, 80]
}

defineExpose({ toggleSidebar })

// --- Chat State ---
interface ChatMessage {
    id: string;
    role: 'user' | 'ai';
    text: string;
    sources?: string[];
    timestamp: number;
    error?: boolean;
}

interface ChatSession {
    id: string;
    title: string;
    lastActive: number;
    messages: ChatMessage[];
}

const sessions = useStorage<ChatSession[]>('matros-ai-sessions', [])
const activeSessionId = ref<string | null>(null)
const input = ref('')
const isLoading = ref(false)
const chatContainer = ref<HTMLElement | null>(null)

// --- Actions ---

const createSession = () => {
    const newSession: ChatSession = {
        id: crypto.randomUUID(),
        title: 'New Conversation',
        lastActive: Date.now(),
        messages: []
    }
    sessions.value.unshift(newSession)
    activeSessionId.value = newSession.id
}

const activeSession = computed(() => sessions.value.find(s => s.id === activeSessionId.value))

// Initialize if empty
onMounted(() => {
    if (sessions.value.length === 0) createSession()
    else if (!activeSessionId.value) activeSessionId.value = sessions.value[0].id
})

const scrollToBottom = () => {
    nextTick(() => {
        if (chatContainer.value) {
            chatContainer.value.scrollTop = chatContainer.value.scrollHeight
        }
    })
}

const sendMessage = async () => {
    if (!input.value.trim() || isLoading.value || !activeSession.value) return
    
    const question = input.value
    const userMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'user',
        text: question,
        timestamp: Date.now()
    }
    
    activeSession.value.messages.push(userMsg)
    activeSession.value.lastActive = Date.now()
    
    // Generate Title if first message
    if (activeSession.value.messages.length === 1) {
        activeSession.value.title = question.length > 30 ? question.substring(0, 30) + '...' : question
    }

    input.value = ''
    isLoading.value = true
    scrollToBottom()

    try {
        const res = await AiService.ask(question)
        const aiMsg: ChatMessage = {
            id: crypto.randomUUID(),
            role: 'ai',
            text: res.answer,
            sources: res.sources,
            timestamp: Date.now()
        }
        activeSession.value.messages.push(aiMsg)
    } catch(e: any) {
        activeSession.value.messages.push({
            id: crypto.randomUUID(),
            role: 'ai',
            text: `Error: ${e.message}`,
            timestamp: Date.now(),
            error: true
        })
    } finally {
        isLoading.value = false
        scrollToBottom()
    }
}

const deleteSession = (id: string) => {
    const idx = sessions.value.findIndex(s => s.id === id)
    if (idx !== -1) {
        sessions.value.splice(idx, 1)
        if (activeSessionId.value === id) {
            activeSessionId.value = sessions.value[0]?.id || null
            if (!activeSessionId.value) createSession()
        }
    }
}

const clearHistory = () => {
    if (confirm('Delete all chat history?')) {
        sessions.value = []
        createSession()
    }
}

const formatTime = (ts: number) => useDateFormat(ts, 'MMM D, HH:mm').value
</script>

<template>
  <MasterDetailLayout 
    :layout="flatLayout"
    @update:layout="handleLayoutUpdate"
  >
      <!-- SIDEBAR: HISTORY -->
      <template #sidebar>
          <AppPane title="Chat History">
              <template #actions>
                  <BaseButton variant="ghost" size="iconSm" @click="clearHistory" title="Clear All History" class="text-muted-foreground hover:text-destructive">
                      <Eraser :size="16" />
                  </BaseButton>
                  <div class="w-px h-3 bg-border mx-1"></div>
                  <BaseButton variant="ghost" size="iconSm" class="text-primary" @click="createSession" title="New Chat">
                      <Plus :size="16" stroke-width="3" />
                  </BaseButton>
              </template>

              <div class="p-2 space-y-1">
                  <div 
                    v-for="session in sessions" 
                    :key="session.id"
                    @click="activeSessionId = session.id"
                    class="group flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all border border-transparent"
                    :class="activeSessionId === session.id ? 'bg-primary/10 border-primary/20 text-primary' : 'hover:bg-muted/50 text-muted-foreground hover:text-foreground'"
                  >
                      <MessageSquare :size="18" class="shrink-0" :class="activeSessionId === session.id ? 'text-primary' : 'opacity-50'" />
                      <div class="flex-1 min-w-0">
                          <div class="text-sm font-medium truncate">{{ session.title }}</div>
                          <div class="text-[10px] opacity-70">{{ formatTime(session.lastActive) }}</div>
                      </div>
                      <button 
                        @click.stop="deleteSession(session.id)" 
                        class="opacity-0 group-hover:opacity-100 p-1.5 hover:bg-destructive/10 hover:text-destructive rounded transition-all"
                        title="Delete Chat"
                      >
                          <Trash2 :size="14" />
                      </button>
                  </div>
              </div>
          </AppPane>
      </template>

      <!-- MAIN: CHAT -->
      <template #list>
          <div class="h-full flex flex-col bg-background relative">
              <!-- Chat Area -->
              <div ref="chatContainer" class="flex-1 overflow-y-auto p-4 md:p-8 space-y-6 scroll-smooth">
                  <div v-if="!activeSession?.messages.length" class="h-full flex flex-col items-center justify-center text-muted-foreground opacity-50 select-none">
                      <div class="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-6">
                          <Bot :size="48" />
                      </div>
                      <h2 class="text-xl font-bold mb-2">How can I help you?</h2>
                      <p class="text-sm">Ask questions about your documents, invoices, or contracts.</p>
                  </div>

                  <div 
                    v-for="msg in activeSession?.messages" 
                    :key="msg.id" 
                    class="flex gap-4 max-w-4xl mx-auto animate-in fade-in slide-in-from-bottom-2 duration-300"
                    :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
                  >
                      <!-- AI Avatar -->
                      <div v-if="msg.role === 'ai'" class="w-8 h-8 rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 flex items-center justify-center shrink-0 mt-1">
                          <Bot :size="18" />
                      </div>

                      <div class="flex flex-col gap-1 max-w-[85%] md:max-w-[75%]">
                          <div 
                            class="p-4 rounded-2xl shadow-sm text-sm leading-relaxed whitespace-pre-wrap"
                            :class="[
                                msg.role === 'user' 
                                    ? 'bg-blue-600 text-white rounded-br-none' 
                                    : (msg.error 
                                        ? 'bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-800 dark:text-red-200 rounded-bl-none' 
                                        : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-foreground rounded-bl-none')
                            ]"
                          >
                              <div v-if="msg.error" class="flex items-center gap-2 mb-1 font-bold text-xs uppercase tracking-wide">
                                  <AlertCircle :size="14"/> System Error
                              </div>
                              {{ msg.text }}
                          </div>
                          
                          <!-- Sources -->
                          <div v-if="msg.sources?.length" class="flex flex-wrap gap-2 mt-1 ml-1">
                              <span class="text-[10px] font-bold text-muted-foreground uppercase tracking-wide">Sources:</span>
                              <span v-for="source in msg.sources" :key="source" class="text-[10px] bg-muted px-1.5 py-0.5 rounded border border-border text-foreground font-mono truncate max-w-[200px]">
                                  {{ source }}
                              </span>
                          </div>
                          
                          <div class="text-[10px] text-muted-foreground px-1" :class="msg.role === 'user' ? 'text-right' : 'text-left'">
                              {{ formatTime(msg.timestamp) }}
                          </div>
                      </div>

                      <!-- User Avatar -->
                      <div v-if="msg.role === 'user'" class="w-8 h-8 rounded-full bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 flex items-center justify-center shrink-0 mt-1">
                          <User :size="18" />
                      </div>
                  </div>

                  <div v-if="isLoading" class="flex gap-4 max-w-4xl mx-auto">
                      <div class="w-8 h-8 rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 flex items-center justify-center shrink-0">
                          <Bot :size="18" />
                      </div>
                      <div class="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 rounded-2xl rounded-bl-none shadow-sm flex items-center gap-3">
                          <BaseSpinner class="text-purple-500" />
                          <span class="text-xs font-medium text-muted-foreground animate-pulse">Thinking...</span>
                      </div>
                  </div>
              </div>

              <!-- Input Area -->
              <div class="p-4 bg-background border-t border-border shrink-0">
                  <div class="max-w-4xl mx-auto relative flex gap-2">
                      <input 
                        v-model="input"
                        @keydown.enter="sendMessage"
                        type="text" 
                        class="flex-1 bg-muted/50 border border-input hover:border-primary/50 focus:border-primary focus:ring-1 focus:ring-primary rounded-xl px-4 py-3 text-sm outline-none transition-all shadow-sm"
                        placeholder="Ask a question about your documents..." 
                        :disabled="isLoading"
                        autofocus
                      />
                      <button 
                        @click="sendMessage"
                        :disabled="!input.trim() || isLoading"
                        class="bg-primary hover:bg-primary-hover text-primary-foreground p-3 rounded-xl disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-md active:scale-95 flex items-center justify-center"
                      >
                          <Send :size="20" />
                      </button>
                  </div>
                  <div class="text-center mt-2">
                      <p class="text-[10px] text-muted-foreground">AI can make mistakes. Please verify important information.</p>
                  </div>
              </div>
          </div>
      </template>
  </MasterDetailLayout>
</template>