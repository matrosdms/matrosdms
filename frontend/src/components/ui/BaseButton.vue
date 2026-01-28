<script setup lang="ts">
import { cva, type VariantProps } from 'class-variance-authority'
import { computed } from 'vue'
import { cn } from '@/lib/utils'
import BaseSpinner from './BaseSpinner.vue'

const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 select-none",
  {
    variants: {
      variant: {
        default: "bg-primary text-primary-foreground hover:bg-primary-hover shadow-sm",
        destructive: "bg-destructive text-destructive-foreground hover:bg-destructive-hover shadow-sm",
        outline: "border border-input bg-background hover:bg-muted/50 text-foreground",
        secondary: "bg-muted text-foreground hover:bg-muted/80",
        ghost: "hover:bg-muted text-foreground",
        link: "text-primary underline-offset-4 hover:underline",
        success: "bg-success text-success-foreground hover:bg-emerald-600 shadow-sm",
        warning: "bg-warning text-warning-foreground hover:bg-amber-600 shadow-sm",
      },
      size: {
        default: "h-9 px-4 py-2",
        sm: "h-8 rounded-md px-3 text-xs",
        lg: "h-10 rounded-md px-8",
        icon: "h-9 w-9",
        iconSm: "h-7 w-7 p-1",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

type ButtonProps = VariantProps<typeof buttonVariants>

const props = withDefaults(defineProps<{
  variant?: ButtonProps['variant']
  size?: ButtonProps['size']
  class?: string
  disabled?: boolean
  loading?: boolean
  type?: "button" | "submit" | "reset"
  title?: string
}>(), {
  variant: "default",
  size: "default",
  type: "button"
})

const emit = defineEmits(['click'])

const classes = computed(() => cn(buttonVariants({ variant: props.variant, size: props.size }), props.class))
</script>

<template>
  <button 
    :type="type" 
    :class="classes" 
    :disabled="disabled || loading" 
    :title="title"
    @click="emit('click', $event)"
  >
    <BaseSpinner v-if="loading" class="mr-2" />
    <slot />
  </button>
</template>