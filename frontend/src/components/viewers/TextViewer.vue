<script setup lang="ts">
import { computed } from 'vue'
import { usePreferencesStore } from '@/stores/preferences'

const props = defineProps<{
  blobUrl?: string | null
  textContent?: string | null
}>()

const prefs = usePreferencesStore()

// If we have raw text content, we render it directly.
// Otherwise we use the iframe for Blob URLs (JSON/XML/Text files).
const isRaw = computed(() => !!props.textContent)
</script>

<template>
  <div class="w-full h-full bg-white dark:bg-gray-950 overflow-auto">
      <div v-if="isRaw" class="p-6 font-mono text-xs leading-relaxed whitespace-pre-wrap text-gray-800 dark:text-gray-300">
          {{ textContent }}
      </div>
      
      <iframe
        v-else-if="blobUrl"
        :src="blobUrl"
        class="w-full h-full border-0 bg-white"
        :style="{ colorScheme: prefs.isDarkMode ? 'dark' : 'light' }"
      />
      
      <div v-else class="h-full flex items-center justify-center text-muted-foreground italic">
          No text content available.
      </div>
  </div>
</template>