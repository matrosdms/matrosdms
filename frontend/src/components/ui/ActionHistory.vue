<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { MessageSquare, Plus } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  history: any[]
}>()

const emit = defineEmits(['add-comment'])
const auth = useAuthStore()

const isAddingComment = ref(false)
const newComment = ref('')
const commentInput = ref<HTMLTextAreaElement | null>(null)

const formatTime = (ts: string) => {
    if (!ts) return ''
    return new Date(ts).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const saveComment = () => {
    if (!newComment.value.trim()) return
    emit('add-comment', newComment.value)
    newComment.value = ''
    isAddingComment.value = false
}

const openInput = () => {
    isAddingComment.value = true
    nextTick(() => commentInput.value?.focus())
}
</script>

<template>
  <div class="mt-4 pt-4 border-t border-gray-100">
    <div class="flex items-center justify-between mb-2">
        <label class="text-xs font-bold text-gray-500 uppercase tracking-wider flex items-center gap-1.5">
            <MessageSquare :size="14" class="text-blue-500" /> Activity History
        </label>
        <button 
            v-if="!isAddingComment"
            @click="openInput"
            class="text-[10px] text-blue-600 bg-blue-50 hover:bg-blue-100 border border-blue-200 px-2 py-0.5 rounded flex items-center gap-1 transition-colors"
        >
            <Plus :size="12" /> Add Comment
        </button>
    </div>

    <div class="space-y-3 mb-3">
        <div v-if="history.length === 0 && !isAddingComment" class="text-xs text-gray-400 italic text-center py-2 opacity-60">
            No history recorded.
        </div>
        
        <div v-for="(msg, idx) in history" :key="idx" class="flex gap-3 group">
             <div class="w-6 h-6 rounded-full flex items-center justify-center text-[9px] font-bold shrink-0 border mt-0.5 select-none"
                :class="msg.user === 'System' ? 'bg-gray-100 text-gray-500 border-gray-200' : 'bg-blue-100 text-blue-600 border-blue-200'">
                {{ msg.user === 'System' ? 'SYS' : (msg.user ? msg.user.substring(0,2).toUpperCase() : '?') }}
             </div>
             <div class="flex-1">
                 <div class="flex items-baseline justify-between">
                     <span class="text-[11px] font-bold" :class="msg.user === 'System' ? 'text-gray-500' : 'text-gray-700'">{{ msg.user }}</span>
                     <span class="text-[9px] text-gray-400">{{ formatTime(msg.timestamp) }}</span>
                 </div>
                 <div class="text-xs mt-0.5 whitespace-pre-wrap leading-relaxed" :class="msg.user === 'System' ? 'text-gray-500 italic' : 'text-gray-700'">
                    {{ msg.message }}
                 </div>
             </div>
        </div>
    </div>

    <div v-if="isAddingComment" class="animate-in fade-in slide-in-from-top-1 bg-blue-50/50 p-2 rounded border border-blue-100">
        <textarea 
            ref="commentInput"
            v-model="newComment" 
            rows="2" 
            class="w-full text-xs border border-blue-300 rounded p-2 focus:ring-2 focus:ring-blue-100 outline-none bg-white mb-2 resize-none"
            placeholder="Type your comment... (Ctrl+Enter to save)"
            @keydown.enter.ctrl="saveComment"
        ></textarea>
        <div class="flex justify-end gap-2">
            <button @click="isAddingComment = false" class="px-2 py-1 text-xs text-gray-500 hover:text-gray-700 transition-colors">Cancel</button>
            <button @click="saveComment" class="px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 shadow-sm transition-colors" :disabled="!newComment">Save Comment</button>
        </div>
    </div>
  </div>
</template>