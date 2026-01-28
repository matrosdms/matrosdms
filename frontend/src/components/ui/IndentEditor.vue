<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits(['update:modelValue', 'save'])

const textarea = ref<HTMLTextAreaElement | null>(null)
const pre = ref<HTMLDivElement | null>(null)

const onInput = (e: Event) => {
  const val = (e.target as HTMLTextAreaElement).value
  emit('update:modelValue', val)
  syncScroll()
}

const syncScroll = () => {
    if (textarea.value && pre.value) {
        pre.value.scrollTop = textarea.value.scrollTop
        pre.value.scrollLeft = textarea.value.scrollLeft
    }
}

// Colors for indentation levels (0 to 5) - Updated for Dark Mode Support
const colors = [
    'text-gray-800 dark:text-gray-200',
    'text-blue-700 dark:text-blue-400 font-medium',
    'text-green-700 dark:text-green-400',
    'text-purple-700 dark:text-purple-400',
    'text-orange-700 dark:text-orange-400',
    'text-red-700 dark:text-red-400'
]

// Simple syntax highlighter for indentation visualization
const highlightedCode = ref('')

watch(() => props.modelValue, (val) => {
    if (!val) {
        highlightedCode.value = ''
        return
    }
    
    const lines = val.split('\n')
    const html = lines.map(line => {
        if (!line.trim()) return '<br>'
        const indentMatch = line.match(/^(\s*)/)
        const indent = indentMatch ? indentMatch[0].length : 0
        // Assume 2 spaces per level for coloring logic
        const level = Math.floor(indent / 2)
        const colorClass = colors[Math.min(level, colors.length - 1)]
        
        // Preserve leading spaces with non-breaking spaces for visual alignment in HTML
        const content = line.replace(/</g, '&lt;').replace(/>/g, '&gt;')
        return `<div class="${colorClass}">${content || '<br>'}</div>`
    }).join('')
    
    highlightedCode.value = html
}, { immediate: true })

const handleTab = (e: KeyboardEvent) => {
    const start = textarea.value?.selectionStart || 0
    const end = textarea.value?.selectionEnd || 0
    const val = props.modelValue
    
    // Insert 2 spaces
    const newVal = val.substring(0, start) + '  ' + val.substring(end)
    emit('update:modelValue', newVal)
    
    nextTick(() => {
        if (textarea.value) {
            textarea.value.selectionStart = textarea.value.selectionEnd = start + 2
        }
    })
}

// Expose formatting function to parent
const autoFormat = () => {
    const lines = props.modelValue.split('\n')
    const result: string[] = []
    
    // Stack stores the indentation of parents: { indent: number, level: number }
    // Root level is -1 indent, level -1
    const stack = [{ indent: -1, level: -1 }]
    
    lines.forEach(line => {
        if (!line.trim()) {
            result.push('')
            return
        }
        
        // Calculate raw indentation (space count)
        const rawIndent = line.search(/\S/)
        
        // Pop stack until we find the parent (strictly less indent)
        // If indent is equal to a previous sibling, we will stop after popping its children
        while (stack.length > 1 && stack[stack.length - 1].indent >= rawIndent) {
            stack.pop()
        }
        
        const parent = stack[stack.length - 1]
        const currentLevel = parent.level + 1
        
        // Push current context
        stack.push({ indent: rawIndent, level: currentLevel })
        
        // Reconstruct line with normalized 2-space indentation
        result.push('  '.repeat(currentLevel) + line.trim())
    })
    
    const formatted = result.join('\n')
    emit('update:modelValue', formatted)
    return formatted
}

defineExpose({ autoFormat })
</script>

<template>
  <div class="relative w-full h-full border border-gray-300 dark:border-gray-700 rounded bg-white dark:bg-gray-950 overflow-hidden text-xs font-mono transition-colors duration-300">
     
     <!-- Highlight Layer (Background) -->
     <div 
        ref="pre"
        class="absolute inset-0 p-4 pointer-events-none whitespace-pre overflow-hidden leading-relaxed"
        v-html="highlightedCode"
     ></div>

     <!-- Input Layer (Foreground) -->
     <textarea 
        ref="textarea"
        :value="modelValue"
        :disabled="disabled"
        :placeholder="placeholder"
        @input="onInput"
        @scroll="syncScroll"
        @keydown.tab.prevent="handleTab"
        class="absolute inset-0 w-full h-full p-4 bg-transparent text-transparent caret-black dark:caret-white outline-none resize-none leading-relaxed"
        spellcheck="false"
     ></textarea>
  </div>
</template>