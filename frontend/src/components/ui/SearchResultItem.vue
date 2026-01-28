<script setup lang="ts">
import { FileText, ArrowRight, Folder } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  active?: boolean
  title: string
  subtitle?: string
  score?: number
  date?: string
  uuid?: string
  context?: string
}>(), {
  title: 'Untitled Document'
})
</script>

<template>
  <div 
    class="w-full text-left px-3 py-2.5 border-b border-border last:border-0 flex items-start gap-3 transition-colors group cursor-pointer"
    :class="active ? 'bg-blue-50 dark:bg-blue-900/30' : 'hover:bg-muted/30 bg-background'"
  >
    <div 
        class="p-2 rounded-lg shrink-0 transition-colors mt-0.5"
        :class="active ? 'bg-blue-100 dark:bg-blue-800 text-blue-600 dark:text-blue-300' : 'bg-muted text-muted-foreground'"
    >
        <FileText :size="18" />
    </div>
    
    <div class="flex-1 min-w-0">
        <!-- Top Row: Name + Score -->
        <div class="flex items-center justify-between mb-0.5">
            <div class="text-sm font-medium text-foreground truncate pr-2">{{ title }}</div>
            <div class="flex items-center gap-2">
                <span v-if="score" class="text-[9px] font-mono text-muted-foreground bg-muted/50 px-1 rounded">{{ Math.round(score * 100) }}%</span>
                <ArrowRight v-if="active" :size="14" class="text-primary opacity-50" />
            </div>
        </div>
        
        <!-- Highlight / Description -->
        <div v-if="subtitle" class="text-xs text-muted-foreground line-clamp-2 leading-relaxed mb-1.5" v-html="subtitle"></div>
        
        <!-- Meta Row -->
        <div class="text-[10px] text-muted-foreground flex flex-wrap gap-2 items-center">
            <span v-if="date" class="font-mono opacity-80">{{ new Date(date).toLocaleDateString() }}</span>
            
            <span v-if="context" class="flex items-center gap-1 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 px-1.5 py-0.5 rounded border border-blue-100 dark:border-blue-900 font-medium max-w-[120px] truncate">
                <Folder :size="10" /> {{ context }}
            </span>
            
            <span v-if="uuid" class="text-[9px] text-gray-300 dark:text-gray-600 font-mono select-all ml-auto">
                #{{ uuid.substring(0,8) }}
            </span>
        </div>
    </div>
  </div>
</template>

<style>
/* Style for backend-provided highlight tags usually <em> or <mark> */
em, mark {
    @apply bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-300 font-bold not-italic px-0.5 rounded;
}
</style>