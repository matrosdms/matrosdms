<script setup lang="ts">
import { computed } from 'vue'
import * as icons from 'lucide-vue-next'

const props = defineProps<{
  name: string
  size?: number
  class?: string
}>()

const iconComponent = computed(() => {
  if (!props.name) return null
  
  // Normalize string (e.g. "user" -> "User", "file-text" -> "FileText")
  // Lucide usually exports PascalCase names
  const pascalName = props.name.charAt(0).toUpperCase() + props.name.slice(1)
  
  // Direct lookup
  if (icons[props.name as keyof typeof icons]) {
      return icons[props.name as keyof typeof icons]
  }
  // Try PascalCase
  if (icons[pascalName as keyof typeof icons]) {
      return icons[pascalName as keyof typeof icons]
  }
  
  // Fallback icon if needed, or return null to render nothing
  return icons.HelpCircle
})
</script>

<template>
  <component 
    :is="iconComponent" 
    :size="size || 16" 
    :class="$props.class" 
    v-if="iconComponent"
  />
</template>