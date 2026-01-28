<script setup lang="ts">
import { ref, computed, shallowRef } from 'vue'
import { onClickOutside } from '@vueuse/core'
import { Search, X, ChevronDown } from 'lucide-vue-next'
import * as LucideIcons from 'lucide-vue-next'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)
const searchQuery = ref('')

// Load all icons into a non-reactive reference for performance
const allIcons = shallowRef(Object.keys(LucideIcons).filter(k => k !== 'default' && k !== 'createLucideIcon'))

// Filter logic
const filteredIcons = computed(() => {
    const q = searchQuery.value.toLowerCase().trim()
    if (!q) return allIcons.value.slice(0, 60) // Show first 60 by default
    return allIcons.value.filter(name => name.toLowerCase().includes(q)).slice(0, 60) // Limit render to 60 for performance
})

const currentIconComponent = computed(() => {
    if (!props.modelValue) return null
    // Explicitly cast to any to satisfy TS compiler
    return (LucideIcons as any)[props.modelValue] || null
})

// Helper to resolve icon in template safely
const getIcon = (name: string) => {
    return (LucideIcons as any)[name]
}

const selectIcon = (name: string) => {
    emit('update:modelValue', name)
    isOpen.value = false
}

const clear = () => {
    emit('update:modelValue', '')
}

onClickOutside(containerRef, () => isOpen.value = false)
</script>

<template>
  <div ref="containerRef" class="relative w-full">
    <label class="text-sm font-medium leading-none text-foreground block mb-1.5">Icon</label>
    
    <!-- Trigger Button -->
    <div 
        @click="isOpen = !isOpen"
        class="flex items-center justify-between w-full h-9 px-3 py-2 bg-background border border-input rounded-md shadow-sm cursor-pointer hover:bg-muted/50 transition-colors"
        :class="{'ring-2 ring-primary ring-offset-1': isOpen}"
    >
        <div class="flex items-center gap-2 overflow-hidden">
            <component :is="currentIconComponent" v-if="currentIconComponent" class="w-4 h-4 text-primary" />
            <span v-if="modelValue" class="text-sm font-medium">{{ modelValue }}</span>
            <span v-else class="text-sm text-muted-foreground italic">Select Icon...</span>
        </div>
        <div class="flex items-center gap-1">
            <button v-if="modelValue" @click.stop="clear" class="p-0.5 hover:bg-muted rounded text-muted-foreground hover:text-destructive transition-colors">
                <X :size="14" />
            </button>
            <ChevronDown :size="14" class="text-muted-foreground opacity-50" />
        </div>
    </div>

    <!-- Dropdown -->
    <div v-if="isOpen" class="absolute z-50 top-full mt-1 w-[280px] bg-white dark:bg-gray-800 border border-border rounded-lg shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-100">
        
        <!-- Search Input -->
        <div class="p-2 border-b border-border bg-gray-50 dark:bg-gray-900">
            <div class="relative">
                <Search class="absolute left-2 top-2 text-muted-foreground" :size="14" />
                <input 
                    v-model="searchQuery" 
                    ref="searchInput"
                    type="text" 
                    placeholder="Search icons..." 
                    class="w-full pl-8 pr-2 py-1.5 text-xs bg-white dark:bg-gray-950 border border-input rounded-md focus:outline-none focus:ring-1 focus:ring-primary text-foreground"
                    autofocus
                />
            </div>
        </div>

        <!-- Grid -->
        <div class="p-2 grid grid-cols-5 gap-1 max-h-[200px] overflow-y-auto custom-scrollbar bg-white dark:bg-gray-800">
            <button 
                v-for="name in filteredIcons" 
                :key="name"
                @click="selectIcon(name)"
                class="flex flex-col items-center justify-center gap-1 p-2 rounded hover:bg-primary/10 hover:text-primary transition-colors text-muted-foreground"
                :class="{'bg-primary/20 text-primary border border-primary/30': modelValue === name}"
                :title="name"
            >
                <component :is="getIcon(name)" :size="18" />
            </button>
        </div>
        
        <div class="px-2 py-1 bg-gray-50 dark:bg-gray-900 text-[9px] text-center text-muted-foreground border-t border-border">
            {{ filteredIcons.length }} icons shown
        </div>
    </div>
  </div>
</template>