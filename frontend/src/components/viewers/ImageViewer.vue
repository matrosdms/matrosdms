<script setup lang="ts">
/**
 * ImageViewer — simple image display with object-fit contain.
 */
defineProps<{
  blobUrl: string
}>()
</script>

<template>
  <div class="w-full h-full flex items-center justify-center bg-gray-200 dark:bg-black overflow-auto p-4">
    <img :src="blobUrl" class="max-w-full max-h-full object-contain shadow-md" />
  </div>
</template>
