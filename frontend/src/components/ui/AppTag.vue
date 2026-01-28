<script setup lang="ts">
import { computed } from 'vue'
import { getTagStyle } from '@/utils/tagStyles'
import { cn } from '@/lib/utils'
import { X } from 'lucide-vue-next'

const props = defineProps<{
  dimension?: string
  rootId?: string
  label?: string
  removable?: boolean
  class?: string
}>()

const emit = defineEmits(['remove', 'click'])

const dynamicStyle = computed(() => {
    const key = props.rootId || props.dimension || '';
    return getTagStyle(key);
})

const handleKeydown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault()
        emit('click', e)
    }
}
</script>

<template>
  <span 
    :class="cn(
      'matros-tag px-2 py-0.5 rounded-md text-[11px] font-bold flex items-center border whitespace-nowrap select-none shadow-sm', 
      props.class, 
      { 'cursor-pointer hover:brightness-95': $attrs.onClick }
    )"
    :style="dynamicStyle"
    :role="$attrs.onClick ? 'button' : undefined"
    :tabindex="$attrs.onClick ? 0 : undefined"
    @click="$emit('click', $event)"
    @keydown="handleKeydown"
  >
    <slot>{{ label }}</slot>
    
    <button 
      v-if="removable" 
      @click.stop="$emit('remove')" 
      @keydown.enter.stop="$emit('remove')"
      class="ml-1.5 opacity-60 hover:opacity-100 hover:text-red-600 focus:text-red-700 transition-opacity flex items-center rounded-sm focus:outline-none"
      type="button"
      title="Remove tag"
    >
        <X :size="12" stroke-width="3" />
    </button>
  </span>
</template>