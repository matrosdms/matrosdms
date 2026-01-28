<script setup lang="ts">
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSpinner from '@/components/ui/BaseSpinner.vue'
import { useHotkeys } from '@/composables/useHotkeys'

const props = defineProps<{
  title: string;
  subtitle?: string;
  isLoading?: boolean;
  submitLabel?: string;
  cancelLabel?: string;
  wide?: boolean;
}>()

const emit = defineEmits(['submit', 'cancel'])

useHotkeys(['s', 'S'], () => emit('submit'), { 
    ctrl: true, 
    condition: () => !props.isLoading 
})

useHotkeys('Escape', () => emit('cancel'), { 
    condition: () => !props.isLoading 
})
</script>

<template>
  <div class="h-full flex flex-col bg-muted/30 transition-colors">
    <!-- Header -->
    <div class="px-4 py-2 border-b bg-background flex items-center justify-between shrink-0 h-[45px]">
      <div class="flex items-center gap-2 overflow-hidden">
          <span class="text-sm font-bold text-foreground truncate">{{ title }}</span>
          <span v-if="subtitle" class="text-[10px] bg-primary-light text-primary border border-blue-100 dark:border-blue-900 rounded px-1.5 py-0.5">{{ subtitle }}</span>
      </div>
      <div class="flex items-center gap-1">
          <slot name="header-actions"></slot>
      </div>
    </div>

    <!-- Body -->
    <div class="flex-1 overflow-auto relative flex flex-col custom-scrollbar" :class="wide ? 'p-0' : 'p-6 gap-4'">
       <!-- Loading Overlay -->
       <div v-if="isLoading" class="absolute inset-0 flex items-center justify-center bg-background/50 z-50 backdrop-blur-[1px]">
          <div class="bg-background p-4 rounded-full shadow-lg border"><BaseSpinner :size="24" class="text-primary" /></div>
       </div>

       <!-- Content Wrapper -->
       <div :class="wide ? 'w-full h-full' : 'w-full max-w-2xl mx-auto flex flex-col gap-5'">
         <slot></slot>
       </div>
    </div>

    <!-- Footer -->
    <div class="p-3 border-t bg-background shrink-0 flex justify-between items-center">
        <slot name="footer">
            <div class="text-[10px] text-muted-foreground flex gap-3">
                <span class="hidden md:inline"><kbd class="font-mono border rounded px-1 bg-muted">Ctrl+S</kbd> Save</span>
                <span class="hidden md:inline"><kbd class="font-mono border rounded px-1 bg-muted">Esc</kbd> Cancel</span>
            </div>

            <div class="flex gap-2">
                <BaseButton variant="outline" @click="$emit('cancel')" :disabled="isLoading">
                    {{ cancelLabel || 'Cancel' }}
                </BaseButton>
                <BaseButton variant="default" @click="$emit('submit')" :loading="isLoading">
                    {{ submitLabel || 'Save Changes' }}
                </BaseButton>
            </div>
        </slot>
    </div>
  </div>
</template>