<script setup lang="ts">
/**
 * TextViewer — renders plain text, JSON, XML via a sandboxed iframe.
 */
import { usePreferencesStore } from '@/stores/preferences'

defineProps<{
  blobUrl: string
}>()

const prefs = usePreferencesStore()
</script>

<template>
  <iframe
    :src="blobUrl"
    class="w-full h-full border-0 bg-white"
    :style="{ colorScheme: prefs.isDarkMode ? 'dark' : 'light' }"
  />
</template>
