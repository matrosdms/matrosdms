<script setup lang="ts">
import { computed } from 'vue'
import { Pencil, Trash2 } from 'lucide-vue-next'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import type { AnyEntity } from '@/types/models'

const props = defineProps<{
  item: AnyEntity
}>()

defineEmits(['edit', 'delete'])

// Helper to safely access dynamic properties
const safeItem = computed(() => props.item as any)
const typeLabel = computed(() => safeItem.value.dataType || null)
const shortName = computed(() => safeItem.value.shortname || null)
</script>

<template>
  <div class="px-6 py-5 border-b bg-background flex justify-between items-start">
      <div class="flex-1 min-w-0 pr-4">
          <div class="flex items-center gap-2 flex-wrap mb-1">
              <h2 class="text-xl font-bold text-foreground truncate">
                  {{ item.name }}
              </h2>
              <BaseBadge v-if="shortName" variant="secondary">{{ shortName }}</BaseBadge>
              <BaseBadge v-if="typeLabel" variant="outline" class="font-mono text-purple-600 border-purple-200 bg-purple-50">{{ typeLabel }}</BaseBadge>
          </div>
          <div class="text-[10px] text-muted-foreground font-mono select-all flex items-center gap-2">
              <span class="opacity-70">UUID:</span> {{ item.uuid }}
          </div>
      </div>
      
      <div class="flex gap-1 shrink-0">
          <BaseButton variant="ghost" size="icon" @click="$emit('edit')" title="Edit">
              <Pencil :size="18" />
          </BaseButton>
          <BaseButton variant="ghost" size="icon" class="text-destructive hover:bg-destructive-light" @click="$emit('delete')" title="Delete">
              <Trash2 :size="18" />
          </BaseButton>
      </div>
  </div>
</template>