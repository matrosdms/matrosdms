import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { client } from '@/api/client'
import { ECategoryScope, type ERootCategoryType } from '@/enums'
import type { components } from '@/types/schema'

type RootCategoryMeta = components['schemas']['RootCategoryMeta']

export const useConfigStore = defineStore('config', () => {
    const categoryDefinitions = ref<RootCategoryMeta[]>([])
    const isLoaded = ref(false)

    async function loadDefinitions() {
        try {
            // Fix: Use /api prefix and cast for safety
            const { data } = await client.GET("/api/category/definitions", {})
            if (data) {
                categoryDefinitions.value = (data as any[]) || []
                isLoaded.value = true
            }
        } catch (e) {
            console.error("Failed to load category definitions", e)
        }
    }

    const contextDimensions = computed(() => 
        categoryDefinitions.value
            .filter(d => d.scope === ECategoryScope.CONTEXT)
            .map(d => d.key as ERootCategoryType)
    )

    const documentDimensions = computed(() => 
        categoryDefinitions.value
            .filter(d => d.scope === ECategoryScope.DOCUMENT)
            .map(d => d.key as ERootCategoryType)
    )

    function getLabel(key: string) {
        const def = categoryDefinitions.value.find(d => d.key === key)
        return def?.label || key
    }

    return { 
        categoryDefinitions, 
        isLoaded,
        loadDefinitions, 
        contextDimensions, 
        documentDimensions,
        getLabel
    }
})