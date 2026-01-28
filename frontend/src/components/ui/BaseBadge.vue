<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { computed } from 'vue'
import { cn } from '@/lib/utils'

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 select-none",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground hover:bg-primary/80",
        secondary: "border-transparent bg-muted text-foreground hover:bg-muted/80",
        destructive: "border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80",
        outline: "text-foreground",
        success: "border-transparent bg-success text-success-foreground",
        warning: "border-transparent bg-warning text-warning-foreground",
        // Specific semantic styles for your app
        context: "border-blue-200 bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 dark:border-blue-800",
        category: "border-purple-200 bg-purple-50 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300 dark:border-purple-800",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

const props = defineProps<{
  variant?: VariantProps<typeof badgeVariants>['variant']
  class?: string
}>()

const classes = computed(() => cn(badgeVariants({ variant: props.variant }), props.class))
</script>

<template>
  <div :class="classes">
    <slot />
  </div>
</template>