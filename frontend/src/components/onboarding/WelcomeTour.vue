<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useStorage } from '@vueuse/core'
import { X, ArrowRight, Check, Settings, Layout } from 'lucide-vue-next'
import { useUIStore } from '@/stores/ui'

const ui = useUIStore()
const tourCompleted = useStorage('matros-global-tour-seen-v3', false)
const isOpen = ref(false)
const step = ref(1)

onMounted(() => {
    if (!tourCompleted.value) {
        setTimeout(() => { isOpen.value = true }, 1500)
    }
})

const next = () => { step.value++ }

const finish = () => {
    isOpen.value = false
    tourCompleted.value = true
    ui.setView('settings')
}

const steps = [
    { 
        title: 'Welcome to MatrosDMS', 
        text: 'Your document management journey starts here. Let us get you set up.',
        icon: Layout
    },
    {
        title: 'Configuration First',
        text: 'Before you start uploading, you need to configure your Master Data. This defines how your documents are organized.',
        icon: Settings
    },
    {
        title: 'Go to Settings',
        text: 'Click the Gear Icon in the bottom left sidebar to access Stores, Users, and the Category Import tool.',
        icon: Settings,
        highlightTarget: true
    }
]
</script>

<template>
  <Teleport to="body">
    <div v-if="isOpen" class="fixed inset-0 z-[200] flex items-center justify-center bg-black/40 backdrop-blur-sm animate-in fade-in duration-300">
        <div v-if="steps[step-1].highlightTarget" class="fixed bottom-14 left-3 w-10 h-10 rounded-full border-4 border-yellow-400 animate-ping pointer-events-none z-[210]"></div>

        <div class="bg-white rounded-xl shadow-2xl w-full max-w-sm overflow-hidden relative z-[220]">
            <button @click="finish" class="absolute top-3 right-3 text-gray-400 hover:text-gray-600 p-1 rounded-full hover:bg-gray-100 transition-colors">
                <X :size="20" />
            </button>

            <div class="p-8 text-center">
                <div class="flex justify-center mb-6 text-blue-600">
                    <component :is="steps[step-1].icon" :size="48" />
                </div>
                <h2 class="text-xl font-bold text-gray-800 mb-2">{{ steps[step-1].title }}</h2>
                <p class="text-sm text-gray-500 leading-relaxed">{{ steps[step-1].text }}</p>
            </div>

            <div class="p-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
                <div class="flex gap-1.5">
                    <div 
                        v-for="i in steps.length" 
                        :key="i"
                        class="w-2 h-2 rounded-full transition-colors"
                        :class="step === i ? 'bg-blue-600' : 'bg-gray-300'"
                    ></div>
                </div>

                <button 
                    v-if="step < steps.length" 
                    @click="next" 
                    class="bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
                >
                    Next <ArrowRight :size="16" />
                </button>
                <button 
                    v-else 
                    @click="finish" 
                    class="bg-green-600 hover:bg-green-700 text-white text-sm font-semibold px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
                >
                    Go to Settings <Check :size="16" />
                </button>
            </div>
        </div>
    </div>
  </Teleport>
</template>